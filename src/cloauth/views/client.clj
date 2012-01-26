(ns cloauth.views.client 
  "Client Views"
  (:require [noir.response :as resp]
             [noir.session :as session]
             [cloauth.views.common :as common]
             [cloauth.models.kdb :as kdb])
  (:use noir.core
        hiccup.core
        hiccup.page-helpers
        hiccup.form-helpers))


;; Client registration 

(defpage "/client/register"  {:as req}
  (common/layout 
      [:p "Register New Client" ]
     [:div
      (common/simple-post-form "/client/register" 
                      {:orgName "Organization Name"
                       :description "Description"
                       :redirectUrl "Redirect URL"})]))


(defpage [:post "/client/register"]  {:keys [orgName description redirectUrl] :as form}
  (println "register " form)
  (kdb/insert-client! 
    (kdb/new-client orgName description redirectUrl (kdb/current-userId)))
  (session/flash-put! "Added new Client")
  (resp/redirect "/client/admin"))
  

; Display a single read only client record
(defpartial display-client [clientRec] 
  [:tr
    [:td (:userName clientRec)]
    [:td (:orgName clientRec)]
    [:td (:description clientRec)]
    [:td (:clientId clientRec) [:br] (:clientSecret clientRec)]
    
    [:td (link-to (str "/client/admin/delete?id=" (:id clientRec)) "delete")]])
  
(defpartial display-clients [] 
  [:table.zebra-striped
   [:thead
   [:tr 
    [:th {:width "20%"} "Owner"]
    [:th {:width "20%"} "Organization"]
    [:th {:width "30%"} "Description"]
    [:th {:width "20%"} "id / secret"]
    [:th  {:width "10%"} "Action"]]]
    
   [:tbody
   (map #(display-client %)  (kdb/all-clients))]])

; Admin clients
; todo: Check role if admin - they can see all clients
;  - if user - only clients they have registered
(defpage "/client/admin" [] 
  (common/layout  
    [:h4 "Registered Clients"]
    [:p (display-clients) ]))

(defpage "/client/admin/delete" {:keys [id]}
  (kdb/delete-client! id)
  (resp/redirect "/client/admin"))

