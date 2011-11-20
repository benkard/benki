(ns mulk.benki.main
  (:refer-clojure)
  (:use [clojure         core repl pprint]
        noir.core
        [hiccup core     page-helpers]
        [mulk.benki      util])
  (:require [noir server options]
            [mulk.benki wiki auth]
            [ring.middleware.file]))


(defn wrap-utf-8 [handler]
  (fn [request]
    (let [response  (handler request)
          ctype     (get-in response [:headers "Content-Type"])
          utf8ctype (str ctype "; charset=utf-8")]
      (if (and ctype
               (re-matches #"^(text/html|text/plain|application/xhtml+xml|text/xml)$" ctype))
        (assoc-in response [:headers "Content-Type"] utf8ctype)
        response))))

(defn wrap-base-uri [handler]
  (fn [request]
    (let [base-uri "http://localhost:3001"]
      (with-base-url base-uri
        ((noir.options/wrap-options handler {:base-url base-uri}) request)))))

(do-once ::init
  (noir.server/add-middleware #(wrap-utf-8 %))
  (noir.server/add-middleware #(wrap-base-uri %))
  ;;(noir.server/add-middleware #(ring.middleware.static/wrap-static ))
  (noir.server/add-middleware #(ring.middleware.file/wrap-file % "static")))

(defonce server (doto (Thread. #(noir.server/start 3001))
                  (.setDaemon true)
                  (.start)))


(defn -main [& args]
  (loop []
    (Thread/sleep 1000000)
    (recur)))
