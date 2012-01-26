(ns cloauth.server
  (:require [noir.server :as server])
  (:use korma.db ))

(comment
(defdb db (mysql {:db "cloauth"
                  :host "localhost"
                  :port "3306"
                  :delimiters "`"
                  :user "cloauth"
                  :password "password"})))



(def db-params 
  (merge {:classname "org.postgresql.Driver"
          :subprotocol "postgresql"}
         (if-let [url (System/getenv "DATABASE_URL")]
           {:subname url}
           ; else
           {:user "cloauth"
            :subname "//localhost:5432/cloauth"
            :password "password"})))
 
 
(defdb db db-params)

(println "Db defined " db)
                 

(server/load-views "src/cloauth/views/")


(defn -main [& m]
  (let [mode (keyword (or (first m) :dev))
        port (Integer. (get (System/getenv) "PORT" "8080"))]
    (server/start port {:mode mode
                        :ns 'cloauth})))



; For dev - start server on load
;(-main)