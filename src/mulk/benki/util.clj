(ns mulk.benki.util
  (:refer-clojure)
  (:use [hiccup core page-helpers]
        [clojure.core.match :only [match]]
        noir.core)
  (:require [noir.session  :as session]
            [noir.request  :as request]
            [noir.response :as response]
            [clojure.java.jdbc :as sql])
  (:import [java.text DateFormat]
           [java.security SecureRandom]
           [java.math BigInteger]))


(def fmt clojure.pprint/cl-format)


(def ^:dynamic *user*)


(defonce #^:private finished-initializations (atom #{}))

(defmacro do-once [key & body]
  `(while (not (@(deref #'finished-initializations) key))
     (let [fininit-copy# @(deref #'finished-initializations)]
       (when (compare-and-set! (deref #'finished-initializations)
                               fininit-copy#
                               (conj fininit-copy# key))
         (do ~@body)))))


;; defpartial is just defn + html.
(defpartial layout [kind title & content]
  (html5 {:xml? true}
   [:head {:data-logged-in (if *user* "true" "false")}
    [:title title]
    ;; jQuery
    [:script {:type "text/javascript"
              :src (resolve-uri "/3rdparty/jquery/jquery-1.7.min.js")}]
    [:script {:type "text/javascript"
              :src (resolve-uri "/3rdparty/browserid/include.js")}]
    [:script {:type "text/javascript"
              :src (resolve-uri "/js/browserid.js")}]
    [:link {:type "text/css"
            :rel  "stylesheet"
            :href (resolve-uri "/style/benki.css")}]
    [:meta {:content "initial-scale=1.0, width=device-width"
            :name    "viewport"}]
    (:head kind)]
   [:body [:h1 title]
    content
    (:bottom kind)]))

(defn linkrel [& args]
  (match [(vec args)]
    [[:login]]           (fmt nil "/login")
    [[:marx]]            (fmt nil "/marx")
    [[:marx :submit]]    (fmt nil "/marx/submit")
    [[:marx :feed]]      (fmt nil "/marx/feed")
    [[:marx id]]         (fmt nil "/marx/~a" id)
    [[:wiki title & xs]] (fmt nil "/wiki/~a~@[~a~]" title (first xs))
    ))

(defn link [& args]
  (resolve-uri (apply linkrel args)))

(defn call-with-auth [thunk]
  (if *user*
    (thunk)
    (do (session/flash-put! (str (:uri (request/ring-request))
                                 (if-let [q (:query-string (request/ring-request))]
                                   (str "?" q)
                                   "")))
        (response/redirect "/login"))))

(defmacro with-auth [& body]
  `(call-with-auth (fn [] ~@body)))

(defn redirect [x]
  {:status 302, :headers {"Location" x}, :body ""})

(defn format-date [x]
  (.format (DateFormat/getDateTimeInstance DateFormat/FULL DateFormat/FULL)
           x))

(defonce secure-random (SecureRandom.))
(defn genkey []
  ;;(.toString (BigInteger. 260 secure-random) 32)
  (BigInteger. 260 secure-random))
