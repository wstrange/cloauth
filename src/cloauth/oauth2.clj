(ns cloauth.oauth2
  (:require [cloauth.models.db :as db]))


; OAuth 2 error codes - as keys
(def error-codes #{ :access_denied :invalid_request :unauthorized_client 
                   :unsupported_response_type :invalid_scope
                   :server_error :temporarily_unavailable
                   ; this is not part of OAuth error code spec.
                   ; we use it as a marker 
                   :redirectUriInvalid  
                   })

(defprotocol ValidateRequestProtocol 
    (validate [this]))

(defn error-code [r]
  "Get the error code from a request"
  (-> r :errors :error_code))

(defn error-description [r]
  "Get the error description form a request"
  (-> r :errors :errorDescription))

(defprotocol AuthRequestProtocol
  (redirectUri-invalid? [this]))
  

; errors - is a map consisting of :errorcode - oauth error code 
; :error_description -    error_uri (optional)

(defrecord AuthRequest [clientId responseType redirectUri scope state errors]
  ValidateRequestProtocol 
  (validate [this] (assoc this :errors {}))
 
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
  
  
; Token Handling

(defrecord TokenRequest[clientId clientSecret redirectUri  grantType  code ]
  ValidateRequestProtocol 
  (validate [this]
            (assoc this :errors {})))

(defn new-token-request [clientId clientSecret redirectUri grantType  code]
  (validate (TokenRequest. clientId clientSecret redirectUri grantType  code)))

(defn auth-token-request [request] 
  "Handle an token request"
  "This isn't right..."
  (let [t (db/new-access-token (:clientId request) (:scope request))
        expires-in  (- (:expires t) (System/currentTimeMillis))]
      (println "to do - persist code" t)
     {:access_token (:token t)
      :token_type "Bearer"
      :expires_in expires-in}))





