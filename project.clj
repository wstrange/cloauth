(defproject cloauth "0.1.0-SNAPSHOT"
            :description "OAuth AuthZ provider in Clojure"
            :dependencies [[org.clojure/clojure "1.3.0"]
                           [noir "1.2.2"]
                           [clj-http "0.2.5"]
                           ;[clj-json "0.4.3"]
                           [org.clojure/data.json "0.1.1"]
                           [lobos "1.0.0-SNAPSHOT"]
                           ; for rdb 
                           [korma "0.3.0-alpha12"]
                           ; postgres
                           [postgresql/postgresql "9.1-901.jdbc4"]
                           ; mysql 
                           ;[mysql/mysql-connector-java "5.1.18"]
                            ]
            :dev-dependencies [[lein-eclipse "1.0.0"]])
            ;:main cloauth.server)
