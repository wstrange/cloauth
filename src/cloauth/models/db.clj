(ns cloauth.models.db
  (:require [somnium.congomongo :as mongo]
            [somnium.congomongo.config :as mongoconfig]
            [somnium.congomongo.error :as mongoerror]
            [cloauth.util :as util]
            [noir.util.crypt :as crypt]
            [noir.validation :as vali]
            [noir.session :as session]))


(def conn (mongo/make-connection "cloauth":host "127.0.0.1"  :port 27017))


(defn init-db []
  "Initialize the DB. Ensure indexes are created, etc. "
  (mongo/set-connection! conn)
  (mongo/set-write-concern conn :strict)
  ;(mongo/set-write-concern mongoconfig/*mongo-config* :strict)
  (mongo/add-index! :users [:userName :verifiedEmail] :unique true))

; Some user functions 
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



(defrecord Client [ownerId companyName description redirectUrl clientId clientSecret ])


(defn new-client [companyName description redirectUrl]
  "Generate a new client record. Generates unique id and client secret
  The client is ownned by the current user "
  (Client. (current-userName) companyName description redirectUrl (util/gen-id 32) (util/gen-id 32)))

(defn insert-client! [client]
  "Insert a client record into the DB"
  (mongo/insert! :clients client))

; Users 

(defrecord User [userName firstName lastName verifiedEmail roles])

(defn new-user [userName firstName lastName verifiedEmail roles]
  (User. userName firstName lastName verifiedEmail roles))

(defn query-user [query-map]
  "Query for a user with the given attribute specified by query-map.
   map is actually a mongo query 
  But for our purposes this is a keyword search ANDed together
  (fetch {:userName foo :lastName Bar}) "
  (mongo/fetch :users :where query-map))

(defn get-user [username]
  (mongo/fetch-one :users :where {:userName username}))

(defn is-registered? [userName]
  "return the user object if registered, or nil"
  (get-user userName))

; return true / false
; Due to json encoding we must generate false (not a nil)
(defn registered-user? [uname] 
  "Return true/false if the user is registered
   Side effect - put the user object in the session
   Should registered include a signup completion check?"
  (let [u (get-user uname)]
      (if  (:registrationComplete u)
        (session/put! :user u)
        false)))

    


(defn logged-in? []
  "Not nil if the user is logged in"
  (current-userName)) 

(defn all-users [] 
  "Return all users... dodgy..."
  (mongo/fetch :users))


;; Mutations and Checks

;; Operations

;; What to do on error?
(defn add-user! [user]
  (let [u (mongo/insert! :users user)
        error (mongoerror/get-last-error)]
    (if error 
      (throw (Exception. (str "Duplicate record! " error)))
      u)))
     

(defn login! [user]
  " Log the user in"
  (session/put! :user user))
   

(defn update-user! [query newobj]
  "Update the user specifie with query with newobj"
  (mongo/update! :users query newobj))

(defn remove-user! [query]
  "Remove the user. Query is map matching the user
  Example:  {:userName fred}"
  (mongo/destroy! :users query))



