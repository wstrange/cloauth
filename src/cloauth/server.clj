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

(def db-params {:classname "org.postgresql.Driver"
          :subprotocol "postgresql"
          :subname "//localhost:5432/cloauth"
          :user "cloauth"
          :password "password"})
  
(defdb db db-params)
  (comment {:classname  "org.postgresql.Driver"
           :user "cloauth"
           :password "password"
           :subprotocol "postgresql"
           :subname (get (System/getenv) "DATABASE_URL" "//localhost:5432/cloauth")})                

(server/load-views "src/cloauth/views/")


(defn -main [& m]
  (let [mode (keyword (or (first m) :dev))
        port (Integer. (get (System/getenv) "PORT" "8080"))]
    (server/start port {:mode mode
                        :ns 'cloauth})))



; For dev - start server on load
;(-main)