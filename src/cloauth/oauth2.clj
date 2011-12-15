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
                   })
)


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
  
 
; should be in db?
(defn valid-client? [id secret]
  "Check to see that client has a valid id and secret"
  (if-let [client  (db/get-client-by-clientId id)]
    (= secret (:clientSecret client))))

(defn- check-client [id secret]
 "check the client - and set the error map "
  (if-let [e (valid-client? id secret)]
    {} 
    ; else - error
    {:error :unauthorized_client
     :error_description "Client is not authorized"}))

; Token Handling

(defrecord TokenRequest[clientId clientSecret redirectUri  grantType  code ]
  ValidateRequestProtocol 
  (validate [this]
            (assoc this :errors 
              (check-client clientId clientSecret))))

(defn new-token-request [clientId clientSecret redirectUri grantType  code]
  (validate (TokenRequest. clientId clientSecret redirectUri grantType  code)))

(defn- msec-to-sec [msec] 
  "Convert msec to seconds"
  (int (/ msec 1000)))
  
(defn- expiry [absolute-msec]
  "Given an absolute time in the future (msec) calculate 
   the remain time in seconds"
  (msec-to-sec (- absolute-msec (System/currentTimeMillis) )))


(defn auth-token-request [request] 
  "Handle an token request. Return a map that will be converted into JSON
   "
  (if (has-error? request)
    (:errors request) ; will return map with error_code set 
    ; else
    (let [t (db/new-access-token (:clientId request) (:scope request))]
      (println "to do - persist code" t)
     {:access_token (:token t)
      :token_type "Bearer"
      :expires_in (expiry (:expires t))})))

