(ns mulk.benki.util
  (:refer-clojure)
  (:use [hiccup core page-helpers]
        [clojure.core.match :only [match]]
        noir.core)
  (:require [noir.session  :as session]
            [noir.request  :as request]
            [noir.response :as response])
  (:import [java.text DateFormat]))


(def fmt clojure.pprint/cl-format)


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
   [:head
    [:title title]
    ;; jQuery
    [:script {:type "text/javascript"
              :src (resolve-uri "/3rdparty/jquery/jquery-1.7.min.js")}]
    (:head kind)]
   [:body [:h1 title]
    content
    (:bottom kind)]))


(defn fresolve [s & args]
  (resolve-uri (apply fmt nil s args)))

(defn link [& args]
  (match [(vec args)]
    [[:login]]           (fresolve "/login")
    [[:marx]]            (fresolve "/marx")
    [[:wiki title & xs]] (fresolve "/wiki/~a~@[~a~]" title (first xs))
    ))

(defn call-with-auth [thunk]
  (if (session/get :user)
    (thunk)
    (do (session/flash-put! (:uri (request/ring-request)))
        (response/redirect "/login"))))

(defmacro with-auth [& body]
  `(call-with-auth (fn [] ~@body)))

(defn redirect [x]
  {:status 302, :headers {"Location" x}, :body ""})

(defn format-date [x]
  (.format (DateFormat/getDateTimeInstance DateFormat/FULL DateFormat/FULL)
           x))
