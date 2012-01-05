(ns cloauth.views.welcome
  (:require [cloauth.views.common :as common]
            [noir.session :as session])
  (:use noir.core    
        hiccup.core
        hiccup.page-helpers))

(defpage "/" []
         (common/layout
           [:h2 "OAuth AZ Server"]
           [:p "Welcome"]
           [:p (session/flash-get)]))

;; Todo - check for continuing request redirect
(defpage "/welcome" []
         (common/layout
           [:h1 "OAuth AZ Server"]
           [:p "Logged in user landing page"]))

; Page used to force login
; doto: figure out how to trigger JS login ... 
(defpage "/login" []
         (common/layout
           [:script "$('#navbar').accountChooser('showAccountChooser');"]
           [:h1 "To continue Please login First"]
           [:p "Log in "])) 
