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
  




