(ns cloauth.views.testui
  (:require [cloauth.models.db :as db] 
            [cloauth.views.common :as common]
            [noir.response :as resp]
            [clj-http.client :as http])
   (:use noir.core
        hiccup.core
        hiccup.page-helpers
        hiccup.form-helpers
        clojure.data.json))


;; Create a test user 

; userName firstName lastName verifiedEmail roles

(def testuser1 (db/new-user "test1" "test" "tester1" "test1@test.com" '(:user :admin)))

(defn create-test-user [] 
  (if (db/get-user "test1")
    (println "test user exists")
    (db/add-user! testuser1)))

;; For testing

(create-test-user)

(defpage "/test" []
  (common/layout 
    [:h1 "Test Links"]
    [:p   
     
     (link-to "/test/login" "Login Test User")
     [:br]
     (link-to "/test/authorize" "Test OAuth Authorize")
     ]))


; url that performs login on a test user 
; This is convenience for testing that provides fast login
(defpage "/test/login" []
  (db/login! (db/get-user "test1"))
  (resp/redirect "/welcome"))

(defpage "/test/sampledata" []
  (resp/redirect "/"))

(defn- mk-url [path]
  (str "http://localhost:8080" path))

;; testing code 

(def test-client-id "wj6F0pX70z3ZgN1skf7m6COjaeFXL3kG" )

; Pretend to be a client making an oauth Authorization request 
(defpage "/test/authorize" []
  (common/layout 
    [:p  "Test Authorize "]
    (link-to (str "/oauth2/authorize?"
                  (encode-params {:client_id test-client-id
                                  :redirect_uri "/test/redirect"
                                  :response_type "code"
                                  :state "teststate"
                                  :scope "test"}))
             "Authorize")))


; Rediret URL for testing purposes
(defpage "/test/redirect" {:keys [code] :as req} 
  (common/layout 
    [:h2 "Redirect test page" ]
    (if code 
      [:div 
       [:p "Use code to get an access token"]
       [:p (link-to (str "/test/get-token?code=" code) "Get Access Token")]]
       ; else
       [:p "There was an error " (:error req)])))

; Show the result of exchanging a auth code for a token
(defpage "/test/get-token" {:keys [code]}
  (let [client (db/get-client-by-clientId test-client-id)
        id (:clientId client)
        secret (:clientSecret client)
        params {:code  code 
                :grant_type "authorization_code"
                :client_id  id
                :client_secret secret
                :redirect_uri (:redirectUri client)}
        result (http/post (mk-url "/client/token" )
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

 
(defpage "/test/resource" {:keys [access_token]} 
  (common/layout
    [:p "Test Access for " access_token ]))
  