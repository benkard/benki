(ns mulk.benki.config
  (:refer-clojure))

(def benki-config
  (read-string (slurp "config.sexp")))
