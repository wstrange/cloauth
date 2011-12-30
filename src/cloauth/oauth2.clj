(ns cloauth.oauth2
  (:require [cloauth.models.db :as db]))


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

(defn error-code [r]
  "Get the error code from a request"
  (-> r :errors :error))

(defn has-error? [r] 
  (error-code r))

(defn error-description [r]
  "Get the error description from a request"
  (-> r :errors :errorDescription))

  
(defprotocol AuthRequestProtocol
  (redirectUri-invalid? [this]))
  

; errors - is a map consisting of :errorcode - oauth error code 
; :error_description -    error_uri (optional)

(defrecord AuthRequest [clientId responseType redirectUri scope state errors]
  ValidateRequestProtocol 
  (validate [this] (assoc this :errors {})) ; todo: TBD
 
  AuthRequestProtocol
   ; todo - should we check this some other way?
  (redirectUri-invalid? [this]
                        (= (error-code this) :redirectUriInvalid)))

(defn new-auth-request [clientId responseType redirectUri scope state] 
  "Create a new OAuth AuthZ request. Runs validation on the request "
  (validate (AuthRequest. clientId responseType redirectUri scope state nil)))


(defn auth-code-request [request]
  "Handle An authentication code request.
   Return a param map for the response"
  (let [t (db/new-authcode (:clientId request) (:scope request))]
     (db/insert-token! t)
     {:code (:token t)}))
  
 
; should be in db?
(defn valid-client? [id secret]
  "Check to see that client has a valid id and secret"
  (if-let [client  (db/get-client-by-clientId id)]
    (= secret (:clientSecret client))))

(defn- check-client [id secret]
 "check the client id and secret match"
  (if-not (valid-client? id secret)
    {:error :unauthorized_client
     :error_description "Client is not authorized"}))

(defn- check-grantType [grantType] 
  (if (not= grantType "authorization_code") 
    {:error :invalidrequest
     :error_description "Grant type must be authorization_code"}))

(defn- check-authCode [clientId code]
  "Check that the auth code for the request is in fact valid "
  (let [t (db/get-token-by-id code)]
    (if (or 
          ; there is no auth code in the db
          (nil? t)  
          ; there is one, but the clientId does not match
          (not= clientId  (:clientId t))
          ; what else to check?
          )
          {:error :unauthorized_client
           :error_description "ClientId or code is  invalid"})))             
    
; Token Handling


(defrecord TokenRequest[clientId clientSecret redirectUri  grantType  code ]
  ValidateRequestProtocol 
  (validate [this]
            (assoc this :errors 
              (merge   ; todo: should we return multiple errors?
                (check-client clientId clientSecret)
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


; todo: Have to look up the user id
(defn auth-token-request [request] 
  "Handle an token request. Return a map that will be converted into JSON response
   request - Token request
   "
  (prn "Token request " request)
  (if (has-error? request)
    (:errors request) ; will return map with error_code set 
    ; else
    (let [clientId (:clientId request)
          code (:code request)
          codeToken (db/get-token-by-id code)        
          userId (:userId codeToken)
          scope (:scope codeToken)
          t (db/new-access-token clientId userId scope)]
     (db/insert-token! t)
     {:access_token (:token t)
      :token_type "Bearer"
      :expires_in (expiry (:expires t))})))
