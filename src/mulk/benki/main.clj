(ns mulk.benki.main
  (:refer-clojure)
  (:use [clojure         core repl pprint]
        noir.core
        [hiccup core     page-helpers]
        [mulk.benki      util])
  (:require noir.server
            [mulk.benki wiki auth]))


(defonce server (doto (Thread. #(noir.server/start 3001))
                  (.setDaemon true)
                  (.start)))

(defn wrap-utf-8 [handler]
  (fn [request]
    (let [response  (handler request)
          ctype     (get-in response [:headers "Content-Type"])
          utf8ctype (str ctype "; charset=utf-8")]
      (if (and ctype
               (re-matches #"^(text/html|text/plain|application/xhtml+xml|text/xml)$" ctype))
        (assoc-in response [:headers "Content-Type"] utf8ctype)
        response))))

(do-once ::init
  (noir.server/add-middleware #'wrap-utf-8)
  ;;(set! *base-url* nil)
  )


(defn -main [& args]
  (loop []
    (Thread/sleep 1000000)
    (recur)))
