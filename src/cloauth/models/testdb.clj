(ns cloauth.models.testdb
  "Create sample data for testing"
   (:require   
            [cloauth.models.kdb :as db]
            [cloauth.util :as util]
            [noir.validation :as vali])
   (:use korma.db korma.core))

(def testUser "test@test.com")

(defn orgname [user]  (str "Company-" user))

(defn create-sample-data [uname]
  (insert db/scopes (values {:uri "test" :description "Test Scope"}))
  (let [userId   (db/insert-user! {:userName uname :verifiedEmail uname})
        clientId (db/insert-client! 
                   (db/new-client (orgname uname)
                        "Purveyor of Fine Widgets"
                        "/test/redirect" 
                        userId))
        xx  (println "Client id" clientId)
        grant (db/create-grant clientId userId ["test"] "dummyrefreshtoken")]
    (println "Client id=" clientId)
    {:clientId clientId :userId userId}))


(defn nuke-it [] 
  (delete db/scopes )
  (delete db/clients)
  (delete db/users))

(defn testUserId [] (:id (db/get-user testUser)))
(defn testClientId [] (:clientId (first (db/clients-owned-by-user-id (testUserId)))))
    


(def ids 
  
  (try (do
         (nuke-it)
         (create-sample-data testUser))
    (catch Exception e (prn e))))


(comment
 (def ids 
  (try (create-sample-data testUser)
    (catch Exception e (prn e))))

(nuke-it)
(create-sample-data testUser)

  )





