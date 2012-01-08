(ns cloauth.oauth2
  "OAuth2 protocol flow functions" 
  (:require [cloauth.models.kdb :as db]
            [cloauth.token :as token])
  )


; OAuth 2 error codes - as keys
(comment "do we need???"
(def error-codes #{ :access_denied :invalid_request :unauthorized_client 
                   :unsupported_response_type :invalid_scope
                   :server_error :temporarily_unavailable
                   ; this is not part of OAuth error code spec.
                   ; we use it as a marker 
                   :redirectUriInvalid  
                   }))

(defprotocol ValidateRequestProtocol 
    (validate [this]))

(defn has-error? [r] 
  " Does the request have an error"
  (:error_code r))
  

; "Check" protocols - returns :error_code/:error_description set or nil if everything is OK

(defn- check-valid-client-id [oauth-request] 
  "Make sure the client id is valid"
       (if-not (db/get-client-by-clientId (:clientId oauth-request))
                {:error_code :redirectUriInvalid 
                 :error_description "Bad client id"}))

; todo: we may have more than one redirect uri....
(defn- check-valid-redirectUri [oauth-request] 
  "Make sure the redirect URI is registered" 
  (let [client (db/get-client-by-clientId (:clientId oauth-request))
        clientUris (:redirectUri client)
        uri (:redirectUri oauth-request)]
         (if-not (or (= uri clientUris)
                     (some uri clientUris))
           {:error_code :invalid_request 
            :error_description "Redirect URI is not registered"})))
  
  
(defn request-needs-approval? [oauth-request] 
  "true if a request needs to be approved. This will happen if
   the request is new and the user has not previosly approved it, or the scopes have changed"
  true)



; errors - is a map consisting of :errorcode - oauth error code 
; :error_description -    error_uri (optional)

; OAuth Request record - generic
(defrecord OAuthRequest [clientId responseType redirectUri scope state access_type]
  ValidateRequestProtocol 
  (validate [this] 
     (merge this 
      (or 
        (check-valid-client-id this)))))

(defn new-oauth-request [clientId responseType redirectUri scope state access_type] 
  "Create a new OAuth AuthZ request. Runs validation on the request and return the result "
  (validate (OAuthRequest. clientId responseType redirectUri scope state access_type)))

(comment
(defn handle-oauth-code-request [request]
  "Handle An authentication code request (response_typ= code)
   At this point the user has already constented to the request
   and we have validated the request
   Return a param map for the response to return the client"
  (let [t (db/new-authcode (:clientId request) (:scope request))]
     (db/insert-token! t)
     {:code (:token t)}))
)
  
 
(defn valid-client? [id secret]
  "Check to see that client has a valid id and secret"
  (if-let [client  (db/get-client-by-clientId id)]
    (= secret (:clientSecret client))))

(defn- check-client-id-secret [id secret]
 "check the client id and secret match"
  (if-not (valid-client? id secret)
    {:error_code :unauthorized_client
     :error_description "Client is not authorized"}))

(defn- check-grantType [grantType] 
  (if (not= grantType "authorization_code") 
    {:error_code :invalidrequest
     :error_description "Grant type must be authorization_code"}))

(defn- check-authCode [clientId code]
  "Check that the auth code for the request is in fact valid "
  (let [t (token/get-authcode-entry code)]
    (if (or 
          ; there is no auth code in the db
          (nil? t)  
          ; there is one, but the clientId does not match
          (not= clientId  (-> t :request :clientId ))
          ; what else to check?
          )
          {:error_code :unauthorized_client
           :error_description "ClientId or code is  invalid"})))             
    
; Token Handling


(defrecord TokenRequest[clientId clientSecret redirectUri  grantType  code ]
  ValidateRequestProtocol 
  (validate [this]
            (assoc this :errors 
              (merge   ; todo: should we return multiple errors?
                (check-client-id-secret clientId clientSecret)
                (check-grantType grantType)
                (check-authCode clientId code )))))


(defn new-token-request [clientId clientSecret redirectUri grantType code]
  "Create a new validated token request
   If the token is not valid the :errors map will be set"
  (validate (TokenRequest. clientId clientSecret redirectUri grantType  code)))

(defn- msec-to-sec [msec] 
  "Convert msec to seconds"
  (int (/ msec 1000)))
  
(defn- expiry [future-msec]
  "Given a time in the future (msec) calculate 
   the remain time in seconds"
  (msec-to-sec (- future-msec (System/currentTimeMillis) )))




(comment 
; todo: Have to look up the user id
(defn handle-oauth-token-client-request [request] 
  "Handle an token request made by a client (not a user). 
   At this point validation has already been run on the request
   i.e.: The client id is valid ...
   Return a map that will be converted into JSON response
   request - Token request
   "
  (prn "Token request " request)
  (if (has-error? request)
    {:error (:error_code request) 
     :error_description (:error_description request)}
    ; else
    (let [clientId (:clientId request)
          code (:code request)
          authEntry (token/get-authcode-entry code)
          oauthRequest (:request authEntry)
          userId (:userId request)
          scopes (:scopes request)]
     (token/new-access-token clientId userId scopes))))
)

(defn handle-oauth-token-user-request [oauthrequest] 
  "This is the 2 legged oauth handler
  The request has come from a web client (by JS perhaps) and 
  the request is for an access_token (not a code)"
  (let  [userId (db/current-userName)
         clientId (:clientId oauthrequest)
         scope (:scope oauthrequest)]
    (token/new-access-token clientId userId scope)))