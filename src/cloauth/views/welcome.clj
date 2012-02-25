(ns cloauth.views.welcome
  (:require [cloauth.views.common :as common]
            [noir.session :as session]
            [cloauth.models.kdb :as db])
  (:use noir.core    
        hiccup.core
        hiccup.page-helpers))

(def test-page-link   (link-to "/test" "test page"))

(defpage "/" []
         (common/layout
           [:h3 "OAuth AZ Server"]          
           [:p "This is an experimental OAuth provider written in Clojure"]
         
           (if-let [user (db/current-userName) ]
             [:div 
              [:p "Welcome " user ]
              [:p "You can test sample OAuth flows from the " test-page-link]]
             ; else
             [:div   
              [:p "To get started login with an openid account using the Sign In Button (upper right corner) " 
               "or use the 'test' account login available under the " test-page-link]])

           [:p "Get the source on " (link-to "https://github.com/wstrange/cloauth" "github")]))

;; Todo - check for continuing request redirect
(defpage "/welcome" []
         (common/layout
           [:h3 "OAuth AZ Server"]
           [:p "Logged in user landing page"]
           [:p "Admin user ="  (if (db/user-is-admin?) "true" "false")]))

; Page used to force login
; doto: figure out how to trigger JS login ... 
(defpage "/login" []
         (common/layout
           [:script "$('#navbar').accountChooser('showAccountChooser');"]
           [:h1 "To continue Please login First"]
           [:p "Log in "])) 
