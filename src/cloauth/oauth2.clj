(ns cloauth.oauth2
  "OAuth2 protocol flow functions" 
  (:require [cloauth.models.kdb :as db]
            [cloauth.token :as token])
  (:use [clojure.string :only (split)]))
  


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

(defn- check-valid-client-id [clientId] 
  "Make sure the client id is valid"
       (if-not (db/get-client-by-clientId clientId)
                {:error :unauthorized_client
                 :error_description "Bad client id"}))

; todo: we may have more than one redirect uri....
(defn- check-invalid-redirectUri [clientId uri] 
  "Make sure the redirect URI is registered" 
  (let [client (db/get-client-by-clientId clientId)
        clientUris (:redirectUri client)]
         (if-not (or (= uri clientUris)
                     (some uri clientUris))
           {:error :invalid_request 
            :error_description "Redirect URI is not registered to this client"})))
  
(defn request-already-approved? [oauth-request] 
  "true if a request has been previously approved . 
   Note a change in scope triggers re-approval"
  (db/grant-scopes-are-the-same 
    (:userId oauth-request)  (:clientId oauth-request) (:scopes  oauth-request)))
 
; errors - is a map consisting of :errorcode - oauth error code 
; :error_description -    error_uri (optional)

; OAuth Request record - generic
(defrecord OAuthRequest [clientId userId responseType redirectUri scopes state access_type]
  ValidateRequestProtocol 
  (validate [this] 
     (merge this 
      (or 
        (check-valid-client-id (:clientId this))))))

(defn new-oauth-request [clientId userId responseType redirectUri scope state access_type] 
  "Create a new OAuth AuthZ request. Runs validation on the request and return the result
   we need to convert scopes (space delim) to a list "
  (let [scopes  (split scope #"\s+" )]
    (validate (OAuthRequest. clientId userId responseType redirectUri scopes state access_type))))

 
(defn valid-client? [id secret]
  "Check to see that client has a valid id and secret"
  (if-let [client  (db/get-client-by-clientId id)]
    (= secret (:clientSecret client))))

(defn- check-client-id-secret [id secret]
 "check the client id and secret match"
  (if-not (valid-client? id secret)
    {:error_code :unauthorized_client
     :error_description "Client is not authorized"}))


(defn- check-authCode [clientId code]
  "Check that the auth code for the request is in fact valid "
  (let [t (token/get-authcode-entry code)]
    (println "Check authcode " t )
    (if (or 
          ; there is no auth code 
          (nil? t)  
          ; there is one, but the clientId does not match
          (not= clientId  (-> t :request :clientId ))
          ; what else to check?
          )
          {:error_code :unauthorized_client
           :error_description "ClientId or code is  invalid"})))             
    
; Token Handling


(defn- msec-to-sec [msec] 
  "Convert msec to seconds"
  (int (/ msec 1000)))
  
(defn- expiry [future-msec]
  "Given a time in the future (msec) calculate 
   the remain time in seconds"
  (msec-to-sec (- future-msec (System/currentTimeMillis) )))
  
  
; Handle an authorization code grant 
; The client is asking to exchange an auth code for an access token and refresh token 
; returns a map that gets turned into a json response
(defn handle-authcode-grant [clientId redirectUri code ]   
  (let [authEntry (token/get-authcode-entry code)
        oauthRequest (:request authEntry)
        originalClientId (:clientId oauthRequest)
        userId (:userId oauthRequest)
        scopes (:scopes oauthRequest)
        badRedirect (check-invalid-redirectUri clientId redirectUri)]
    
    (cond 
      ; no matching auth entry ? Might have expired or it is bogus...
      (nil? authEntry) 
      {:error :access_denied}
        
      ; badredirect URL true ? return that error
      badRedirect badRedirect
      
      (not= originalClientId clientId)
      {:error :access_denied :error_description "Client Id does not match original request"}
      
      :else
      (do 
         (token/purge-code code) ; one time use only 
         ; return new access token
         (token/new-access-token clientId userId scopes)))))

(defn handle-refresh-token-grant [request]  
  {:error "not done!"})



(defn handle-oauth-token-user-request [oauthrequest] 
  "This is the 2 legged oauth handler
  The request has come from a web client (by JS perhaps) and 
  the request is for an access_token (not a code)"
  (let  [userId (db/current-userName)
         clientId (:clientId oauthrequest)
         scope (:scope oauthrequest)]
    (token/new-access-token clientId userId scope)))

