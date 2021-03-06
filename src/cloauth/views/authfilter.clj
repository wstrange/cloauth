(ns cloauth.views.authfilter
  "Authentication Filters"
  (:require 
            [cloauth.models.kdb :as db]
            [noir.session :as session]
            [noir.response :as resp])
  (:use noir.core))


; Any URI route that matches this java regex pattern is public
; Usefull regex tester: http://www.regexplanet.com/simple/index.html
; Regex docs: http://docs.oracle.com/javase/7/docs/api/java/util/regex/Pattern.html
(def public-uris #"/|^/(favicon|css|js|authn|img|test|login).*")


(defn public-uri? [uri]
  (re-matches public-uris uri))

(def admin-pages #"^/admin.*")

; OAuth clients use client id / secret authentication
(def client-pages #"^/oauthclient.*")
(defn client-uri? [uri] 
  (re-matches client-pages uri))

(defn admin-page? [uri] 
  (re-matches admin-pages uri))

(defn please-login [uri]
  "Handle attempt to access restricted page "
  ;(println "Restricted page " uri)
  (session/flash-put! :error "You must login first!") (resp/redirect "/login"))
  
; the user must be authenticated to request a protected uri 
(pre-route [:any "/*"] {:keys [uri] :as req} 
   ;(println "check uri " uri)
   (cond 
     ; pass-through all public uris
     (public-uri? uri)  nil  
     ; pass-through client uris /oauthclient
     ; they use client auth -
     (client-uri? uri) nil
     ; uri is not public. Is the user logged in?
     (not (db/logged-in?))  (please-login uri)
     ; Is the page an admin page?
     (admin-page? uri)  (print "admin page! " uri)
     ; else - page is OK
     :else  nil))
