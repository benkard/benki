(ns mulk.benki.auth
  (:refer-clojure)
  (:use [clojure         core repl pprint]
        [hiccup core     page-helpers]
        [mulk.benki      util db]
        [clojure.core.match
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


(defn redirect [x]
  {:status 302, :headers {"Location" x}, :body ""})


(defn return-from-openid-provider []
  (let [parlist      (ParameterList. (:query-params (request/ring-request)))
        discovered   (session/get :discovered)
        ;; Does the following work for POST requests?
        request-uri  (str (resolve-uri "/login/return")
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
                (if-let [return-uri (session/flash-get)]
                  (redirect return-uri)
                  (layout {} "Authenticated!" [:p "Welcome back, " (:first_name user) "!"])))
            (layout "Authentication Failed"
                    [:p "Did not recognize OpenID."]
                    [:p "Your OpenID is: " [:strong (.getIdentifier id)]]))))
      (layout "Authentication Failed" [:p "OpenID authentication failed."]))))


(defpage [:post "/login/return"] []
  (return-from-openid-provider))

(defpage "/login/return" []
  (return-from-openid-provider))

(defpage "/login/authenticate" {openid :openid_identifier}
  (let [discoveries (.discover     manager openid)
        discovered  (.associate    manager discoveries)
        authreq     (.authenticate manager discovered (resolve-uri "/login/return"))]
    (session/put! :discovered discovered)
    (redirect (.getDestinationUrl authreq true))))

(def login-page-layout
  {:head (list
          [:link {:type "text/css", :rel "stylesheet", :href "/3rdparty/openid-selector/css/openid.css"}]
          [:script {:type "text/javascript", :src "/3rdparty/openid-selector/js/openid-jquery.js"}]
          [:script {:type "text/javascript", :src "/3rdparty/openid-selector/js/openid-en.js"}]
          [:script {:type "text/javascript", :src "/js/openid-login.js"}]
          )})

(defpage "/login" []
  (layout login-page-layout "Benki Login"
    [:form {:action (resolve-uri "/login/authenticate"),
            :method "GET"}
     [:div {:id "openid_choice"}
      [:p "Please select your OpenID provider:"]
      [:div {:id "openid_btns"}]]
     [:div {:id "openid_input_area"}
      [:input {:type "text", :name "openid_identifier", :id "openid_identifier"}]
      [:input {:type "submit"}]]]))
