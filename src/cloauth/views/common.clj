(ns cloauth.views.common
  (:use noir.core
        hiccup.core
        hiccup.page-helpers)
  (:require [cloauth.models.db :as db]
            [gitauth.gitkit :as gitkit]
            ))

; Define all of the CSS and JS includes that we might need
(def includes {:jquery (include-js "http://ajax.googleapis.com/ajax/libs/jquery/1.6.1/jquery.min.js")
               :jquery-ui (include-js "https://ajax.googleapis.com/ajax/libs/jqueryui/1.8.2/jquery-ui.min.js")
               :jquery-local (include-js "js/jquery-1.6.2.min.js")
               :jquery-ui-local (include-js "js/jquery-ui-1.8.16.custom.min.js")
               :bootstrap (include-css "/css/bootstrap.css")
             
               :google-apis (include-js "https://ajax.googleapis.com/ajax/libs/googleapis/0.0.4/googleapis.min.js")
               :jsapi (include-js "https://ajax.googleapis.com/jsapi")
              })

; 

(defpartial build-head [incls scripts]
            [:head
             [:meta {:charset "utf-8"}]
             [:title "Cloauth"]
             (map #(get includes %) incls)
             (map #(get gitkit/javascripts %) scripts) 
              [:style {:type "text/css"}  "body { padding-top: 60px;}  "]
             ])

(def admin-links [{:url "/admin" :text "Admin/Main"}
                  {:url "/test" :text "Test Page"}
                ])

(def client-links [{:url "/client/register" :text "Register Client"}
                    {:url "/client/admin" :text "Manage Clients"}])

(def main-links [{:url "/oauth2/user/tokens" :text "Authorized Applications" }])

(def all-links (flatten [main-links admin-links client-links]))

; todo set default class for link items?
(defpartial link-item [{:keys [url cls text]}]
            [:li (link-to url text)])

; Navigation Side bar
(defpartial nav-content []
  [:div.sidebar
   [:div.well
   [:h2 "Links"]
   [:h5 "Admin"]
   [:ul (map #(link-item %) admin-links)]
   [:h5 "Client"]
   [:ul (map #(link-item %) client-links)]
   [:h5 "My Apps"]
   [:ul (map #(link-item %) main-links)]]])

;; Display the logged in user name or a login link
; The chooser div will get a GIT Sign in Button inserted via Javascript
(defpartial logged-in-status [] 
  (let [u (db/current-userName)]
  (if u  ; If user logged in?
    [:span (link-to "/profile" u) " -" (link-to "/authn/logout" "Logout")]
    [:div#chooser "Login"])))

; Top mast header
(defpartial topmast-content []
       [:div.topbar 
         [:div.topbar-inner
          [:div.container-fluid
           [:a.brand {:href "/"} "CloAuth"]
           [:ul.nav
            [:li.active (link-to "/" "Home")]
            [:li (link-to "/about" "About")]
           ]
           [:p.pull-right (logged-in-status)]]]])

  
(defn header []
  (if (db/logged-in?)
    (build-head [:bootstrap :jquery :jquery-ui ] [] )
    (build-head [:bootstrap :jquery :jquery-ui :jsapi :google-apis] [:git-load :git-init])))

;; Layouts

;(defpartial layout-with-header [header & content]
;  (layout content))


; Layout with an include map for optional css / js
(defpartial layout-with-includes [ {:keys [css js]} & content]
  ;(prn "option map " css js "content " content)
  (html5 {:lang "en"}
        (header)
            [:body
             (topmast-content)
              [:div.container-fluid 
               (nav-content)
               [:p]
               [:p]
              [:div.content 
               [:div.hero-unit content ]       
              
               [:footer  [:p "Copyright (c) 2011 Warren Strange"]]]]]))

; Standard layout - no additional javascript or css
(defpartial layout [& content]
  ;(prn "Layout " content)
  (layout-with-includes {}, content))
                              
