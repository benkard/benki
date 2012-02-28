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
  (:import [java.text DateFormat]))


(defpartial login-message []
  (let [user-id (session/get :user)
        user    (and user-id
                     (with-dbt (sql/with-query-results results
                                   ["SELECT * FROM users WHERE id = ?" user-id]
                                 (first results))))]
    (if user-id
      [:div {:class "logged-in-as"}  (:first_name user) " " (:last_name user)]
      [:div {:class "not-logged-in"} [:a {:href (link :login)} "Log in"]])))
