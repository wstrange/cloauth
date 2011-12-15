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
               :blueprint (include-css "/css/blueprint/screen.css")
               :default (include-css "/css/default.css")
               :reset (include-css "/css/reset.css")
               :util.js (include-js "/js/util.js")
               :google-apis (include-js "https://ajax.googleapis.com/ajax/libs/googleapis/0.0.4/googleapis.min.js")
               :jsapi (include-js "https://ajax.googleapis.com/jsapi")
              })

; 

(defpartial build-head [incls scripts]
            [:head
             [:title "Cloauth"]
             (map #(get includes %) incls)
             (map #(get gitkit/javascripts %) scripts) 
             ])

(def admin-links [{:url "/admin" :text "Admin/Main"}
                  {:url "/admin/users" :text "Add Users"}
                  {:url "/test" :text "Test Page"}
                ])

(def client-links [{:url "/client/register" :text "Register Client"}])

(def main-links [{:url "/admin" :text "Admin"}])
(def all-links (flatten [admin-links client-links]))


(defpartial link-item [{:keys [url cls text]}]
            [:li
             (link-to {:class cls} url text)])

; Navigation Side bar
(defpartial nav-content []
  [:div.nav 
   [:h2 "Links"]
   [:ul.nav (map #(link-item %) all-links)]
   ])

;; Display the logged in user name or a login link
; The chooser div will get a GIT Sign in Button inserted via Javascript
(defpartial logged-in-status [] 
  (let [u (db/current-userName)]
  (if u  ; If user logged in?
    [:div u " - " (link-to "/authn/logout" "Logout")]
    [:div#chooser "Login"])))

; Top master header
(defpartial header-content []
       [:div.header 
         [:br]
         [:h2.span-10  (link-to "/" "CloAuth")]
         [:p.span-8.last {:align "right"}(logged-in-status)]
         [:hr]
         ])

  
(defn header []
  (if (db/logged-in?)
    (build-head [:blueprint :jquery :util.js :jquery-ui :default] [] )
    (build-head [:blueprint :jquery :util.js :jquery-ui :jsapi :google-apis] [:git-load :git-init])))

;; Layouts

;(defpartial layout-with-header [header & content]
;  (layout content))


; Layout with an include map for optional css / js
(defpartial layout-with-includes [ {:keys [css js]} & content]
  ;(prn "option map " css js "content " content)
  (html 
        (header)
            [:body
              [:div.container.showgrid             ; change showgridx to showgrid to show blueprint grid
                (header-content)  ; 24 col wide header
                [:div.span-4 (nav-content)]          ; Nav bar that is 4 cols wide
                [:div.span-20.last content ]         ; 20 cols for content
                [:p.span-24 " "]   ; space 
                [:hr]
                [:div {:class "clear prepend-8 last"}  "Copyright (c) 2011 Warren Strange"] ; footer
              ]
            ]))

; Standard layout - no additional javascript or css
(defpartial layout [& content]
  ;(prn "Layout " content)
  (layout-with-includes {}, content))
                              
