(ns mulk.benki.config
  (:refer-clojure))

(def benki-config
  (atom (read-string (slurp "config.sexp"))))
