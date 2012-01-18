(ns lobos.migrations 
   (:refer-clojure :exclude [alter drop
                            bigint boolean char double float time])
  (:use (lobos [migration :only [defmigration]] core schema
               config helpers)))


(comment
(def db
  {:classname   "org.postgresql.Driver"
   :subprotocol "postgresql"
    :subname "//localhost:5432/cloauth"
    :user "cloauth"
    :password "password"})
(open-global db))

(defmigration add-users-table
  (up [] (create
          (tbl :users
            (varchar :userName 48 :unique)
            (varchar :verifiedEmail 64 :unique)
            (varchar :firstName 32 )
            (varchar :lastName 32)
            (varchar :displayName 64)
            (varchar :language 10)
            (varchar :roles 32))))          
  (down [] (drop (table :users))))

(defmigration add-clients-table
  (up [] (create
          (tbl :clients
            (varchar :clientId 32 :unique)
            (varchar :clientSecret 32)
            (varchar :orgName 32)
            (varchar :description 64)
            (varchar :redirectUri 256)
            (refer-to :users))))
  (down [] (drop (table :clients))))

(defmigration add-grant-table 
  (up [] (create 
           (tbl :grants 
                (bigint :expiry)
                (varchar :refreshToken 32)
                (refer-to :users)
                (refer-to :clients))))
  (down [] (drop (table :grants))))

(defmigration add-scope-table 
  (up [] (create 
           (tbl :scopes
                (varchar :uri 128 :unique)
                (varchar :description 128))))
  (down [] (drop (table :scopes))))

(defmigration add-grant-scope-table 
  (up [] (create 
           (table :grant_scope 
                  (refer-to :scopes)
                  (refer-to :grants))))
  (down [] (drop (table :grant_scope))))

