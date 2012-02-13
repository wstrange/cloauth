(ns cloauth.views.testui
  (:require [cloauth.models.kdb :as db] 
            [cloauth.token :as token] 
            [cloauth.models.testdb :as testdb] 
            [cloauth.views.common :as common]
            [noir.response :as resp]
            [noir.request :as request]
            [clj-http.client :as http])
   (:use noir.core
        hiccup.core
        hiccup.page-helpers
        hiccup.form-helpers
        ))


; Test page
(defpage "/test" []
  (common/layout 
    (if-not (db/current-userName)
      [:div
        [:h4 "Test Login"]
        [:p "The 'test' user has the admin role and can access expanded functionality"]
        [:p  (link-to "/test/login" "Login as the Test User")]])
    
    [:h4 "OAuth Flows"]
    [:p "The following links will initiate OAuth flows. 
Rather than have a seperate client for testing, we are using a built in client that will simulate the client side of the
OAuth dance"]
    [:p  "If the user 
has not previously consented they will be asked to review the request. If granted the provider will continue the flow
and return a code token (3 legged) or access token (2 legged) back to the client"]
    
    [:p "If you are not seeing the consent page go to " (link-to "/oauth2/user/grants" "Authorized Applications") " and 
revoke any existing consent"]
    [:hr]
    [:h4 "Three legged (Web server) flow"]
     (link-to (str "/oauth2/authorize?"
                  (encode-params {:client_id (testdb/testClientId)
                                  :redirect_uri "/test/redirect"
                                  :response_type "code"
                                  :state "teststate"
                                  :scope "test"}))
             "Initiate Web server 3 legged OAuth")
  
     [:hr]
     [:h4 "Two Legged OAuth"]
     [:br]
     (let [uri (str "/oauth2/authorize?"
                  (encode-params {:client_id (testdb/testClientId)
                                  :redirect_uri "/test/redirect"
                                  :response_type "token"
                                  :state "teststate"
                                  :scope "test"}))]    
       [:a {:href  uri :target "_blank"} "Test two legged oauth flow"])))

; url that performs login on a test user 
; This is convenience for testing that provides fast login
(defpage "/test/login" []
  (db/login! (db/get-user testdb/testUser))
  (resp/redirect "/"))

(defpage "/test/sampledata" []
  (resp/redirect "/"))

; create a url for making a http/ request
; todo: we should pr
(defn- mk-url [path]
  (let [headers (:headers (request/ring-request))
        host (headers "host")]
    (println "h=" headers)
    (str "http://" host path)))

;; testing code 

; Pretend to be a client making an oauth Authorization request 


; Redirect callback URL for testing purposes
; The same redirect url is used for both 2 legged (auth_code) and 3 legged response
(defpage "/test/redirect" {:keys [code access_token token_type expires_in] :as req} 
  (common/layout 
    [:h2 "Client redirect test page" ]
   
    (cond code 
      [:div  
        [:p "If you see this page the user has consented to allow the client
access to the requested scope. The provider has returned a code token which can be exchanged by the
client for an access token and refresh token"]
       [:p "The access code returned by the provider is " code]
       [:p "This code is valid for a one time use, and will expire in 5 minutes"]
       [:p (link-to (str "/test/get-token?code=" code) "Get an Access Token")]]
     access_token 
        [:div 
        [:p "Access Token " access_token " Expires " expires_in ]
        [:p (link-to (str "/test/resource?access_token=" access_token ) "Make a Test Request")]]
       :else
       [:p "There was an error " (:error req)])))


(defn call-token-endpoint [call-params] 
  "Call the token endpoint on the provider - return a parsed json response or nil on error"
   (let [client (db/get-client-by-clientId (testdb/testClientId))
        id (:clientId client)
        secret (:clientSecret client)
        params (merge { :client_id  id
                        :client_secret secret
                        :redirect_uri (:redirectUri client)}
                      call-params)
        url  (mk-url "/oauthclient/token")
        result (http/post url {:form-params params  
                     :content-type :json
                     :basic-auth [id secret]})]
     
     (if (= (:status result) 200)
        (cheshire.core/parse-string (:body result) true))))

  
; Show the result of exchanging a auth code for a token
(defpage "/test/get-token" {:keys [code] :as req}
   (common/layout 
      [:h1 "Access Token Result"]
      [:p "This is a client side page showing the access token returned to the client.
   This token can be used to access a resource. For the demo there is simulated resource that you can access with the token"]
      (if-let [json (call-token-endpoint {:code  code :grant_type "authorization_code"})]
        (do 
          (println "Got response back from token endpoint " json)
          [:div     
           [:p "Retrieved token: " [:br] json ]
           [:p (link-to (str "/test/resource?access_token=" (:access_token json)
                             "&refresh_token=" (:refresh_token json)) 
                        "Make a Test Resource Request")]])
        ; else
        [:p "There was an error calling the token endpoint. "])))

; Simulates a protected resource
(defpage "/test/resource" {:keys [access_token refresh_token]} 
  (common/layout
    [:h4 "Access Resource Test Page"]
    [:p "This is a client side page that simulates accessing a resource using the granted access token.
Note for the purpose of the demo the access token expiry is quite short. "]
    [:p "We simulate 'accessing' a resource 
by making a direct call to validate the token. If you reload this page in the browser you will see
that the token will eventually expire."]
    [:p "Resource Access Test: access_token=" access_token ]
    (if-let [t (token/get-token-entry access_token)]
      [:div 
         [:p "Token is valid" ]
         [:p "Allowed Scopes = " (str (:scopes t))]]
      ; else
      [:div 
       [:p "Access token has expired."]])
    
    (if refresh_token 
      [:div  
       [:h4 "Refresh Token"] 
       [:p "Refresh token value is " refresh_token]
       [:p " " (link-to (str "/test/refresh?refresh_token=" refresh_token) "Use refresh token ")]])))

(defpage "/test/refresh" {:keys [refresh_token]} 
   (common/layout 
     (if-let [json (call-token-endpoint {:grant_type "refresh_token" :refresh_token refresh_token})]
       [:div 
        [:h4 "Refresh Token Result"]
        [:p "Got back the following response " json]
        [:p (link-to (str "/test/resource?access_token=" 
                          (:access_token json)
                          "&refresh_token=" 
                          (:refresh_token json)) "Access a resource with this token")]]
       ; else
       [:p "There was an error calling the endpoint"])))

    

(defpage "/test/create-data" []
  (testdb/create-sample-data)
  (resp/redirect "/test"))
  