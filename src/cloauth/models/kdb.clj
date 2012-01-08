(ns cloauth.models.kdb
  "DB functions using Korma"
   (:require      
            [cloauth.util :as util]
            [noir.session :as session]
            [cloauth.util :as util]
            [noir.validation :as vali])
   (:use korma.db korma.core))


; Some user session management
(defn has-role? [user role]
  "Non nil if the user has role (identified by keyword)
  example (has-role user :admin)"  
  (some (name role) (:roles user)))

(defn current-user-record []
  (session/get :user))

(defn user-is-admin? []
  "True if the current user has the admin role"
  (has-role? (current-user-record) :admin))

(defn current-userName []
  "Return the current user name (probably email) for the logged in user"
  (:userName (current-user-record)))

(defn current-userId []
  (:id (current-user-record)))

(defn login! [user]
  " Log the user in"
  (session/put! :user user))
   

(defn logged-in? []
  "Not nil if the user is logged in"
  (current-userName)) 

(defentity users 
  (entity-fields :verifiedEmail :userName  :firstName :lastName :roles ))

(defn get-user [username]
  "return the user with the username"
  (first
    (select users (where {:userName username}))))

(defn is-registered? [userName]
  "return the true if the user if registered, or nil"
  (if (get-user userName)
    true
    false))

(defn- pick-values [m klist] 
  "utility to select certain keys/vals from an object-map 
   m - map to pick from
   klist - keylist of keys to pick"
   (apply merge 
    (map (fn [x] {x (x m)}) klist)))


(defn insert-user! [u] 
  "Add the user object. Return the id"
  (println "Inserting user " u)
  (:GENERATED_KEY
      (insert users (values (pick-values u [:verifiedEmail :userName 
                              :displayName :firstName
                              :lastName :language])))))


(defn update-user! [query fields]
  "Update the user specifie with query with newobj"
  (update users (where query) (set-fields fields)))

(defn delete-user! [id] (delete users (where {:id id})))

(defn delete-username [userName] (delete users (where {:userName userName})))


(defn all-users [] 
  "Return all users... dodgy..."
  (select users))

;; Client - An OAuth 2 client 

(defentity clients 
  (entity-fields :id :clientSecret :orgName :description :redirectUri 
                 :users_id )
  
  (belongs-to users)
  
  (prepare (fn [x] 
      (merge x 
        (if-not (:id x)  {:id (util/gen-id 32)})
        (if-not (:clientSecret x) {:clientSecret (util/gen-id 32)})))))

                          
;todo: prepare could do this?
(defn new-client [orgName description redirectUri userId]
  "Generate a new client record. Generates unique id and client secret
  The client is ownned by the current user "
  {:orgName orgName 
   :users_id userId
   :description description 
   :redirectUri  redirectUri 
   :id (util/gen-id 32) 
   :clientSecret (util/gen-id 32)})

(defn insert-client! [client]
  "Insert a client record into the DB. Return the primary key"
   (insert clients (values client))
   (:id client))


(defn clients-owned-by-user-id [id]
  (select clients (where {:users_id id})))

(defn get-client-by-clientId [clientId] 
  (select clients (where {:id clientId})))

(defn get-client-by-id [id]
  (select clients (where {:id id})))
 
(defn delete-client! [id]
  (delete clients (where {:id [= id]})))



;(defn all-clients [] (select clients))

(defn all-clients []
  "return all clients - we also join with the users. table to 
  get the userName of the owning user. Makes for a nicer display"
  (select clients (with users )
          (fields :id  :clientSecret 
                  :orgName :description :redirectUri :users.userName)))

; scope 

(defentity scope )

(defn get-scope [uri] 
  (first (select scope (where {:uri [= uri]}))))

; grants 
(defentity grant 
  (belongs-to users)
  (belongs-to clients))

; grant_scope - joins set of scopes to a grant
(defentity grant_scope 
  (belongs-to scope)
  (belongs-to grant))

(defn- add-scope-to-grant [grantId scope]
  "Add a scope e.g. /api/calendar to 
  a grant. A user will grant a set of scopes"
  (println "Add scope " scope " to grantid " grantId)
  (let [s (get-scope scope)] 
    (insert grant_scope
         (values
            {:grant_id grantId 
             :scope_id (:id s)}))))

(defn new-grant [clientId userId scope-list]
  "Create a new grant. scopes is a list of scopes granted"
  (println "Create grant clientid=" clientId " userid=" userId " scopes " scope-list)
  (let [g (insert grant (values {:clients_id clientId :users_id userId}))
        gid (:GENERATED_KEY  g)] 
    (doseq [s scope-list] 
      (add-scope-to-grant gid s))))
 
(defn get-grants [userId]
  "todo:  get granted clients/ scopes"
  nil)

