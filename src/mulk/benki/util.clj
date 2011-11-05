(ns mulk.benki.util
  (:refer-clojure)
  (:use [hiccup core     page-helpers]
        noir.core))


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
  (html5
   [:head [:title title]]
   [:body [:h1 title]
    content]))
