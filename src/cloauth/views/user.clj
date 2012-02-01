(ns cloauth.views.user
  "User Managment views "
   (:require [cloauth.views.common :as common]
            [cloauth.models.kdb :as db]
            [cloauth.util :as util]
            [noir.session :as session]
            [noir.validation :as vali]
            [noir.response :as resp]
            [noir.request :as request]
            [clojure.string :as string]  
            [clj-http.client :as client]
            )
  (:use noir.core
        hiccup.core
        hiccup.page-helpers
        hiccup.form-helpers))

;; Partials 

(defpartial error-text [errors]
            [:p (string/join "<br/>" errors)])


(defpartial user-item [{:keys [userName]}]
            [:li
             (link-to (str "/admin/user/edit?u=" userName) userName)])


(defpartial user-fields [{:keys [userName displayName] :as usr}]
            (vali/on-error :userName error-text)       
            (text-field {:placeholder "Username" :size 40} :userName userName)
            [:br]
            (text-field {:placeholder "Display Name" :size 32} :displayName displayName))


;; Admin Pages


(defpage "/admin/user" []
   (common/layout
     [:h1 "Admin Page"]
     [:p "Curent Users"]
     [:ul.items 
      (map user-item (db/all-users)) ] ))


(defpage "/admin/user/edit" {:keys [u]}
         (let [user (db/get-user u)]
           (common/layout
             [:h2 "Edit User"]
             (form-to [:post "/admin/user/edit"]
                      (user-fields user)
                      [:ul.actions
                        [:li (link-to {:class "submit"} "/" "Submit")]
                        [:li (link-to {:class "delete"} (str "/admin/user/remove?user=" u ) "Remove")]]
                      [:button "Submit"]
                      ))))


;; Post action after user is updated
(defpage [:post "/admin/user/edit"] {:as user}
  (println "user is " user)
  (let [username (:userName user)
        u (db/get-user username)
        updated  (merge u user)]
    (println "Updating merge =" updated)
         (if (db/update-user! {:userName username} updated)
           (resp/redirect "/admin")
           (render "/admin/user/edit?u=" (:username user)))))

(defpage "/admin/user/remove" {:keys [user]}
  (println "removing" user)
  (db/delete-username! {:userName user})
  (resp/redirect "/admin/user"))

;; User profile pages
(defpage "/user/profile" []
  (let [u (db/current-user-record) ] 
    (common/layout 
      [:p "Update your profile " ]
      [:div
        (common/simple-post-form "/user/profile" 
                      {:displayName (:displayName u) 
                       :firstName (:firstName u) 
                       :lastName (:lastName u)
                       ;:verifiedEmail (:verifiedEmail u)
                       })]
      [:p (link-to "/" "Home")])))

(defpage [:post "/user/profile"] {:keys [firstName lastName displayName] }
  (let [updates {:firstName firstName
                 :lastName lastName
                  :displayName displayName}
        newu (merge (db/current-user-record) updates)]
    (println "new record " newu)
    (db/update-user! {:id (db/current-userId)} updates)
    (db/login! newu))
  (resp/redirect "/user/profile"))
    
