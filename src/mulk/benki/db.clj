(ns mulk.benki.db
  (:refer-clojure)
  (:use [mulk.benki util config])
  (:require [clojure.java.jdbc :as sql]))


(defn ^:private db []
  (:database @benki-config))

(defn call-with-db [thunk]
  (sql/with-connection (db)
    (thunk)))

(defmacro with-db [& body]
  `(call-with-db (fn [] ~@body)))

(defmacro with-dbt [& body]
  `(call-with-db (fn [] (sql/transaction ~@body))))

(defmacro query [query-string & params]
  `(sql/with-query-results results# ~(into [] (concat [query-string] params))
     (into '() results#)))

(defmacro query1 [query-string & params]
  `(first (query ~query-string ~@params)))
