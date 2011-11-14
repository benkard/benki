(ns mulk.benki.db
  (:refer-clojure)
  (:use mulk.benki.util
        clojure.java.io)
  (:import [tokyocabinet HDB ADB]))

(def ^{:private true} db (HDB.))

(defn- db-directory []
  ;; FIXME: This is a hack.
  ;;(file (.getParent (first (clojure.java.classpath/classpath))) "db")
  (file "db"))

(defn call-with-transaction [thunk]
  (let [tr? (.tranbegin db)]
    (if tr?
      (try (let [result (thunk)]
             (.trancommit db)
             result)
           (finally (.tranabort db)))
      (do (.println java.lang.System/err "-- RETRYING TRANSACTION --")
          (Thread/sleep 300)
          (call-with-transaction thunk)))))

(defn call-with-db [thunk mode]
  (let [op? (.open db (str (file (db-directory) "benki.tch")) mode)]
    (if op?
      (try (thunk)
           (finally (.close db)))
      (do (.println java.lang.System/err "-- RETRYING OPENING DB --")
          (Thread/sleep 300)
          (call-with-transaction thunk)))))

(defmacro with-dbw [& body]
  `(call-with-db (fn [] (call-with-transaction (fn [] ~@body)))
     (bit-or HDB/OTSYNC HDB/OWRITER HDB/OCREAT)))

(defmacro with-dbr [& body]
  `(call-with-db (fn [] ~@body) HDB/OREADER))

(defn- dump-str-for-db [x]
  (binding [*print-dup*  true
            *print-meta* true]
    (pr-str x)))

(defn- getkey [key default]
  (let [thing (.get db (str key))]
    (if (nil? thing)
      default
      (read-string thing))))

(defn- putkey [key val]
  (.put db (str key) (dump-str-for-db val)))


(defonce state-vars (atom {}))


(defmacro defstate [sym default]
  (let [dbkey (str *ns* "/" (name sym))]
    `(defonce ~sym
       (let [r# (ref (with-dbr (getkey ~dbkey ~default)))]
         (swap! state-vars #(assoc % ~dbkey r#))
         r#))))


(defn save-all-global-state! []
  (with-dbw
    (dosync
      (doseq [[key r] @state-vars]
        (putkey key (ensure r))))))

(defn reload-all-global-state! []
  (with-dbr
    (dosync
      (doseq [[key r] @state-vars]
        (alter r #(getkey key %))))))


;; Example.
(comment
  (defstate testvar 150))
