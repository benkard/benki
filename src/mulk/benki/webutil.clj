(ns mulk.benki.webutil
  (:refer-clojure)
  (:use [hiccup      core page-helpers]
        [clojure.core.match :only [match]]
        noir.core
        [mulk.benki  db util])
  (:require [noir.session  :as session]
            [noir.request  :as request]
            [noir.response :as response]
            [clojure.java.jdbc :as sql])
  (:import [java.text DateFormat]
           [java.math BigDecimal]))



(defn authlink []
  (with-dbt
    (let [req  (request/ring-request)
          user *user*
          uri  (:uri req)
          dkey (sql/with-query-results results
                   ["SELECT * FROM page_keys WHERE \"user\" = ? AND page = ?"
                    user uri]
                 (if-let [rec (first results)]
                   (:key rec)
                   (let [key (BigDecimal. (genkey))]
                     (sql/with-query-results results
                         ["INSERT INTO page_keys(\"user\", page, \"key\")
                                VALUES (?, ?, ?)
                             RETURNING \"key\""
                          user uri key]
                       (:key (first results))))))
          key  (.toBigIntegerExact dkey)]
      (fmt nil "~A?auth=~A" uri (.toString key 36)))))


(defpartial login-message []
  (let [user    (and *user*
                     (with-dbt (sql/with-query-results results
                                 ["SELECT * FROM users WHERE id = ?" *user*]
                                 (first results))))]
    (if *user*
      [:div {:class "logged-in-as"}
       (:first_name user) " " (:last_name user)
       " "
       [:a {:href (authlink)} "[authlink]"]]
      [:div {:class "not-logged-in"} [:a {:href (link :login)} "Log in"]])))
