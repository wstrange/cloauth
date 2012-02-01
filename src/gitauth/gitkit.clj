(ns gitauth.gitkit
  (:use noir.core
        hiccup.core
        hiccup.page-helpers)
   (:require 
            [cheshire.core :as json]))


; Get our google API key stored on disk or in an environment var (heroku)
(def apikey 
  (or (System/getenv "GOOGLE_API_KEY")
    (slurp "api-key")))

; todo: find out a better way of doing this..
(def deploy-url
  (if (System/getenv "GOOGLE_API_KEY")
    "http://cloauth.herokuapp.com"
    ; else
    "http://localhost:8080"))
    
;; Google Git parameters for this app
;; You can figure these out by going to your google api console and having it generate 
;; the Javascript login widget code
(def gitkit-params {:developerKey apikey
                    :companyName "Noir Test"
                    :callbackUrl (str deploy-url "/authn/callback")
                    :realm ""
                    :userStatusUrl "/authn/userstatus"
                    :loginUrl "/authn/login"
                    :signupUrl "/authn/signup"
                    :homeUrl "/welcome"
                    :logoutUrl "/authn/logout"
                    :language "en"
                    :idps ["Gmail", "Yahoo", "AOL", "Hotmail"],
                    :tryFederatedFirst true
                    :useCachedUserStatus false})

; git params as JSON
(def git-params-json (json/generate-string gitkit-params))


; Define scripts that are needed by GIT. See the GIT docs
; The script generates a Sign In button that is inserted in the "chooser" div
(def javascripts {:git-load (javascript-tag " google.load('identitytoolkit', '1', {packages: ['ac']});")
                  :git-init (javascript-tag (str "$(function(){window.google.identitytoolkit.setConfig(" 
                       git-params-json ");$('#chooser').accountChooser();});"))})
