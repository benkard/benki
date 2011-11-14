(ns mulk.benki.db
  (:refer-clojure)
  (:use mulk.benki.util
        clojure.java.io)
  (:import [com.sleepycat.je
            Database DatabaseConfig DatabaseEntry Environment EnvironmentConfig
            OperationStatus]))

(def ^{:dynamic true :private true} *txn*)
(def ^{:dynamic true :private true} *db*)

(defn- db-directory []
  ;; FIXME: This is a hack.
  ;;(file (.getParent (first (clojure.java.classpath/classpath))) "db")
  (file "db"))

(defn- make-db-env []
  (let [envconfig (doto (EnvironmentConfig.)
                    (.setTransactional            true)
                    (.setAllowCreate              true)
                    (.setTxnSerializableIsolation true))]
    (Environment. (db-directory) envconfig)))

(defonce dbenv (make-db-env))

(defn call-with-transaction [thunk]
  (binding [*txn* (.beginTransaction dbenv nil nil)]
    (try (let [result (thunk)]
           (.commit *txn*)
           result)
      (finally (.abort *txn*)))))

(defn call-with-db [thunk]
  (let [dbconfig (doto (DatabaseConfig.)
                   (.setTransactional            true)
                   (.setAllowCreate              true)
                   (.setSortedDuplicates         false))]
    (call-with-transaction
      (fn []
        (binding [*db* (.openDatabase dbenv *txn* "benki" dbconfig)]
          (thunk))))))

(defmacro with-db [& body]
  `(call-with-db (fn [] ~@body)))

(defn- dump-str-for-db [x]
  (binding [*print-dup*  true
            *print-meta* true]
    (pr-str x)))

(defn- getkey [key default]
  (let [entry (DatabaseEntry.)]
    (if (= (.get *db* *txn* (DatabaseEntry. (.getBytes key)) entry nil)
           OperationStatus/SUCCESS)
      (-> entry
          (.getData)
          (String.)
          (read-string))
      default)))

(defn- putkey [key val]
  (let [bdbkey key]
    (.put *db*
          *txn*
          (DatabaseEntry. (.getBytes bdbkey))
          (DatabaseEntry. (.getBytes (dump-str-for-db val))))))


(defonce state-vars (atom {}))


(defmacro defstate [sym default]
  (let [dbkey (str *ns* "/" (name sym))]
    `(defonce ~sym
       (let [r# (ref (with-db (getkey ~dbkey ~default)))]
         (swap! state-vars #(assoc % ~dbkey r#))
         r#))))


(defn save-all-global-state! []
  (with-db
    (dosync
      (doseq [[key r] @state-vars]
        (putkey key (ensure r))))))

(defn reload-all-global-state! []
  (with-db
    (dosync
      (doseq [[key r] @state-vars]
        (alter r #(getkey key %))))))
