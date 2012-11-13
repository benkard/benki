(ns mulk.benki.util
  (:refer-clojure)
  (:use [hiccup     core page]
        [clojure.core.match :only [match]]
        [noir       core]
        [mulk.benki config db])
  (:require [noir.session  :as session]
            [noir.request  :as request]
            [noir.response :as response]
            [clojure.java.jdbc :as sql]
            [clojure.pprint]
            [clojure.tools.logging :as logging])
  (:import [java.text DateFormat]
           [java.security SecureRandom]
           [java.math BigInteger]
           [org.jsoup Jsoup]
           [org.jsoup.safety Cleaner Whitelist]
           [org.pegdown PegDownProcessor]))


(def fmt clojure.pprint/cl-format)


(def ^:dynamic *user*)
(def ^:dynamic *client-cert*)


(defonce #^:private finished-initializations (atom #{}))


(defn resolve-uri [uri]
  (.toString (.resolve (java.net.URI. (:base-uri @benki-config)) uri)))


(defmacro do-once [key & body]
  `(while (not (@(deref #'finished-initializations) key))
     (let [fininit-copy# @(deref #'finished-initializations)]
       (when (compare-and-set! (deref #'finished-initializations)
                               fininit-copy#
                               (conj fininit-copy# key))
         (do ~@body)))))


;; defpartial is just defn + html.
(defpartial layout [kind title & content]
  (xml-declaration "UTF-8")
  (doctype :html5)
  [:html {:xmlns       "http://www.w3.org/1999/xhtml"
          "xml:lang"   "en"
          :lang        "en"
          "xmlns:cert" "http://www.w3.org/ns/auth/cert#"
          "xmlns:foaf" "http://xmlns.com/foaf/0.1/"
          "xmlns:xsd"  "http://www.w3.org/2001/XMLSchema#"}
   [:head {:data-logged-in      (if *user* "true" "false"),
           :data-websocket-base (:websocket-base @benki-config)}
    [:title title]
    ;; jQuery
    [:script {:type "text/javascript"
              :src (resolve-uri "/3rdparty/jquery/jquery-1.7.min.js")}]
    [:script {:type "text/javascript"
              :src "https://login.persona.org/include.js"
              :defer "defer"}]
    [:script {:type "text/javascript"
              :src (resolve-uri "/js/browserid.js")
              :defer "defer"}]
    [:link {:type "text/css"
            :rel  "stylesheet"
            :href (resolve-uri "/style/benki.css")}]
    [:link {:rel  "profile"
            :href "http://www.w3.org/1999/xhtml/vocab"}]
    [:meta {:content "initial-scale=1.0, width=device-width"
            :name    "viewport"}]
    (:head kind)]
   [:body [:h1 title]
    content
    (:bottom kind)]])

(defmulti user-nickname type)
(defmethod user-nickname java.lang.String [x]
  x)
(defmethod user-nickname java.lang.Number [x]
  (with-dbt
    (:nickname (query1 "SELECT * FROM user_nicknames WHERE \"user\" = ?" x))))

(defn linkrel [& args]
  (match [(vec args)]
    [[:login]]           (str (:cert-req-base @benki-config) "/login")
    [[:home]]            (fmt nil "/")
    [[:marx]]            (fmt nil "/marx")
    [[:marx :submit]]    (fmt nil "/marx/submit")
    [[:marx :feed]]      (fmt nil "/marx/feed")
    [[:marx id]]         (fmt nil "/marx/~a" id)
    [[:lafargue]]        (fmt nil "/lafargue")
    [[:lafargue :feed]]  (fmt nil "/lafargue/feed")
    [[:lafargue :post]]  (fmt nil "/lafargue/post")
    [[:wiki title & xs]] (fmt nil "/wiki/~a~@[~a~]" title (first xs))
    [[:keys]]            "/keys"
    [[:keys :register]]  "/keys/register"
    [[:profile user]]    (fmt nil "/~~~a" (user-nickname user))))


(defn link [& args]
  (resolve-uri (apply linkrel args)))

(defn redirect [x]
  {:status 302, :headers {"Location" x}, :body ""})

(defn call-with-auth [thunk]
  (if *user*
    (thunk)
    (do (session/flash-put! :mulk.benki.auth/return-uri
                            (str (:uri (request/ring-request))
                                 (if-let [q (:query-string (request/ring-request))]
                                   (str "?" q)
                                   "")))
        (redirect (link :login)))))

(defmacro with-auth [& body]
  `(call-with-auth (fn [] ~@body)))

(defn format-date [x]
  (.format (DateFormat/getDateTimeInstance DateFormat/FULL DateFormat/FULL)
           x))

(defonce secure-random (SecureRandom.))
(defn genkey []
  ;;(.toString (BigInteger. 260 secure-random) 32)
  (BigInteger. 260 secure-random))


;;;; * User input
(def pegdown (PegDownProcessor.
              (bit-or org.pegdown.Extensions/SMARTYPANTS
                      org.pegdown.Extensions/ABBREVIATIONS
                      org.pegdown.Extensions/HARDWRAPS
                      org.pegdown.Extensions/TABLES
                      org.pegdown.Extensions/AUTOLINKS
                      org.pegdown.Extensions/DEFINITIONS
                      org.pegdown.Extensions/FENCED_CODE_BLOCKS)))

(def markdown (PegDownProcessor.))

(defn markdown->html [markdown]
  (.markdownToHtml pegdown markdown))

(defn sanitize-html [html]
  (Jsoup/clean html (Whitelist/basic)))


;;;; * Debugging
(defmacro info [& args]
  `(logging/info ~@args))

(defmacro debug [& args]
  `(logging/debug ~@args))

(defmacro log [& args]
  `(logging/info ~@args))
