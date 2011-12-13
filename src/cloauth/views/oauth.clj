(ns cloauth.views.oauth 
  (:require [noir.response :as resp]
            [noir.session :as session]
            [cloauth.oauth2 :as oauth]
            [cloauth.views.common :as common])
  (:use noir.core
        hiccup.core
        hiccup.page-helpers
        hiccup.form-helpers))

;; Entry points for the OAuth protocol 

;; OAuth Spec: http://tools.ietf.org/html/draft-ietf-oauth-v2-22

;; Client Authentication can use HTTP Basic Auth
;; Example: Authorization: Basic czZCaGRSa3F0MzpnWDFmQmF0M2JW
;; Alternativel - Can be in the message body
;; Including the client credentials in the request body using the two
;;   parameters is NOT RECOMMENDED,


(defn send-redirect [request params] 
  "Send a redirect. If there is a state variablin the request it will be 
   automatically appended to the redirect query string"
  (resp/redirect (str (:requestUri request) 
                      (encode-params 
                        (if-let [state (:state params)]
                          (assoc params :state state)
                          (params))))))
                                            

(defn error-response [request] 
  "Send the appropriate error response back to the originator.
  Only If the redirectUri is valid can we redirect back to the client, otherwise we need
  to let the user know the request was malformed. 
  We should not redirect back to an unverifed / unregistered URI"
  (if (oauth/redirectUri-invalid? request)
    (render "/oauth2/error" {:error "Bad request (redirect URI is not registered)"})
    ; else - redirect Uri is OK, but there is some other error
    ; redirect back to client with Json error response
    (send-redirect request {:error (name (oauth/error-code request)) })))
  

(defn auth-code-request [request]
  "Handle An authentication code request"
  )

(defn auth-token-request [request] 
  "Handle an token request"
  )

;;
;; Q: Google uses URL parameters 
; http://code.google.com/apis/accounts/docs/OAuth2UserAgent.html
;  Secret is not used. The redirectURI must be pre-registered.


;; Authorization Endpoint 
;  used to obtain authorization from the
;      resource owner via user-agent redirection
; http://code.google.com/apis/accounts/docs/OAuth2WebServer.html
;
(defpage "/oauth2/authorize"  {:keys [client_id response_type redirect_uri scope state] :as request} 
  (prn "Authorize  Request client=" client_id " type " response_type)
  (let [request (oauth/new-request client_id response_type redirect_uri scope state)]
    (if (oauth/error-code request)
      (error-response request)
      (case (:responseType request)
        "code"  (auth-code-request request)
        "token" (auth-token-request request)
        (throw (Exception. "Unknown response type. This should have been caught by verify function"))
        ))))

; Token Endpoint 
; 

(defpage "/oauth2/token"  {:keys [clientId clientSecret] :as request} 
  (resp/json "not done"))


(defpage "/ouath2/error" {:keys [error]  :as request}
  (common/layout 
    [:h1 "Error Procesing OAuth request"]
    [:p "An unrecoverable error has occured"]
    [:p "Error Message:" error]
    ))
