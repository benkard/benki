(ns mulk.benki.auth
  (:refer-clojure)
  (:use [clojure         core repl pprint]
        [clojure.contrib repl-utils]
        [hiccup core     page-helpers]
        [mulk.benki      util db]
        [clojure.core.match.core
         :only [match]]
        [noir            core]
        [clojure.java.jdbc :only [transaction do-commands]])
  (:require [noir.session      :as session]
            [noir.response     :as response]
            [noir.request      :as request]
            [clojure.java.jdbc :as sql])
  (:import [org.openid4java.consumer ConsumerManager]
           [org.openid4java.message ParameterList]))


(defonce manager (ConsumerManager.))


(defpartial return-from-openid-provider []
  (let [parlist      (ParameterList. (:query-params (request/ring-request)))
        discovered   (session/get :discovered)
        ;; Does the following work for POST requests?
        request-uri  (str "http://localhost:3001/login/return"
                          ;;(resolve-uri "/login/return")
                          (let [query-string (:query-string (request/ring-request))]
                            (if query-string
                              (str "?" query-string)
                              "")))
        verification (.verify manager request-uri parlist discovered)
        id           (.getVerifiedId verification)]
    (if id
      (with-dbt
        (let [openid  (first (query "SELECT * FROM openids WHERE openid = ?"
                                    (.getIdentifier id)))
              user-id (if openid
                        (:user openid)
                        nil)
              user    (first (if user-id
                               (query "SELECT * FROM users WHERE id = ?" user-id)
                               nil))]
          (if user-id
            (do (session/put! :user user-id)
                (layout "Authenticated!" [:p "Welcome back, " (:first_name user) "!"]))
            (layout "Authentication Failed" [:p "Did not recognize OpenID."]))))
      (layout "Authentication Failed" [:p "OpenID authentication failed."]))))


(defpage [:post "/login/return"] []
  (return-from-openid-provider))

(defpage "/login/return" []
  (return-from-openid-provider))

(defpage "/login/authenticate" {openid :openid}
  (let [discoveries (.discover     manager openid)
        discovered  (.associate    manager discoveries)
        authreq     (.authenticate manager discovered ;;(resolve-uri "/login/return")
                                   "http://localhost:3001/login/return"
                                   )]
    (session/put! :discovered discovered)
    (response/redirect (.getDestinationUrl authreq true))))

(defpage "/login" []
  (layout "Benki Login"
    [:p "Please enter your OpenID:"]
    [:form {:action (resolve-uri "/login/authenticate"),
            :method "GET"}
     [:input {:type "text", :name "openid"}]
     [:input {:type "submit"}]]))
