(ns cloauth.oauth2)


; OAuth 2 error codes - as keys
(def error-codes #{ :access_denied :invalid_request :unauthorized_client 
                   :unsupported_response_type :invalid_scope
                   :server_error :temporarily_unavailable
                   ; this is not part of OAuth error code spec.
                   ; we use it as a marker 
                   :redirectUriInvalid  
                   })



(defprotocol RequestProtocol
  (validate [this])
  (error-code [this])
  (redirectUri-invalid? [this])
  (error-description [this]))

; errors - is a map consisting of :errorcode - oauth error code 
; :error_description -    error_uri (optional)

(defrecord Request [clientId responseType redirectUri scope state errors]
  RequestProtocol 
  (validate [this] 
            (assoc this :errors {}))
  ; return error code, or nil if there is none 
  (error-code [this]
               (-> this :errors :error_code))
  ; todo - should we check this some other way?
  (redirectUri-invalid? [this]
                        (= (error-code this) :redirectUriInvalid))
  ; get the first error? or do we con cat all the messages?
  (error-description [this]
                 (-> this :errors :errorDescription))
  )

(defn new-request [clientId responseType redirectUri scope state] 
  "Create a new OAuth AuthZ request. Runs validation on the request "
  (validate (Request. clientId responseType redirectUri scope state nil)))




; test

(def x (new-request "cle" "r" "f" "s" "x"))

