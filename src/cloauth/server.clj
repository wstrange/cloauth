(ns cloauth.server
  (:require [noir.server :as server]
            [cloauth.models.db :as db])
  (:use korma.db))


(defdb db (mysql {:db "cloauth"
                  :host "localhost"
                  :port "3306"
                  :delimiters "`"
                  :user "cloauth"
                  :password "password"}))



(server/load-views "src/cloauth/views/")


(defn -main [& m]
  (let [mode (keyword (or (first m) :dev))
        port (Integer. (get (System/getenv) "PORT" "8080"))]
    (server/start port {:mode mode
                        :ns 'cloauth})))


; For dev - start server on load
(-main)