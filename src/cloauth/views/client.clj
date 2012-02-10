(ns cloauth.views.client 
  "Client Views"
  (:require [noir.response :as resp]
             [noir.session :as session]
             [noir.validation :as vali]
             [cloauth.views.common :as common]
             [cloauth.models.kdb :as kdb]
             [clojure.string :as str]
             [cloauth.util :as util])
  (:use noir.core    
        hiccup.core
        hiccup.page-helpers
        hiccup.form-helpers))
  

; Display a single read only client record
(defpartial display-client [clientRec] 
  [:tr
    [:td (:userName clientRec)]
    [:td (:orgName clientRec)]
    [:td (:description clientRec)]
    [:td (:clientId clientRec) [:br] (:clientSecret clientRec)]  
    [:td (link-to (str "/client/delete?id=" (:id clientRec)) "delete") ", "
     (link-to (str "/client/edit?id=" (:id clientRec)) "edit")
     ]])
  
(defpartial display-clients [] 
  [:table.table.table-striped.table-condensed
   [:thead
   [:tr 
    [:th  "Owner"]
    [:th "Organization"]
    [:th  "Description"]
    [:th "id / secret"]
    [:th  "Action"]]]
    
   [:tbody
   (map #(display-client %)  (kdb/all-clients))]])


; Admin clients
; todo: Check role if admin - they can see all clients
;  - if user - only clients they have registered
(defpage "/client/admin" [] 
  (common/layout  
    [:h4 "Registered Clients"]
    [:p (display-clients) ]))

(defn- check-and-fetch-client [id]
  "id is the pk of a client record. This looks up the record based on id
  and returns the record if the user has permission to edit it, otherwise nil
   Admins can edit any record. Users can edit records they own"
  (if-let [clientRec (kdb/get-client id)]
    (if (or (kdb/user-is-admin?) 
            ; compare the current user id (pk) to the user_id that owns the record
            (=  (:user_id clientRec) (kdb/current-user-pk)))
      ; OK - return the record 
      clientRec)))


(defpage "/client/delete" {:keys [id]}
  (if (check-and-fetch-client id)
    (do 
      (kdb/delete-client! id)
      (session/flash-put! :message "Client Deleted")
      (render "/client/admin"))
    ; else
    (common/layout 
      [:p "You do not have permissio to delete this record"])))
    
(defn- mk-label [id text]
  (label {:class "control-label"} id text))

  
(defpartial client-fields [client]
  [:div
           [:fieldset  
            (hidden-field :id  (:id client))  
            (hidden-field :user_id  (:user_id client))   
             [:div.control-group 
                (mk-label :clientId "ClientId") 
                [:div.controls 
                 [:input {:type "text"
                          :name "clientId"
                          :id "clientId"
                          :value (:clientId client)
                          :readonly "readonly"}]]
                (mk-label :clientSecret "ClientSecret")
                 [:div.controls 
                 [:input {:type "text"
                          :name "clientSecret"
                          :id "clientSecret"
                          :value (:clientSecret client)
                          :readonly "readonly"}]]]
             
             
            [:div.control-group
             (mk-label :generateSecret "Generate new secret?")
              [:div.controls  (check-box  :generateSecret false )]]
            
            [:div.control-group
              (mk-label :orgName "Organization Name:" ) 
              [:div.controls (text-field :orgName (:orgName client))]]
              
            [:div.control-group 
              (mk-label :description "Application Description:" ) 
               [:div.controls 
                ;[:textarea.span12 {:type "text-area" :value (:description-client client)}]
                (text-area :description (:description client))
                [:p.help-block "This descriptive text will be shown to users when they are asked to authorize your application"]]]
            
            [:div.control-group
              (mk-label  :redirectUri "Redirect URIs:" ) 
               [:div.controls (text-area {:size "50" } :redirectUri (:redirectUri client))
                [:p.help-block "Multiple RedirectURIs should be seperated by a space"]]]]])

;; Client registration 

(defpage "/client/register"  {:as req}
  (common/layout 
      [:p "Register New Client" ]
       [:form.form-horizontal {:method "POST", :action (resolve-uri "/client/register")}        
          (client-fields (kdb/new-client "Organization" "description" "http://example.com/redirect" (kdb/current-user-pk) ))
           [:div.form-actions
            [:button.btn.btn-primary "Add "]
            [:button.btn {:type "reset" } "Reset"]
            [:a.btn.btn-small.pull-right {:href "/client/admin" :data-dismiss "alert"} "Return" ]]]))


(defpage [:post "/client/register"]  {:as client}
  (println "register " client)
  ; the form might have some extra junk that is not required on a new client insert
  (kdb/insert-client! (dissoc client :id)) 
  (session/flash-put! :message "Added new Client")
  (resp/redirect "/client/admin"))

(defpartial edit-client-record [client]    
     [:form.form-horizontal {:method "POST", :action (resolve-uri "/client/edit")}
            (client-fields client)
           [:div.form-actions
            [:button.btn.btn-primary "Submit "]
            [:button.btn {:type "reset" } "Reset"]
            [:a.btn.btn-small.pull-right {:href "/client/admin" :data-dismiss "alert"} "Return" ]]])


(defn error-item [errors]
  [:div.alert.alert-error "Error " [:br] (first errors)])

; Edit a client record
(defpage [:get "/client/edit"] {:keys [id]}
  (common/layout 
    [:h3 "Edit Client"]
    [:br]
    (vali/on-error :redirectUri error-item)
    (if-let [clientRec (check-and-fetch-client id)]
        (edit-client-record clientRec)
        ; else
        [:p "Permission Denied. You don't have permission to edit this record"])))


(defn valid-client? [{:keys [redirectUri]}]
  (doseq [uri (str/split redirectUri #" +" )]
    (println "validate " uri)
    (vali/rule (util/get-url uri) 
               [:redirectUri (str "Redirect URL '" (h uri) "' is not valid")]))
  (not (vali/errors? :redirectUri)))
  
; POST back handler on edit submit
(defpage [:post "/client/edit"] {:keys [description orgName redirectUri id generateSecret ] :as client}
  (println "Req " client)
  (if-let [clientRec (check-and-fetch-client id)]
    (do
      (let [new-rec (merge clientRec 
                           {:description  description
                            :orgName orgName
                            :redirectUri redirectUri
                            :clientSecret (if generateSecret 
                                            (util/generate-client-id-or-secret)  
                                            (:clientSecret clientRec))})]                                                 
      
        (if (valid-client? new-rec)
          (do
            (println "New merged record is " new-rec)
            (kdb/update-client! id new-rec)
            (session/flash-put! :message "Client successfully updated")))
          ;else
          (render "/client/edit" {:id id})))))

