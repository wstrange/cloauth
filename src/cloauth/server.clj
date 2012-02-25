(ns cloauth.server
  (:require [noir.server :as server]
            [cloauth.models.kdb :as db]
             [clojure.string :as str] )
  (:use korma.db )
  (:import (java.net URI)))

 
(defn heroku-db
  "Generate the db param map for the Heroku environment"
  []
  (when (System/getenv "DATABASE_URL")
    (let [url (URI. (System/getenv "DATABASE_URL"))
          host (.getHost url)
          port (if (pos? (.getPort url)) (.getPort url) 5432)
          path (.getPath url)]
      (merge
       {:subname (str "//" host ":" port path)}
       (when-let [user-info (.getUserInfo url)]
         {:user (first (str/split user-info #":"))
          :password (second (str/split user-info #":"))})))))

; Define the DB connect parameters. 
; If we are running on Heroku those db params will override the local settings
(def db-params
  (merge {:classname "org.postgresql.Driver"
          :subprotocol "postgresql"
          :subname "//localhost:5432/cloauth"
          :user "cloauth"
          :password "password"}
         (heroku-db)))

 ; Create the db connection pool

(defdb db db-params)


; run test db select to verify its working.... 
;(println "test user select" (db/all-users)) 

; heroku sets the port in an env var. We default to 8080 for localhost
(def port (Integer. (get (System/getenv) "PORT" "8080")))
                 

(server/load-views "src/cloauth/views/")


(defn -main [& m]
  (let [mode (keyword (or (first m) :dev))]
    (println "Starting on " port  " arguments " m)
    (server/start port {:mode mode :ns 'cloauth})))

;(-main)
