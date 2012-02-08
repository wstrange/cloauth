(ns cloauth.views.client 
  "Client Views"
  (:require [noir.response :as resp]
             [noir.session :as session]
             [cloauth.views.common :as common]
             [cloauth.models.kdb :as kdb]
             [cloauth.util :as util])
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
  (session/flash-put! :message "Added new Client")
  (resp/redirect "/client/admin"))
  

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


(defpage "/client/delete" {:keys [id]}
  (kdb/delete-client! id)
  (resp/redirect "/client/admin"))


(defn- mk-label [id text]
  (label {:class "control-label"} id text))

(defpartial edit-client-record [client] 
  [:div.span7
   [:table.table.table-striped.table-condensed
    [:tr [:td "Client Id"] [:td  (:clientId client)]]
     [:tr [:td "Client Secret"] [:td  (:clientSecret client)]]]
  
     [:form.form-horizontal {:method "POST", :action (resolve-uri "/client/edit")}
           [:fieldset  
            (hidden-field :clientId  (:id client))
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
              (mk-label  :redirectUris "Redirect URIs:" ) 
               [:div.controls (text-area {:size "50" } :redirectUris (:redirectUri client))
                [:p.help-block "Multiple RedirectURIs should be seperated by a space"]]]
              
          
           [:div.form-actions
            [:button.btn.btn-primary {:data-content "And here's some amazing content. It's very engaging. right?"}
                                      "Submit "]
            [:button.btn {:type "reset" } "Reset"]
            [:a.btn.btn-small.pull-right {:href "/client/admin" :data-dismiss "alert"} "Cancel" ]]]]])


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


; Edit a client record
(defpage [:get "/client/edit"] {:keys [id]}
  (common/layout 
    [:h3 "Edit Client"]
    [:br]
    (if-let [clientRec (check-and-fetch-client id)]
        (edit-client-record clientRec)
        ; else
        [:p "Permission Denied. You don't have permission to edit this record"])))




; POST back handler on edit submit
(defpage [:post "/client/edit"] {:keys [clientId generateSecret orgName description redirectUris] :as req}
  (println "Req " req)
  (if-let [clientRec (check-and-fetch-client clientId)]
    (do
      (let [new-rec (merge clientRec 
                           {:description description 
                            :orgName orgName
                            :redirectUri redirectUris
                            :clientSecret (if generateSecret 
                                            (util/gen-id 24)  
                                            (:clientSecret clientRec))})]                     
                            
        (println "New merged record is " new-rec)
        (kdb/update-client! clientId new-rec))
        (resp/redirect "/client/admin"))
    ; else 
    ; todo - add flash message
    (resp/redirect (str "/client/edit?id=" clientId))))
     
  
