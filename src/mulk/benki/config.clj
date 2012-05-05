(ns mulk.benki.config
  (:refer-clojure))

(def benki-config
  (atom (read-string (slurp (.getFile (clojure.java.io/resource "config.sexp"))))))
