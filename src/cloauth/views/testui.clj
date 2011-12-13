(ns cloauth.views.testui
  (:require [cloauth.models.db :as db] 
            [cloauth.views.common :as common]
            [noir.response :as resp]
               [clj-http.client :as client])
   (:use noir.core
        hiccup.core
        hiccup.page-helpers
        hiccup.form-helpers))


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

;; testing code 
(defn call-auth [] 
  (client/get "http://localhost:8080/oauth2/auth/"))

(defn call2 [] 
  (client/request {:method :get :url  "http://localhost:8080/oauth2/auth"}))


  
; Pretend to be a client making an oauth request 
(defpage "/test/authorize" []
  (common/layout 
    [:p  "Test Authorize "]
    (link-to (str "/oauth2/authorize?"
                  (encode-params {:client_id "wj6F0pX70z3ZgN1skf7m6COjaeFXL3kG" 
                                  :response_type "code"}))
             "Authorize")))


  