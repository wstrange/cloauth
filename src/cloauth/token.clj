(ns cloauth.token
  "Short term token handling - memory based
   todo: eventually the implemenation should use some kind
   of memcache like protocol to cache tokens.
   Right now this only works in a single instance"
  )
  

; auth code lifetime - from spec: 10 minutes and ONE TIME use only
; access tokens - up to app -but short lived (1 hour max?)
; 

(def ^:dynamic *auth-codes* (atom {}))


(def ascii-codes (concat (range 48 58) (range 66 91) (range 97 123)))
(defn gen-id [length]
  "Generate a random string of given length. Used for client id, secrets, etc."
  (apply str (repeatedly length #(char (rand-nth ascii-codes)))))

(defn generate-token [] (gen-id 32))

(defn- msec-to-sec [msec] 
  "Convert msec to seconds"
  (int (/ msec 1000)))
  
(defn- expire-msec [future-msec]
  "Given a time in the future (msec) calculate 
   the remain time in seconds"
  (msec-to-sec (- future-msec (System/currentTimeMillis) )))

(defn min-to-fmsec [min]
  "minutes to a future msec"
  (+ (System/currentTimeMillis) (* 1000 60 min)))

(defonce authcode-lifetime 10) ; authcode lifetime in minutes

(defn purge-code [code]
  ;(println "Purging code " code)
  (swap! *auth-codes* dissoc code))


(defn print-tokens [] 
  (doseq [code @*auth-codes*]
    (println "code  =" code)))
  
(defn create-auth-code [oauth-request] 
  "Create a new short lived auth code request
   Returns a map which is used as the json response - 
   contains the generated auth code "
  (println "Create Auth Code request = " oauth-request)
  (let  [code (generate-token)
         t  {:request oauth-request
             :authcode code ; todo: we can probably get rid of this
             :expires   (min-to-fmsec authcode-lifetime)}]
       (swap! *auth-codes* assoc code t)
       (print-tokens)
       {:code code} ))

(defn get-authcode-entry [code] 
  "lookup authcode - return the oauth-request map or nil if authcode does not
  exist. "
  (get @*auth-codes* code))
  

(defonce default-access-token-expiry-minutes 60)

; token map - keyed by the access token
; values are a map that describe what the token can access 
(def ^:dynamic *access-tokens* (atom {}))

(defn new-access-token [clientId userId scopes]
  "Create a new access token. Assumes request has been validated
  saves the access token to memory an returns a map
  that is used as JSON response"
  (let [atoken (generate-token) 
        refreshToken (generate-token)
        tval {:access_token atoken
              :expires (min-to-fmsec default-access-token-expiry-minutes)
              :clientId clientId 
              :userId userId
              :scopes scopes}]
      ; add the access token to the map
      (swap! *access-tokens* assoc atoken tval)
      ; return a json map 
      {:access_token atoken
       :refresh_token refreshToken
       :token_type "Bearer"
       :expires_in (* 60 default-access-token-expiry-minutes) }))


(defn get-token-entry [token] 
  "todo" )


(comment
  "todo - "
  
(def purge-task (future 
                  (loop [] (do 
                          (purge-expired-tokens)
                          (println "Expire token thread sleeping...." (.getName (Thread/currentThread)))
                          (Thread/sleep 90000))
                    (recur ))))

)


