(ns mulk.benki.util
  (:refer-clojure)
  (:use [hiccup core page-helpers]
        [clojure.core.match.core :only [match]]
        noir.core)
  (:require [noir.session  :as session]
            [noir.request  :as request]
            [noir.response :as response]))


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
(defpartial layout [title & content]
  (html5 {:xml? true}
   [:head
    [:title title]
    ;; jQuery
    [:script {:type "text/javascript"
              :src (resolve-uri "/3rdparty/jquery/jquery-1.7.min.js")}]
    ;; Aloha Editor
    [:link {:rel "stylesheet"
            :href (resolve-uri "/3rdparty/alohaeditor/aloha/css/aloha.css")}]
    [:script {:type "text/javascript"
              :src (resolve-uri "/3rdparty/alohaeditor/aloha/lib/aloha.js")
              :data-aloha-plugins "common/format,common/highlighteditables,common/list,common/link,common/undo,common/paste,common/block"}]
    ;; JavaScript
    [:script {:type "text/javascript"
              :src (resolve-uri "/js/wiki.js")}]]
   [:body [:h1 title]
    content]))


(defn fresolve [s & args]
  (resolve-uri (apply fmt nil s args)))

(defn link [& args]
  (match [(vec args)]
    [[:wiki title & xs]] (fresolve "/wiki/~a~@[~a~]" title (first xs))))

(defn call-with-auth [thunk]
  (if (session/get :user)
    (thunk)
    (do (session/flash-put! (:uri (request/ring-request)))
        (response/redirect "/login"))))

(defmacro with-auth [& body]
  `(call-with-auth (fn [] ~@body)))
