(ns cloauth.models.testdb
  "Create sample data for testing"
   (:require   
            [cloauth.models.kdb :as db]
            [cloauth.util :as util]
            [noir.validation :as vali])
   (:use korma.db korma.core))

(def testUser "test@test.com")

(defn orgname [user]  (str "Company-" user))

(defn- create-data [uname]
  (insert db/scopes (values {:uri "test" :description "Test Scope"}))
  (let [userId   (db/insert-user! {:userName uname :verifiedEmail uname :roles [:admin]})
        client   (db/new-client (orgname uname)
                        "Purveyor of Fine Widgets"
                        "/test/redirect" 
                        userId)
        clientId (db/insert-client! client)
        grant (db/create-grant (:clientId client) userId ["test"] "dummyrefreshtoken")]
    (println "Client id=" clientId)
    {:clientId clientId :userId userId}))


(defn nuke-it [] 
  (delete db/clients)
  (delete db/users)
  (delete db/scopes ))

(defn testUserId [] (:id (db/get-user testUser)))
(defn testClientId [] (:clientId (first (db/clients-owned-by-user-id (testUserId)))))
    


(defn create-sample-data []
  (try (do
         (nuke-it)
         (create-data testUser))
    (catch Exception e (prn e))))





