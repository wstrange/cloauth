(ns cloauth.views.oauth 
  (:require [noir.response :as resp]
            [noir.session :as session]
            [cloauth.oauth2 :as oauth]
            [cloauth.models.db :as db]
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
  (prn "Redirect to " request  " with params " params)
  (resp/redirect (str (:redirectUri request) "?"
                      (encode-params 
                        (if-let [state (:state params)]
                          (assoc params :state state)
                          params)))))
                                            

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
  

;;
;; Q: Google uses URL parameters 
; http://code.google.com/apis/accounts/docs/OAuth2UserAgent.html
;  Secret is not used. The redirectURI must be pre-registered.


;; Authorization Endpoint 
;  used to obtain authorization from the
;      resource owner via user-agent redirection
; http://code.google.com/apis/accounts/docs/OAuth2WebServer.html
;
(defpage "/oauth2/authorize"  {:keys [client_id response_type redirect_uri scope state] :as req} 
  ;(prn "Authorize  Request client=" client_id " type " response_type " redirect " redirect_uri)
  (let [request (oauth/new-auth-request client_id response_type redirect_uri scope state)]
    (prn "Created request " request)
    (if (oauth/error-code request)
      (error-response request)
      ; else - get consent
      (render "/oauth2/consent" {:oauth-request request}))))


; User Consent page 
; User must approve / deny the request
(defpage "/oauth2/consent" {:keys [oauth-request] :as req}  
  (prn "OAuth request " oauth-request )
  (session/flash-put! oauth-request)
  (let [client (db/get-client-by-clientId (:clientId oauth-request))]
    (common/layout 
      [:h1 "Approve Access Request"]
      [:h3 "Company Requesting Access: " (:companyName client)]
      [:h3 "Company Description:" (:description client)]
      [:p "Access Requested: " (:scope oauth-request) ]
      [:p "Please Grant or Deny this request: " ]
      [:p (link-to "/oauth2/consent/decide?d=grant" "Grant Request")]
      [:p (link-to "/oauth2/consent/decide?d=deny" "Deny Request" )]
      )))

(defpage "/oauth2/consent/decide" {:keys [d] } 
  (if-let [request (session/flash-get)] 
    (if (= d "grant")
      (send-redirect request (oauth/auth-code-request request))
      (send-redirect request {:error "access_denied" }))
    (common/layout 
      [:h1 "Error"]
      [:p "Bad Request. Please Retry "])))
 
    
(comment 
  (send-redirect request 
     (case (:responseType request)
      "code"  (oauth/auth-code-request request)
      "token" (oauth/auth-token-request request)
      (throw (Exception. "Unknown response type. This should have been caught by verify function")))))


; Todo - add client aut filter
(comment
(pre-route [:any "/client/token"]  {:as req} 
           (println "/client req " req  "\nparams " (:params req))))

; Token Endpoint 
; 

; Note /client uri - so we can use client authentication instead of user auth
; todo: We need a better system...
; todo: Client auth should be enforced here...
(defpage [:any "/client/token"]  {:keys [client_id client_secret redirect_uri grant_type code] :as req} 
  ;(println "Token Request " (:params req))
  (let [request (oauth/new-token-request client_id client_secret redirect_uri grant_type code)]
    ;(println "Create req" request)
    (resp/json (oauth/auth-token-request request))))
 

(defpage "/oauth2/error" {:keys [error]  :as request}
  (common/layout 
    [:h1 "Error Procesing OAuth request"]
    [:p "An unrecoverable error has occured"]
    [:p "Error Message:" error]
    ))
