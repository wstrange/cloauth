(ns cloauth.views.oauth 
  "View and Route handlers for our OAuth entry points
  
   uses the oauth request functions in cloauth.oauth2
  OAuth Spec: http://tools.ietf.org/html/draft-ietf-oauth-v2-22"
  (:require [noir.response :as resp]
            [noir.session :as session]
            [cloauth.oauth2 :as oauth]
            [cloauth.token :as token]
            [cloauth.models.kdb :as db]
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


(defn- request-granted [request] 
  "The user has granted the request " 
  
  (let [clientId (:clientId request) 
        userId (:userId request)
        scopes (:scopes request)]
        
  ;Remember the users decision so we don't need to re-prompt them in the future"
  (db/create-grant clientId  userId  scopes (token/generate-token))
   
  (if (= (:responseType request) "token")
     ; token type - 2 legged oauth - we send back an access token - not a code token
     ; todo does this require JSONP ? So the client JS can parse the fragment
     ; in this case the callback might be a javascript function to call?
     ;(resp/json (token/new-access-token clientId userId scopes))
     (send-redirect request (token/new-access-token clientId userId scopes))
     ; else - redirect back to web app
     (send-redirect request (token/create-auth-code request)))))
     
(defn- request-denied [request] 
   (error-response (merge request {:error_code "access_denied"})))


;; Authorization Endpoint 
;  used to obtain authorization from the
;      resource owner via user-agent redirection
; http://code.google.com/apis/accounts/docs/OAuth2WebServer.html
;
; These look like google specific extensions:
; approval_prompt = force or auto to force reprompt for consent
; access_type = online / offline . offline triggers a refresh token to be sent with the first access token 

(defpage "/oauth2/authorize"  {:keys [client_id response_type redirect_uri scope state access_type approval_prompt] :as req} 
  ;(prn "Authorize  Request client=" client_id " type " response_type " redirect " redirect_uri)
  (let [userId (db/current-userId)
        request (oauth/new-oauth-request client_id userId response_type redirect_uri scope state access_type)]
    (prn "Created request " request)
    (if (:error_code request)
      (error-response request)
      ; else - get consent
      (if (or (= approval_prompt "force") 
              (not (oauth/request-already-approved? request)))
        (render "/oauth2/consent" {:oauth-request request})
        ;else - request is preapproved 
        (request-granted request)))))

; User Consent page 
; User must approve / deny the request
; TODO: Support pluggable consent pages for apps, or better ways
; of registering the "scope" description
(defpage "/oauth2/consent" {:keys [oauth-request] :as req}  
  ;(prn "OAuth request " oauth-request )
  (session/flash-put! :oauth-request oauth-request)
  (let [clientId (:clientId oauth-request)       
        client (db/get-client-by-clientId clientId)]
    (common/layout
      [:h2 "Approve Application Access Request"]
      [:h4 "Organization Requesting Access: " (:orgName client)]
      [:h4 "Organization Description:" (:description client)]
      [:p "Access Scope Requested: " (str (:scopes oauth-request)) ]
      [:br ]
      [:p  "Please "  (link-to "/oauth2/consent/decide?d=grant" "Grant")
          "  or "
          (link-to "/oauth2/consent/decide?d=deny" "Deny" )
          " this request"
          ])))

(defpage "/oauth2/consent/decide" {:keys [d] } 
  (if-let [request (session/flash-get :oauth-request)] 
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

; Note /oauthclient/* uri - so we can use client authentication instead of user auth
; todo: We need a better system...

; Note: some of these keys may be nil as they are not present in all requests
(defpage [:any "/oauthclient/token"]  {:keys [client_id client_secret redirect_uri grant_type code refresh_token] :as req} 
  (println "Token Endpoint Request " req)
  (resp/json 
    (cond 
      ; todo: Implement this check as an auth filter...
      (not (oauth/valid-client? client_id client_secret))
       {:error :unauthorized_client
        :error_description "Client is not authorized. Bad client id or secret?"}
              
      (= grant_type "authorization_code")
      (oauth/handle-authcode-grant client_id redirect_uri code)
      
      (= grant_type "refresh_token") 
      (oauth/handle-refresh-token-grant client_id refresh_token)
      
      :else 
      {:error :invalid_request :error_description (str "Invalid or missing grant type=" grant_type)})))

; Validate an access token 
(defpage [:any "/oauthclient/validate"]  {:keys [client_id client_secret access_token]}
  (println "Validate Token")
  (resp/json  {:todo "todo"}))

; Use a refresh token to get a new access token 
; Happens (for ex) when an access token expires
(defpage [:any "/oauthclient/refresh"] {:keys [client_id client_secret refresh_token]}
  (println "Refresh access token ")
  (resp/json {:todo "toodo"}))
              
 

(defpage "/oauth2/error" {:keys [error]  :as request}
  (common/layout 
    [:h1 "Error Procesing OAuth request"]
    [:p "An unrecoverable error has occured"]
    [:p "Error Message:" error]))

; Display an auth token 
(defpartial display-grant [grant]
  (println "display grant " grant)
    [:tr
      [:td  (:orgName grant)]
      [:td (:description grant)]
      ;[:td (:scope grant)]
     
      [:td (link-to (str "/oauth2/user/revoke?grant=" (:id grant)) "Revoke Access")]
    ])

;;; User Token Management
; Show the users auth codes
(defpartial user-grants [userId] 
  [:table.table.table-striped
    [:tr 
    [:th "Company"]
    [:th "Description"]
    ;[:th "Scope"]
  
    [:th "Action"]]
  (map #(display-grant %) (db/get-grants userId))])


; User page to review OAuth2 Grants 
(defpage "/oauth2/user/grants" [] 
  (common/layout 
    [:h4 "Authorized Applications "]
    [:p "You have authorized the following applications"]
    [:p (user-grants (db/current-userId))]
    ))
  
; Revoke a granted user token
; todo; Check if user OWNS the token!
(defpage "/oauth2/user/revoke" {:keys [grant]} 
  (db/delete-grant! grant)
  (resp/redirect "/oauth2/user/grants"))

