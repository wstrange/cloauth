(ns cloauth.views.welcome
  (:require [cloauth.views.common :as common]
            [noir.session :as session]
            [cloauth.models.kdb :as db])
  (:use noir.core    
        hiccup.core
        hiccup.page-helpers))

(defpage "/" []
         (common/layout
           [:h3 "OAuth AZ Server"]
           [:p "Welcome"]
           [:p "This is an experimental OAuth provider written in Clojure"]
           [:p "To get started login with an openid account, or use the 'test' account login available under the "
            (link-to "/test" "test page menu")]
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
