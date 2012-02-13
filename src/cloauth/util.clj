(ns cloauth.util
  "Misc Utility Functions. Move these somewhere else???"
   (:import (java.net URL))
  )


(def ascii-codes (concat (range 48 58) (range 66 91) (range 97 123)))
(defn gen-id [length]
  "Generate a random string of given length. Used for client id, secrets, etc."
  (apply str (repeatedly length #(char (rand-nth ascii-codes)))))

(defn generate-client-id-or-secret [] (gen-id 24))
(defn generate-token [] (gen-id 24))

  
(defn get-url [url] 
  "Convert url string to a java.net.URL. Return nil if not valid"
  (try   
    (let [url (URL. url)] url )
    (catch Exception e nil)))