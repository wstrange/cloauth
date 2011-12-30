(ns cloauth.views.client 
  (:require [noir.response :as resp]
             [noir.session :as session]
             [cloauth.views.common :as common]
             [cloauth.models.db :as db])
  (:use noir.core
        hiccup.core
        hiccup.page-helpers
        hiccup.form-helpers))


(defn- mktext [keyval text] 
  [:div.clearfix
    (label keyval text) 
   [:div.input  
   (text-field {:class "large" :size "40" } keyval text)]])
 
; todo - move these to util
(defpartial simple-post-form [url form-map]
  (form-to [:post url] 
           [:fieldset
           
             (map #(mktext (key %) (val %)) form-map)
           [:div.actions
            [:button.btn.primary "Submit"]
            [:button.btn {:type "reset"} "Cancel"]]]))

;; Client registration 

(defpage "/client/register"  {:as req}
  (common/layout 
      [:p "Register New Client" ]
     [:div
      (simple-post-form "/client/register" 
                      {:companyName "Company Name"
                       :description "Description"
                       :redirectUrl "Redirect URL"})]))


(defpage [:post "/client/register"]  {:keys [companyName description redirectUrl] :as form}
  (println "register " form)
  (db/insert-client! (db/new-client companyName description redirectUrl))
  (session/flash-put! "Added new Client")
  (resp/redirect "/"))
  

; Display a single read only client record
(defpartial display-client [clientRec] 
  [:tr
    [:td (:ownerId clientRec)]
    [:td (:companyName clientRec)]
    [:td (:description clientRec)]
    [:td (link-to (str "/client/admin/delete?clientId=" (:clientId clientRec)) "delete")]])
  
(defpartial display-clients [] 
  [:table.zebra-striped
   [:thead
   [:tr 
    [:th {:width "20%"} "Owner"]
    [:th {:width "30%"} "Company"]
    [:th {:width "40%"} "Description"]
    [:th  {:width "10%"} "Action"]]]
    
   [:tbody
   (map #(display-client %)  (db/query-client nil ))]])

; Admin clients
; todo: Check role if admin - they can see all clients
;  - if user - only clients they have registered
(defpage "/client/admin" [] 
  (common/layout  
    [:h4 "Registered Clients"]
    [:p (display-clients) ]))

(defpage "/client/admin/delete" {:keys [clientId]}
  (db/delete-client!  {:clientId clientId})
  (render "/client/admin"))

