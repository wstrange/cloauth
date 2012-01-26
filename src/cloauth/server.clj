(ns cloauth.server
  (:require [noir.server :as server]
            [cloauth.models.kdb :as db]
             [clojure.string :as str] )
  (:use korma.db )
  (:import (java.net URI)))

(comment
(defdb db (mysql {:db "cloauth"
                  :host "localhost"
                  :port "3306"
                  :delimiters "`"
                  :user "cloauth"
                  :password "password"}))



(def db-params 
  (merge {:classname "org.postgresql.Driver"
          :subprotocol "postgresql"}
         (if-let [url (System/getenv "DATABASE_URL")]
           {:subname url}
           ; else
           {:user "cloauth"
            :subname "//localhost:5432/cloauth"
            :password "password"}))))


 
(defn heroku-db
  "Generate the db map according to Heroku environment when available."
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
(def db-params
  (merge {:classname "org.postgresql.Driver"
          :subprotocol "postgresql"
          :subname "//localhost:5432/cloauth"
          :user "cloauth"
          :password "password"}
         (heroku-db)))


 
(defdb db db-params)

(println "Db defined " db)
(println "DB params " db-params)
; run test db select 
(println "test user select" (db/all-users)) 


(def port (Integer. (get (System/getenv) "PORT" "8080")))
                 
(println "Port " port)

(server/load-views "src/cloauth/views/")

(println "Views loaded")

(defn -main [& m]
  (let [mode (keyword (or (first m) :dev))]
    (println "Starting on " port  " arguments " m)
    (server/start port {:mode mode
                        :ns 'cloauth})))

; For dev - start server on load
;(-main)


