(defproject cloauth "0.1.0-SNAPSHOT"
            :description "OAuth AuthZ provider in Clojure"
            :dependencies [[org.clojure/clojure "1.3.0"]
                           [noir "1.2.2"]
                           [clj-http "0.2.5"]
                           ; for rdb 
                           [korma "0.3.0-alpha12"]
                           [mysql/mysql-connector-java "5.1.18"]
                            ]
            :dev-dependencies [[lein-eclipse "1.0.0"]]
            :main cloauth.server)
