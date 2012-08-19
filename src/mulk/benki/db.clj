(ns mulk.benki.db
  (:refer-clojure)
  (:use [mulk.benki config]
        [clojure.pprint :only [cl-format]])
  (:require [clojure.java.jdbc :as sql]))


(defn ^:private classic-db []
  (let [{host :host, port :port, database :database,
         user :user, password :password}
        (:database @benki-config)]
    {:classname "org.postgresql.Driver"
     :subprotocol "postgresql"
     :subname (cl-format nil "//~A:~D/~A" host port database)
     :user user
     :password password}))

(defonce ^:private +immutant-db+ (atom nil))

(defn ^:private immutant-db []
  (if @+immutant-db+
    {:datasource @+immutant-db+}
    (let [{host :host, port :port, database :database,
           user :user, password :password}
          (:database @benki-config)]
      (when (try
              (require 'immutant.xa)
              true
              (catch Exception e
                false))
        (let [datasource (ns-resolve 'immutant.xa 'datasource)]
          (reset! +immutant-db+
                  (datasource "benkidb"
                              {:adapter  "postgres"
                               :host     host
                               :port     port
                               :database database
                               :username user
                               :password password
                               }))
          {:datasource @+immutant-db+})))))

(defn ^:private db []
  (or (immutant-db) (classic-db)))


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
