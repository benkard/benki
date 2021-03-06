(ns mulk.benki.auth
  (:refer-clojure)
  (:use [clojure         core repl pprint]
        [hiccup          core page]
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
           [org.openid4java.message ParameterList]
           [net.java.dev.sommer.foafssl.claims WebIdClaim]))


(defonce manager (ConsumerManager.))



(defn find-user [user-id]
  (first (if user-id
           (query "SELECT * FROM users WHERE id = ?" user-id)
           nil)))

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
              user    (find-user user-id)]
          (if user-id
            (do (session/put! :user user-id)
                (if-let [return-uri (session/flash-get ::return-uri)]
                  (redirect return-uri)
                  (layout {} "Authenticated!" [:p "Welcome back, " (:first_name user) "!"])))
            (layout {}
                    "Authentication Failed"
                    [:p "Did not recognize OpenID."]
                    [:p "Your OpenID is: " [:strong (.getIdentifier id)]]))))
      (layout {} "Authentication Failed" [:p "OpenID authentication failed."]))))


(defpage [:post "/login/browserid/verify"] {assertion :assertion}
  ;; NB.  Can implement this ourselves if we want.
  (let [reply  (http/post "https://verifier.login.persona.org/verify"
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
            (let [return-uri (session/flash-get ::return-uri)]
              (session/put! :user user-id)
              (response/json {:email email, :returnURI return-uri}))
            {:status 422,
             :headers {"Content-Type" "text/plain"},
             :body "I couldn't find you in the database."})))
      {:status 400,
       :headers {"Content-Type" "text/plain"},
       :body "Your Persona request was crooked."})))


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

(defn try-webid [cert]
  (log (fmt nil "Attempting WebID authentication."))
  (let [webid (second (re-find #"^URI:(.*)" (:subject-alt-name cert)))
        modulus (:modulus cert)
        exponent (:exponent cert)
        pubkey
        (.generatePublic (java.security.KeyFactory/getInstance
                          "RSA")
                         (java.security.spec.RSAPublicKeySpec.
                          (BigInteger. (str modulus)) (BigInteger. (str exponent))))]
    (log (fmt nil "Verifying WebID: ~a" webid))
    (if (.verify (WebIdClaim. (java.net.URI. webid) pubkey))
      (do
        (log "WebID verified!")
        (with-dbt
          (:user
           (query1 "SELECT \"user\" FROM webids WHERE webid = ?" webid))))
      (log "WebID verification failed."))))

(defpage "/login" []
  (let [return-uri (or (session/flash-get ::return-uri)
                       (get-in (request/ring-request) [:headers "referer"]))]
    (with-dbt
      (if-let [cert-user-id (and *client-cert*
                                 (:user
                                  (query1 "SELECT \"user\" FROM user_rsa_keys
                                            WHERE modulus = (?::NUMERIC)
                                                  AND exponent = (?::NUMERIC)"
                                          (str (:modulus *client-cert*))
                                          (str (:exponent *client-cert*)))))]
        (let [cert-user (find-user cert-user-id)]
          (session/put! :user cert-user-id)
          (if return-uri
            (redirect return-uri)
            (layout {} "Authenticated!" [:p "Welcome back, " (:first_name cert-user) "!"])))
        (if-let [webid-user-id (and *client-cert* (try-webid *client-cert*))]
          (let [cert-user (find-user webid-user-id)]
            (session/put! :user webid-user-id)
            (if return-uri
              (redirect return-uri)
              (layout {} "Authenticated!" [:p "Welcome back, " (:first_name cert-user) "!"])))
          (do
            (session/flash-put! ::return-uri return-uri)
            (layout login-page-layout "Benki Login"
              [:div#browserid-box
               [:h2 "Mozilla Persona login"]
               [:a#browserid {:href "#"}
                [:img {:src (resolve-uri "/3rdparty/browserid/sign_in_orange.png")
                       :alt "Sign in using Mozilla Persona"}]]]
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
                 [:input {:type "submit"}]]]])))))))
  
