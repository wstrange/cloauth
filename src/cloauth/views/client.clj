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
  [:div.formtext 
   (label {:class "span-3" } keyval text) 
   (text-field {:class "span-4.last" :size 40} keyval text)
   [:br]])
 
; todo - move these to util
(defpartial simple-post-form [url form-map]
  (form-to [:post url] 
           (map #(mktext (key %) (val %)) form-map)
           [:br]
           [:button "Submit"]))

;; Client registration 

(defpage "/client/register"  {:as req}
  (common/layout 
    [:p "Register New Client" ]
    (simple-post-form "/client/register" 
                      {:companyName "Company Name"
                       :description "Descripition"
                      :redirectUrl "Redirect URL"})))


(defpage [:post "/client/register"]  {:keys [companyName description redirectUrl] :as form}
  (println "register " form)
  (db/insert-client! (db/new-client companyName description redirectUrl))
  (session/flash-put! "Added new Client")
  (resp/redirect "/"))
  

; Display a single read only client record
(defpartial display-client [clientRec] 
  [:tr
    [:td (link-to (str "/client/admin/delete?clientId=" (:clientId clientRec)) "delete")]
    [:td (:ownerId clientRec)]
    [:td (:companyName clientRec)]
    [:td (:description clientRec)]])
  
(defpartial display-clients [] 
  [:table 
   [:tr 
    [:th  {:width "10%"} "Action"]
    [:th {:width "20%"} "Owner"]
    [:th {:width "30%"} "Company"]
    [:th {:width "40%"} "Description"]]

   (map #(display-client %)  (db/query-client nil ))])

; Admin clients
; todo: Check role if admin - they can see all clients
;  - if user - only clients they have registered
(defpage "/client/admin" [] 
  (common/layout  
    [:h1 "Registered Clients"]
    [:p (display-clients) ]))

(defpage "/client/admin/delete" {:keys [clientId]}
  (db/delete-client!  {:clientId clientId})
  (render "/client/admin"))

