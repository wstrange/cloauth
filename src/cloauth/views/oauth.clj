(ns cloauth.views.oauth 
  "View and Route handlers for our OAuth entry points
  
   uses the oauth request functions in cloauth.oauth2
  OAuth Spec: http://tools.ietf.org/html/draft-ietf-oauth-v2-22"
  (:require [noir.response :as resp]
            [noir.session :as session]
            [cloauth.oauth2 :as oauth]
            [cloauth.models.db :as db]
            [cloauth.views.common :as common])
  (:use noir.core
        hiccup.core
        hiccup.page-helpers
        hiccup.form-helpers))


;; Client Authentication can use HTTP Basic Auth
;; Example: Authorization: Basic czZCaGRSa3F0MzpnWDFmQmF0M2JW
;; Alternativel - Can be in the message body
;; Including the client credentials in the request body using the two
;;   parameters is NOT RECOMMENDED,


(defn- send-redirect [request params] 
  "Send a redirect. If there is a state variable in the request it will be 
   automatically appended to the redirect query string
    request - oauth request. Contains the redirectUri and the optional state
    params - map of response params to send back with the redirect. They will be param encoded"
  (prn "Redirect to " request  " with params " params)
  (resp/redirect (str (:redirectUri request) "?"
                      (encode-params 
                        (if-let [state (:state request)]
                          (assoc params :state state)
                          params)))))
                                            
(defn- error-response [request] 
  "Send the appropriate error response back to the originator.
  Only If the redirectUri is valid can we redirect back to the client, otherwise we need
  to let the user know the request was malformed. 
  We should never redirect back to an unverifed / unregistered URI"
  (let [code (:error_code request)]
    (if (=  code :redirectUriInvalid)
      (render "/oauth2/error" {:error "Bad request (redirect URI is not registered)"})
      ; else - redirect Uri is OK, but there is some other error
      ; redirect back to client with Json error response
      (send-redirect request {:error code}))))
  

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
  (let [request (oauth/new-oauth-request client_id response_type redirect_uri scope state)]
    (prn "Created request " request)
    (if (:error_code request)
      (error-response request)
      ; else - get consent
      (render "/oauth2/consent" {:oauth-request request}))))

; User Consent page 
; User must approve / deny the request
; TODO: Support pluggable consent pages for apps, or better ways
; of registering the "scope" description
(defpage "/oauth2/consent" {:keys [oauth-request] :as req}  
  (prn "OAuth request " oauth-request )
  (session/flash-put! oauth-request)
  (let [client (db/get-client-by-clientId (:clientId oauth-request))]
    (common/layout 
      [:h2 "Approve Access Request"]
      [:h3 "Organization Requesting Access: " (:companyName client)]
      [:h3 "Organization Description:" (:description client)]
      [:p "Access Scope Requested: " (:scope oauth-request) ]
      [:p "Please Grant or Deny this request: " ]
      [:p (link-to "/oauth2/consent/decide?d=grant" "Grant Request")]
      [:p (link-to "/oauth2/consent/decide?d=deny" "Deny Request" )]
      )))

(defn- request-granted [request] 
  "The user has granted the request "
  (if (= (:responseType request) "token")
     ; token type - 2 legged oauth - we send back an access token - not a code token
     (send-redirect request (oauth/handle-oauth-token-user-request request))
     ; else
     (send-redirect request (oauth/handle-oauth-code-request request))))
     
(defn- request-denied [request] 
   (error-response (merge request {:error_code "access_denied"})))

(defpage "/oauth2/consent/decide" {:keys [d] } 
  (if-let [request (session/flash-get)] 
    (if (= d "grant")
      (request-granted request)
      ; else
      (request-denied request))
    ; else - if there is no request in the flash session - bad request
    (common/layout 
      [:h2 "Error"]
      [:p "Something went wrong (was the page bookmarked?). Please Retry "])))
 

; Token Endpoint 
; 

; Note /client uri - so we can use client authentication instead of user auth
; todo: We need a better system...
; todo: Client auth should be enforced here... 
; todo: Need to look up the user
(defpage [:any "/client/token"]  {:keys [client_id client_secret redirect_uri grant_type code] :as req} 
  ;(println "Token Request " (:params req))
  (let [request (oauth/new-token-request client_id client_secret redirect_uri grant_type code)]
    (resp/json (oauth/handle-oauth-token-client-request request))))
 

(defpage "/oauth2/error" {:keys [error]  :as request}
  (common/layout 
    [:h1 "Error Procesing OAuth request"]
    [:p "An unrecoverable error has occured"]
    [:p "Error Message:" error]))

; Display an auth token 
(defpartial display-token [token]
  (let [clientId (:clientId token)
        client (db/get-client-by-clientId clientId)]
    [:tr
      [:td  (:companyName client)]
      [:td (:scope token)]
      [:td (:description client)]
      [:td (link-to (str "/oauth2/user/revoke?token=" (:token token)) "Revoke Access")]
    ]))

;;; User Token Management
; Show the users auth codes
(defpartial auth-tokens [] 
  [:table 
    [:tr 
    [:th  {:width "10%"} "Company"]
    [:th  {:width "20%"} "Scope"]
    [:th {:width "40%"} "Description"]
    [:th {:width "30%"} "Action"]]
  (map #(display-token %) (db/get-user-auth-codes))])


; User page to review OAuth2 Grants 
(defpage "/oauth2/user/tokens" [] 
  (common/layout 
    [:h4 "Authorized Applications "]
    [:p "The following applications are authorized to access your data"]
    [:p (auth-tokens)]
    ))
  
; Revoke a granted user token
; todo; Check if user OWNS the token!
(defpage "/oauth2/user/revoke" {:keys [token]} 
  (println "Delete token id=" token)
  (db/delete-token! token)
  (resp/redirect "/oauth2/user/tokens"))

