(ns cloauth.models.kdb
  
  "DB functions using Korma"
   (:require      
            [cloauth.util :as util]
            [noir.validation :as vali])
   (:use korma.db korma.core))


(defdb db (mysql {:db "cloauth"
                  :host "localhost"
                  :port "3306"
                  :delimiters "`"
                  :user "cloauth"
                  :password "password"}))

(defentity users 
  (entity-fields :verifiedEmail ))


(insert users (values {:verifiedEmail "warren2@test.com"}))

(sql-only 
  (select users))
(select users)
