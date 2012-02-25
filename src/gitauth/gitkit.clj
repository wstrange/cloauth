(ns gitauth.gitkit
  "Integration with Google Identity Toolkit"
  (:use noir.core
        hiccup.core
        hiccup.page-helpers)
   (:require 
            [cheshire.core :as json]
            [cloauth.models.kdb :as db]))


; Get our google API key stored on disk or in an environment var (heroku)
(def apikey 
  (or (System/getenv "GOOGLE_API_KEY")
    (slurp "api-key")))

; todo: find a better way of doing this..
(def deploy-url
  (if (System/getenv "GOOGLE_API_KEY")
    "http://cloauth.herokuapp.com"
    ; else
    "http://localhost:8080"))
    
;; Google Git parameters for this app
;; You obtain these from your google api console
;; Have it generate the Javascript login widget code for you
(def gitkit-params {:developerKey apikey
                    :companyName "Noir Test"
                    :callbackUrl (str deploy-url "/authn/callback")
                    :realm ""
                    :userStatusUrl "/authn/userstatus"
                    :loginUrl "/authn/login"
                    :signupUrl "/authn/signup"
                    :homeUrl "/"
                    :logoutUrl "/authn/logout"
                    :language "en"
                    :idps ["Gmail", "Yahoo", "AOL", "Hotmail"],
                    :tryFederatedFirst true
                    :useCachedUserStatus false})

; git params as JSON
(def git-params-json (json/generate-string gitkit-params))

; Generates the Javascript to load GIT toolkit and render the sign in button
; For this app we use the id=chooser for the button location
(defpartial generate-git-javascript []
  (javascript-tag (str "
google.load('identitytoolkit', '1', {packages: ['ac']});
$(function(){ 
window.google.identitytoolkit.setConfig("
git-params-json 
");
$('#chooser').accountChooser();
"
; If the user is logged in this will modify the rendered button to show their username
(if-let [u (db/current-userName)] 
  (str "window.google.identitytoolkit.showSavedAccount('" u "');" )
  "")
"});")))
  
