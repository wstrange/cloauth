(ns cloauth.util
  "Misc Utility Functions. Move these somewhere else???"
  )


(def ascii-codes (concat (range 48 58) (range 66 91) (range 97 123)))
(defn gen-id [length]
  "Generate a random string of given length. Used for client id, secrets, etc."
  (apply str (repeatedly length #(char (rand-nth ascii-codes)))))

(defn generate-token [] (gen-id 32))
