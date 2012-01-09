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

(defn delete-username! [userName] (delete users (where {:userName userName})))


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

(defn get-client-by-id [clientId] 
  (first (select clients (where {:id clientId}))))

 
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

(defn get-scope-id [uri] 
  "Given a uri return the scope id"
  (first (select scope (where {:uri [= uri]}))))

(defn- get-scope-ids [scopes]
  "return a sorted list of scope ids that represent the list of given scopes uris. 
  This is used for comparison
  To do: this data could be cached for performance. No need to look this up on every search"
    (sort (map #(:id (get-scope-id % )) scopes)))


; grant_scope - joins set of scopes to a grant
(defentity grant_scope 
  (belongs-to scope)
  (belongs-to grant))

; grants 
(defentity grant 
  (belongs-to users)
  (belongs-to clients)
  (has-many grant_scope))


(defn- add-scope-to-grant [grantId scope]
  "Add a scope e.g. /api/calendar to 
  a grant. A user will grant a set of scopes"
  (println "Add scope " scope " to grantid " grantId)
  (let [s (get-scope scope)] 
    (insert grant_scope
         (values
            {:grant_id grantId 
             :scope_id (:id s)}))))

(defn create-grant [clientId userId scope-list refreshToken]
  "Create a new grant. scopes is a list of scopes granted. Existing grants are deleted"
  (println "Create grant clientid=" clientId " userid=" userId " scopes " scope-list " token " refreshToken)
  ; first we should delete any existing grant
  ; the db will cascade this
  (delete grant (where {:clients_id clientId :users_id userId}))
   
  (let [g (insert grant (values {:clients_id clientId :users_id userId :refreshToken refreshToken}))
        gid (:GENERATED_KEY  g)] 
    (doseq [s scope-list] 
      (add-scope-to-grant gid s))))
 
(defn get-grants [userId]
  "Get all the grants for the user - joined with the client descriptions"
 (select grant (with clients) 
     (fields :id :clients.orgName :refreshToken :clients.description)
     (where {:users_id userId})))


(defn get-grant-and-scopes [userId clientId]
  "get the list of scopes for the user for a given client
   returns nil (if there is no grant) or a sequence of map entries where :scope_id carries the scope"
  (select grant_scope  (with grant) (where (and {:grant_id :grant.id }
                                               {:grant.users_id userId :grant.clients_id clientId}))))


(defn grant-scopes-are-the-same [userId clientId scopes]
  "Given a set of scopes, return true if there is an existing grant for the user/client that grants the same set of scopes"
  
  (let [sorted-scopes (get-scope-ids scopes)
        grants (get-grant-and-scopes userId clientId)
        gids (map #(:scope_id %) grants)
        sorted-gids (sort gids)]
    (prn "Check grant scopes" userId clientId sorted-scopes sorted-gids)
    (= sorted-gids sorted-scopes)))
       

(defn get-grant-for-clientId [userId clientId]
  "Get the specific grant a user made for a client or nil if no such grant exists. There will never be more than one grant"
  "return the grant id and a set of scopes granted"
  (select scope 
      (fields :uri)
      (where {:users_id userId :clients_id clientId})))
   

(defn delete-grant! [id]  
  (println "delete grant " id)
  (delete grant (where {:id id})))
