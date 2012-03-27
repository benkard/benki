(ns mulk.benki.auth
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
  (:import [org.openid4java.consumer ConsumerManager]
           [org.openid4java.message ParameterList]))


(defonce manager (ConsumerManager.))


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


(defpage [:post "/login/browserid/verify"] {assertion :assertion}
  ;; NB.  Can implement this ourselves if we want.
  (let [reply  (http/post "https://browserid.org/verify"
                          :query {:assertion assertion
                                  :audience (:base-uri @benki-config)}
                :as :json)
        result (:content reply)
        status (:status result)
        email  (:email  result)]
    (if (= (:status result) "okay")
      (with-dbt
        (let [record  (first (query "SELECT * FROM user_email_addresses WHERE email = ?" email))
              user-id (and record (:user record))]
          (if user-id
            (let [return-uri (session/flash-get)]
              (session/put! :user user-id)
              (response/json {:email email, :returnURI return-uri}))
            {:status 418,
             :headers {"Content-Type" "text/plain"},
             :body "I couldn't find you in the database."})))
      {:status 418,
       :headers {"Content-Type" "text/plain"},
       :body "Your BrowserID request was crooked."})))


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
  {:head
   (list
    [:link {:type "text/css", :rel "stylesheet", :href (resolve-uri "/3rdparty/openid-selector/css/openid.css")}])
   :bottom
   (list
    [:script {:type "text/javascript", :src (resolve-uri "/3rdparty/openid-selector/js/openid-jquery.js")}]
    [:script {:type "text/javascript", :src (resolve-uri "/3rdparty/openid-selector/js/openid-en.js")}]
    [:script {:type "text/javascript", :src (resolve-uri "/js/openid-login.js")}]
    )})

(defpage "/login" []
  (session/flash-put! (or (session/flash-get)
                          (get-in (request/ring-request) [:headers "referer"])))
  (layout login-page-layout "Benki Login"
    [:div#browserid-box
     [:h2 "BrowserID login"]
     [:a#browserid {:href "#"}
      [:img {:src (resolve-uri "/3rdparty/browserid/sign_in_orange.png")
             :alt "Sign in using BrowserID"}]]]
    [:div#openid-login-panel
     [:h2 "OpenID login"]
     [:form {:action (resolve-uri "/login/authenticate"),
             :method "GET"
             :id     "openid_form"}
      [:div {:id "openid_choice"}
       [:p "Please select your OpenID provider:"]
       [:div {:id "openid_btns"}]]
      [:div {:id "openid_input_area"}
       [:input {:type "text", :name "openid_identifier", :id "openid_identifier"}]
       [:input {:type "submit"}]]]]))
