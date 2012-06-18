(ns mulk.benki.id
  (:refer-clojure)
  (:use [clojure         core repl pprint]
        [hiccup core     page-helpers]
        [mulk.benki      config util db]
        [clojure.core.match
         :only [match]]
        [noir            core]
        [clojure.java.jdbc :only [transaction do-commands]])
  (:require [noir.session      :as session]
            [noir.response     :as response]
            [noir.request      :as request]
            [clojure.java.jdbc :as sql]
            [com.twinql.clojure.http :as http])
  (:import [org.openid4java.server ServerManager]
           [org.openid4java.message ParameterList AuthRequest DirectError]))


(defonce manager
  (doto (ServerManager.)
    (.setOPEndpointUrl (str (:base-uri @benki-config) "/openid"))))

(def profile-base-uri (str (:base-uri @benki-config) "/id/"))

(defn user-owns-nickname? [user nickname]
  (with-dbt
    (sql/with-query-results results
      ["SELECT 't' FROM user_nicknames WHERE nickname = ? AND \"user\" = ?"
       nickname *user*]
      (doall (seq results)))))

(defn fail-authentication []
  {:status 403, :type "text/plain", :body "Not authorized."})

(defn nickname-from-profile-uri [uri]
  (let [base-uri (.substring uri 0 (.length profile-base-uri))
        nickname (.substring uri (.length profile-base-uri))]
    (if (= base-uri profile-base-uri)
      nickname
      nil)))

(defn format-openid-response [s]
  {:status 200, :type "text/plain", :body s})

(defn verify-openid [paramlist]
  (let [auth-request (AuthRequest/createAuthRequest paramlist (.getRealmVerifier manager))
        claimed-id   (or (.getClaimed auth-request)
                         (get (:query-params (request/ring-request)) "openid.identity"))
        nickname     (nickname-from-profile-uri claimed-id)
        okay?        (and *user* (user-owns-nickname? *user* nickname))
        response     (.authResponse manager paramlist nil claimed-id (boolean okay?) false)]
    (if (isa? (class response) DirectError)
      (fail-authentication)
      (do
        (.sign manager response)
        (redirect (.getDestinationUrl response true))))))

(defn stringify-keys [m]
  (into {} (map (fn [[k v]] [(name k) v]) m)))

(defn process-openid-request []
  (let [query     (:params (request/ring-request))
        paramlist (ParameterList. (stringify-keys query))
        mode      (query "openid.mode")]
    (match [mode]
      ["associate"]
        (format-openid-response
         (.keyValueFormEncoding (.associationRequest manager paramlist)))
      ["check_authentication"]
        (format-openid-response
         (.keyValueFormEncoding (.verify manager paramlist)))
      ["checkid_setup"]
        (with-auth
          (verify-openid paramlist))
      ["checkid_immediate"]
        (verify-openid paramlist)
      [x]
        {:status 200, :headers {"Content-Type" "text/plain; charset=utf-8"}, :body (str "Whaaaat?  What is “" x "” supposed to mean??   This is what you sent:" (request/ring-request)
                                                                                        )})))

(def profile-page {})

(defn show-profile-page []
  (layout profile-page "A Profile Page"
    [:p "This is a profile page."]))

(defn render-xrds [nickname]
  {:status 200
   :headers {"Content-Type" "application/xrds+xml; charset=UTF-8"}
   :body
   (clojure.string/replace
    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>
     <xrds:XRDS xmlns:xrds=\"xri://$xrds\" xmlns=\"xri://$xrd*($v*2.0)\">
       <XRD>
         <Service>
           <Type>http://openid.net/signon/1.0</Type>
           <URI>{base-uri}/openid/api</URI>
         </Service>
       </XRD>
     </xrds:XRDS>"
    "{base-uri}"
    (:base-uri @benki-config))})

(defpage [:get  "/openid/api"] {}
  (process-openid-request))

(defpage [:post "/openid/api"] {}
  (process-openid-request))

(defpage [:get  "/id/:nickname"] {nickname :nickname}
  (if (re-find #"application/xrds\+xml"
               (get-in (request/ring-request) [:headers "accept"]))
    (render-xrds nickname)
    (show-profile-page)))

(defpage [:get  "/~:nickname"] {nickname :nickname}
  (redirect (link :profile nickname)))
