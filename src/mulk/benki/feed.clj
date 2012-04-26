(ns mulk.benki.feed
  (:refer-clojure)
  (:use [clojure    repl]
        [mulk.benki util config webutil])
  (:require [clojure.algo.monads  :as m]
            [clojure.string       :as string]
            [hiccup.core])
  (:import [org.apache.abdera Abdera]))


(defonce abdera (Abdera.))


(defmulti feed-add-entry (fn [feed item] (type item)))

(defmethod feed-add-entry :mulk.benki.book_marx/bookmark [feed item]
  (doto (.addEntry feed)
    (.setId            (fmt nil "tag:~A,2012:/marx/~D"
                            (:tag-base benki-config)
                            (:id item)))
    (.setTitle         (:title item))
    (.setSummaryAsHtml (:html item))
    ;;(.setUpdated     (:updated item))
    (.setPublished     (:date item))
    ;;(.setAuthor      (fmt nil "~A ~A" (:first_name item) (:last_name item)))
    ;;(.addLink        (link :marx (:id item)))
    (.addLink          (:uri item))))

(defmethod feed-add-entry :mulk.benki.lazychat/lazychat-message [feed item]
  (doto (.addEntry feed)
    (.setId            (fmt nil "tag:~A,2012:/lafargue/~D"
                            (:tag-base benki-config)
                            (:id item)))
    (.setSummaryAsHtml (sanitize-html (markdown->html (:content item))))
    (.setPublished     (:date item))
    ;;(.setAuthor        (fmt nil "~A ~A" (:first_name item) (:last_name item)))
    ;;(.addLink        (link :lafargue (:id item)))
    ))

(defn generate-feed [title last-updated tag link items]
  (let [feed (doto (.newFeed abdera)
               (.setId      (fmt nil "tag:~A,2012:/~A"
                                 (:tag-base @benki-config)
                                 tag))
               (.setTitle   title)
               (.setUpdated last-updated)
               (.addLink    link))]
    (doall (map #(feed-add-entry feed %) items))
    (.toString feed)))
