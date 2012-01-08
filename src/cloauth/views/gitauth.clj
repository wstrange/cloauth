(ns cloauth.views.gitauth
"Authentication Functions for log in
This uses the Google Identity Toolkit (GIT)
See http://code.google.com/apis/identitytoolkit/v1/acguide.html
"
  (:require [cloauth.views.common :as common]
            [cloauth.models.kdb :as kdb]
            [cloauth.util :as util]
            [noir.session :as session]
            [noir.validation :as vali]
            [noir.response :as resp]
            [noir.request :as request]
            [clojure.string :as string]  
            [clj-http.client :as client]
            [clj-json.core :as json]
            [gitauth.gitkit :as gitkit])
  (:use noir.core
        hiccup.core
        hiccup.page-helpers
        hiccup.form-helpers))


;; Auth check routes
;; TODO: 


;; Google Identity Toolkit (GIT) Integration


; GIT will post back to query to see if this user is already registered
; Should return a json response of {"registered" :true/false } as appropriate 
(defpage [:post "/authn/userstatus"] {:keys [email]} 
  (println "/userstatus check" email)
  
  (resp/json {:registered (kdb/is-registered? email)}))


; After prompting for a password for a legacy account, 
; GITkit will need to know if a password is correct or incorrect. 
; This endpoint's URL is defined in the JavaScript widget's loginUrl parameter. 
; GITkit will make a POST to this endpoint and expects a JSON object as a response with one parameter.
; If a user entered their password correctly, you should create a 
; user session and log the user in. Then return the following response to GITkit:
; status: This is one of the following:
; "OK" - user entered password correctly
; "passwordError" - password incorrect
;
; Since we don't use passwords this is a no-op
; TODO: Should we return an error?
(defpage [:post "/authn/logincheck"] {:keys [email password]}
  (println "/logincheck " email " pw=" password)
  (resp/json {:status "ERROR"}))



; The verify callback url to GIT
(def verifyurl (str "https://www.googleapis.com/identitytoolkit/v1/relyingparty/verifyAssertion?key=" gitkit/apikey))

(defn requestUri [req] 
  " todo: why does (:scheme req ) put a : in front of http??"
  (str "http://" (:server-name req) ":" (:server-port req) (:uri req)))
  
; Create the html response to return to GIT 
; The argument js will be a javascript call that denotes success/failure
(defn- generate-git-response [js]
   (html [:body
          (include-js "https://ajax.googleapis.com/jsapi")
          (javascript-tag "google.load('identitytoolkit', '1.0', {packages: ['notify']});")
          (javascript-tag js)]))
          

; Called after the login has been been verified. 
; Log the user in and return the response to GIT
; The "res" param is going to be a map of key/value pairs that we can use to 
; seed the user record in the db. This is a bit of a hack in that 
; the User has a proper type which we should create.. fix??
;
; todo: create a stub record with registered = false
; See http://code.google.com/apis/identitytoolkit/v1/acguide.html
(defn login-verifed [res]
  (let [email (:verifiedEmail res)
        user (kdb/get-user email)
        missing (nil? user)
        js (json/generate-string {:email email :registered (not missing)})]
        (println "Login verfied js=" js  " user " user)
    (if missing 
      (session/put! :signup (merge res {:userName email})))
     
    (kdb/login! user)
    (generate-git-response (str "window.google.identitytoolkit.notifyFederatedSuccess(" js  ")"))))


(defn login-failed [res]
   (generate-git-response "window.google.identitytoolkit.notifyFederatedError()"))
    
  
;; GIT will callback to this  URL (GET or POST) after the user has signed on 
; at the IDP. You must POST back to GITs verification URL to verify the login
;
;
; If the authentication is valid git returns a 200 response and a json body with a bunch of info (verifiedEmail,..)
;; See http://code.google.com/apis/identitytoolkit/v1/acguide.html
; and http://code.google.com/apis/identitytoolkit/v1/reference.html  
;
; This is a pre-route because we need access to the request params... 
; Doesn't work if a defpage... not sure why..

(pre-route [:any "/authn/callback"] {:as req} 
  (let [body (json/generate-string 
               ; GIT expects a JSON encoded response like this:
               { :requestUri (requestUri req) :postBody (encode-params (:params req))})   
        ; POST back to GIT
        result (client/post  verifyurl {:body body  :content-type :json})]
    (if (= (:status result) 200) 
      ; GIT sends a 200 if the user is authenticated/validated
      (login-verifed (json/parse-string (:body result) true))
      (login-failed (:body result)))))

; Log the user out
(defpage "/authn/logout" []
  (session/clear!)
  (resp/redirect "/"))


;; git will callback to this url if the user has been verified
;; but has not been registered
; You want to let the user edit any profile data here and submit to consent to 
; account creation. 
; The session variable :signup contains the map of the IDP verified parameters that 
; we will use to create the account.
; the login-verifed function writes :signup session var
;
; If the user cancels we redirect to / and no account will be created
(defpage [:any "/authn/signup"] {:keys [email] }
  (let [user (session/get :signup)]
   (println "Signup User rec " user)
  (common/layout 
    [:h1 "Signup page"]
    [:p "Please complete the new user registration process"]
    [:p "You have signed up as " email ]  
    (form-to [:post "/authn/signup/complete" ]
             (label :displayName "Display Name") 
             (text-field :displayName (:displayName user))
             [:br]
               [:button "Submit"] )
    [:br]
    (link-to "/" "Cancel Registration"))))

; The user has completed registration
; Create their account and create a session for them
(defpage [:post "/authn/signup/complete"] {:keys [displayName] }
  (println "Complete Registration")
  (let [u (session/get :signup)
        edits {:displayName displayName}
        user (merge u edits)
        ]
    (println "u " user )
    (kdb/insert-user! user )
    ; force the login - which triggers read of newly inserted
    ; record 
    (kdb/login! (kdb/get-user (:userName user)))
    ; nuke the signup session var as it is not needed anymore
    (session/remove! :signup)) 
  (resp/redirect "/welcome")) 

(defpage "/signup/cancel" {:keys [userName]}
  (render "/authn/logout"))
  
