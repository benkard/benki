(ns mulk.benki.main
  (:refer-clojure)
  (:use [clojure         core repl pprint]
        noir.core
        [hiccup core     page-helpers]
        [mulk.benki      util config db])
  (:require [noir server options]
            [mulk.benki wiki auth book_marx]
            [ring.middleware.file]
            [noir.session      :as session]
            [noir.request      :as request]
            [clojure.java.jdbc :as sql])
  (:import [java.math BigDecimal BigInteger]))


(defn wrap-utf-8 [handler]
  (fn [request]
    (let [response  (handler request)
          ctype     (get-in response [:headers "Content-Type"])
          utf8ctype (str ctype "; charset=utf-8")]
      (if (and ctype
               (re-matches #"^(text/html|text/plain|application/xhtml+xml|text/xml|application/atom+xml)$" ctype))
        (assoc-in response [:headers "Content-Type"] utf8ctype)
        response))))

(defn wrap-base-uri [handler]
  (fn [request]
    (let [base-uri (:base-uri @benki-config)]
      (hiccup.core/with-base-url base-uri
        ((noir.options/wrap-options handler {:base-url base-uri}) request)))))

(defn wrap-cache-control [handler]
  (fn [request]
    (let [response (handler request)]
      (if (get-in response [:headers "Cache-Control"])
        response
        (assoc-in response [:headers "Cache-Control"] "no-cache")
        ;; no-cache, no-store, must-revalidate
        ;; Which one is the most appropriate?
        ;; (is must-revalidate even valid for server responses?)
        ))))

(defn wrap-auth-token [handler]
  (fn [request]
    (binding [*user*
              (or (when-let [key (get-in request [:params :auth])]
                    (with-dbt
                      (sql/with-query-results results
                          ["SELECT \"user\" AS uid FROM page_keys
                             WHERE page = ? AND \"key\" = ?"
                           (:uri request)
                           (BigDecimal. (BigInteger. key 36))]
                        (:uid (first results)))))
                  (session/get :user))]
      (handler request))))

(do-once ::init
  (noir.server/add-middleware #(wrap-utf-8 %))
  (noir.server/add-middleware #(wrap-base-uri %))
  (noir.server/add-middleware #(wrap-auth-token %))
  (noir.server/add-middleware #(wrap-cache-control %))
  (noir.server/add-middleware #(ring.middleware.file/wrap-file % "static")))

(defonce server (doto (Thread. #(noir.server/start (:web-port @benki-config)))
                  (.setDaemon true)
                  (.start)))


(defn -main [& args]
  (loop []
    (Thread/sleep 1000000)
    (recur)))
