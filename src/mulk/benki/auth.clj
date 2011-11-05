(ns mulk.benki.auth
  (:refer-clojure)
  (:use [clojure         core repl pprint]
        [clojure.contrib error-kit]
        [hiccup core     page-helpers]
        [mulk.benki      util]
        [clojure.core.match.core
         :only [match]]
        noir.core))


(defpage "/login" []
  (layout "Benki Login"
    []))
