(defproject cloauth "0.1.0-SNAPSHOT"
            :description "OAuth AuthZ provider in Clojure"
            :dependencies [[org.clojure/clojure "1.3.0"]
                           [noir "1.2.1"]
                           [congomongo "0.1.7"]
                           [clj-http "0.2.5"]
                            ]
            :dev-dependencies [[lein-eclipse "1.0.0"]]
            :main cloauth.server)
