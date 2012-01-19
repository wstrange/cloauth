(ns cloauth.views.testui
  (:require [cloauth.models.kdb :as db] 
            [cloauth.token :as token] 
            [cloauth.models.testdb :as testdb] 
            [cloauth.views.common :as common]
            [noir.response :as resp]
            
            [clj-http.client :as http])
   (:use noir.core
        hiccup.core
        hiccup.page-helpers
        hiccup.form-helpers
        clojure.data.json))


; Test page
(defpage "/test" []
  (common/layout 
    [:h4 "Test Links"]
    [:p  
     (link-to "/test/login" "Login as Test User")
     [:br]
     (link-to (str "/oauth2/authorize?"
                  (encode-params {:client_id (testdb/testClientId)
                                  :redirect_uri "/test/redirect"
                                  :response_type "code"
                                  :state "teststate"
                                  :scope "test"}))
             "Test Web server - Auth code flow")
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

(defn- mk-url [path]
  (str "http://localhost:8080" path))

;; testing code 

; Pretend to be a client making an oauth Authorization request 



; Redirect callback URL for testing purposes
; The same redirect url is used for both 2 legged (auth_code) and 3 legged response
(defpage "/test/redirect" {:keys [code access_token token_type expires_in] :as req} 
  (common/layout 
    [:h2 "Redirect test page" ]
    (cond code 
      [:div 
       [:p "Use code to get an access token"]
       [:p (link-to (str "/test/get-token?code=" code) "Get Access Token")]]
      access_token 
        [:div 
        [:p "Access Token " access_token " Expires " expires_in ]
        [:p (link-to (str "/test/resource?access_token=" access_token ) "Make a Test Request")]]
       :else
       [:p "There was an error " (:error req)])))

; Show the result of exchanging a auth code for a token
(defpage "/test/get-token" {:keys [code]}
  (let [client (db/get-client-by-clientId (testdb/testClientId))
        id (:clientId client)
        secret (:clientSecret client)
        params {:code  code 
                :grant_type "authorization_code"
                :client_id  id
                :client_secret secret
                :redirect_uri (:redirectUri client)}
        result (http/post (mk-url "/oauthclient/token" )
                          {:form-params params  
                           :content-type :json
                           :basic-auth [id secret]})]
    (common/layout 
      [:h1 "Access Token Result"]
      (if (= (:status result) 200)
        (let  [body (:body result) 
               json (read-json body)
               token (:access_token json)]
        [:div 
          [:p"Retrieved token: " json ]
          [:p (link-to (str "/test/resource?access_token=" token ) "Make a Test Request")]])
        ; else
        [:p "There was an error. " (prn result)]))))

; Simulates a protected resource
(defpage "/test/resource" {:keys [access_token]} 
  (common/layout
    [:p "Resource Access Test: access_token=" access_token ]
    (if-let [t (token/get-token-entry access_token)]
      [:div 
         [:p "Token found (OK)" ]
         [:p "Scopes = " (str (:scopes t))]]
      ; else
      [:p "Token is invalid"])))


  