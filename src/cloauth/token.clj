(ns cloauth.token
  "Short term token handling - memory based

   todo: eventually the implemenation should use some kind
   of memcache like protocol to cache tokens across server instances.
   Right now this only works in a single instance of the application"
  )
  

; auth code lifetime - from Oauth spec: 10 minutes and ONE TIME use only
(def authcode-lifetime 10) ; authcode lifetime in minutes


; access tokens - up to application -but generally short lived (1 hour max?)
; for testing we can make it really short
(def default-access-token-expiry-minutes 1)


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

(defn min-to-future-unix-time [min]
  "calculate the future unix time (msec) that is ahead by min minutes  "
  (+ (System/currentTimeMillis) (* 1000 60 min)))



(defn purge-code [code]
  "Called by token generating code to invalidate an auth code. These are one time use only"
  ;(println "Purging code " code)
  (swap! *auth-codes* dissoc code))

(defn create-auth-code [oauth-request] 
  "Create a new short lived auth code request
   Returns a map which is used as the json response - 
   contains the generated auth code "
  (println "Create Auth Code request = " oauth-request)
  (let  [code (generate-token)
         t  {:request oauth-request ; save the original request
             :expires   ( min-to-future-unix-time authcode-lifetime)}]
       (swap! *auth-codes* assoc code t)
       ;(print-tokens)
       {:code code} ))

(defn get-authcode-entry [code] 
  "lookup authcode - return the oauth-request map or nil if authcode does not
  exist. "
  (get @*auth-codes* code))
  


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
              :expires (min-to-future-unix-time default-access-token-expiry-minutes)
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
  (get @*access-tokens* token))

(defn time-expired [t]
  "True if time t is in the past"
  (> (System/currentTimeMillis) t))

(defn- purge-expired [atom] 
  "Purge any map entries whos :expires value is in the past"
  (doseq [[t tval] @atom] 
    (if (time-expired (:expires tval))
      (do 
        (println "token expired " tval)
        (swap! atom dissoc t)))))
  
(defn purge-expired-tokens [] 
  "Remove all access tokens and auth codes that have expired"
  (purge-expired *access-tokens*)
  (purge-expired *auth-codes* ))

(defonce purge-task 
  (future 
    (while true (do 
        (purge-expired-tokens)
        ;(println "Expire token thread sleeping...." (.getName (Thread/currentThread)))
        (Thread/sleep 30000)))))