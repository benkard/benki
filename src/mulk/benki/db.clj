(ns mulk.benki.db
  (:refer-clojure)
  (:use mulk.benki.util)
  (:require [clojure.java.jdbc :as sql]))


(def ^:private db
  {:classname "org.postgresql.Driver"
   :subprotocol "postgresql"
   :subname "//localhost:5432/benki"
   :user "benki"
   :password ""})

(defn call-with-db [thunk]
  (sql/with-connection db
    (thunk)))

(defmacro with-db [& body]
  `(call-with-db (fn [] ~@body)))

(defmacro with-dbt [& body]
  `(call-with-db (fn [] (sql/transaction ~@body))))

(defmacro query [query-string & params]
  `(sql/with-query-results results# ~(into [] (concat [query-string] params))
     (into '() results#)))
