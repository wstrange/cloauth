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
    [:h4 "Test Login"]
    [:p "The 'test' user has the admin role and can access expanded functionality"]
    [:p  (link-to "/test/login" "Login as the Test User")]
    
    [:h4 "OAuth Flows"]
    [:p "The following link will initiate an OAuth 3 legged 'web' server flow. 
Rather than have a seperate client for testing, we are using a built in client that will simulate the client side of the
OAuth flow"]
    [:p  "When this link is clicked the client will request a code token from the provider. If the user 
has not previously consented they will be asked to review the request. If granted the provider will return
code token to the client"]
    
    [:p "If you are not seeing the consent page go to " (link-to "/oauth2/user/grants" "Authorized Applications") " and 
revoke any existing consent"]
    [:p
    
     (link-to (str "/oauth2/authorize?"
                  (encode-params {:client_id (testdb/testClientId)
                                  :redirect_uri "/test/redirect"
                                  :response_type "code"
                                  :state "teststate"
                                  :scope "test"}))
             "Initiate Web server 3 legged OAuth")]
  
     [:p "The following link will initiate a two legged OAuth flow"
      [:br]
     (let [uri (str "/oauth2/authorize?"
                  (encode-params {:client_id (testdb/testClientId)
                                  :redirect_uri "/test/redirect"
                                  :response_type "token"
                                  :state "teststate"
                                  :scope "test"})) ]
       
     [:a {:href  uri 
          :target "_blank"}
          "POP UP - Test two legged oauth flow"])
     ]))

; url that performs login on a test user 
; This is convenience for testing that provides fast login
(defpage "/test/login" []
  (db/login! (db/get-user testdb/testUser))
  (resp/redirect "/welcome"))

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
access to the requested scope. The provider has returned a code token which can now be used by the
client to request an access token"]
       [:p "The access code returned by the provider is " code]
       [:p "This code is valid for a one time use, and will expire in 5 minutes"]
       [:p (link-to (str "/test/get-token?code=" code) "Get an Access Token")]]
     access_token 
        [:div 
        [:p "Access Token " access_token " Expires " expires_in ]
        [:p (link-to (str "/test/resource?access_token=" access_token ) "Make a Test Request")]]
       :else
       [:p "There was an error " (:error req)])))

; Show the result of exchanging a auth code for a token
(defpage "/test/get-token" {:keys [code] :as req}
  (let [client (db/get-client-by-clientId (testdb/testClientId))
        id (:clientId client)
        secret (:clientSecret client)
        params {:code  code 
                :grant_type "authorization_code"
                :client_id  id
                :client_secret secret
                :redirect_uri (:redirectUri client)}
        url  (mk-url "/oauthclient/token")
        result (http/post url
                          {:form-params params  
                           :content-type :json
                           :basic-auth [id secret]})]
    (common/layout 
      [:h1 "Access Token Result"]
      [:p "This is a client side  page that shows the access token returned to the client.
This token can be used to access a resource. We have a simulated resource that you can access with the token"]
      (if (= (:status result) 200)
        (let  [body (:body result) 
               json (cheshire.core/parse-string body true)
               token (:access_token json)]
          [:div
         
           [:p "Retrieved token: " [:br] json ]
           [:p (link-to (str "/test/resource?access_token=" token ) "Make a Test Resource Request")]])
        ; else
        [:p "There was an error. " (prn result)]))))

; Simulates a protected resource
(defpage "/test/resource" {:keys [access_token]} 
  (common/layout
    [:h4 "Access Resource Test Page"]
    [:p "This is a client side page that simulates accessing a resource using the granted access token.
Note for the purpose of the demo the access token expiry is quite short.  We simulate 'accessing' a resource 
by making a direct call to validate the token. If you reload this page in the browser you will see
that the token will eventually expire."]
    [:p "Resource Access Test: access_token=" access_token ]
    (if-let [t (token/get-token-entry access_token)]
      [:div 
         [:p "Token is valid" ]
         [:p "Allowed Scopes = " (str (:scopes t))]]
      ; else
      [:p "Token is invalid"])))


(defpage "/test/create-data" []
  (testdb/create-sample-data)
  (resp/redirect "/test"))
  