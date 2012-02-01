(ns cloauth.views.common
  (:use noir.core
        hiccup.core
        hiccup.page-helpers
         hiccup.form-helpers)
  (:require [cloauth.models.kdb :as db]
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

; create the page <head>
(defpartial build-head [incls scripts]
            [:head
             [:meta {:charset "utf-8"}]
             [:title "Cloauth"]
             (map #(get includes %) incls)
             (map #(get gitkit/javascripts %) scripts) 
              [:style {:type "text/css"}  "body { padding-top: 60px;}  "]
             ])
; "Menu" data structure :title  :check (optional fn to call to see if the menu should be rendered) :links 
(def client-menu {:title "Client"
                  :links [["/client/register" "Register Client"]
                          ["/client/admin" "Manage Clients"]]})

(def apps-menu  {:title "My Applications" 
                 :links [["/oauth2/user/grants" "Authorized Applications" ]]})

(def admin-menu {:title "Admin"  
                 :check-fn db/user-is-admin?
                 :links 
                 [["/admin/user" "Admin/Main"]
                 ]})   

(def test-menu {:title "Test Pages"
                :links  [["/test" "Test Page"]]})

; todo set default class for link items?
(defpartial link-item [{:keys [url cls text]}]
            [:li (link-to url text)])

(defn menu-items [links]
  (for [[url text] links] 
    [:li (link-to url text )]))

(defpartial render-menu [{:keys [title check-fn links]}]
  (if (or (nil? check-fn) 
          (check-fn))
    [:div 
     [:h5 title]
     [:ul (menu-items links)]]))
                          

; Navigation Side bar
(defpartial nav-content []
  [:div.sidebar
   [:div.well
    (render-menu admin-menu)
    (render-menu test-menu)
    (render-menu client-menu)
    (render-menu main-menu)]])


;; Display the user name or a login link if the user has not logged in
; The chooser div will get a GIT Sign in Button inserted via Javascript
(defpartial logged-in-status [] 
  (let [u (db/current-userName)]
  (if u  ; If user logged in?
    [:span (link-to "/user/profile" u) " -" (link-to "/authn/logout" "Logout")]
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
                              

(defn- mktext [keyval text] 
  [:div.clearfix
    (label keyval keyval) 
   [:div.input  
   (text-field {:class "large" :size "40" } keyval text)]])
 
; 
(defpartial simple-post-form [url form-map]
  (form-to [:post url] 
           [:fieldset
           
             (map #(mktext (key %) (val %)) form-map)
           [:div.actions
            [:button.btn.primary "Submit"]
            [:button.btn {:type "reset"} "Reset"]]]))
