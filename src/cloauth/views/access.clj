(ns cloauth.views.access 
  (:require [noir.response :as resp]
             [noir.session :as session])
  (:use noir.core
        hiccup.core
        hiccup.page-helpers
        hiccup.form-helpers))


; Return information on an access token 
; AuthN: Only a service associated with the token scope can call this 
; The service uses the expires time to know when the token is no longer valid
(defpage "/token/info"  {:keys [token] :as request} 
  )


(defpage "/token/access" {:keys [clientId clientSecret] :as request} 
  (resp/json "not done" ))


(defpage "/authorize"  {:keys [clientId clientSecret] :as request} 
  (resp/json "not done"))

