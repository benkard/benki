(ns mulk.benki.main
  (:refer-clojure)
  (:use [clojure         core repl pprint]
        noir.core
        [hiccup core     page-helpers]
        [mulk.benki      util config db])
  (:require [noir server options]
            [mulk.benki wiki auth book_marx id lazychat]
            [ring.middleware.file]
            [noir.session      :as session]
            [noir.request      :as request]
            [clojure.java.jdbc :as sql]
            [lamina.core       :as lamina]
            [aleph.http        :as ahttp]
            [aleph.formats     :as aformats]
            [ring.util.codec   :as codec])
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

(defn wrap-extension-mimetype [handler]
  (fn [request]
    (let [uri       (codec/url-decode (:uri request))
          response  (handler request)
          extension (second (re-find #"\.([\w]*)($|\?)" uri))
          exttype   ({"txt"  "text/plain"
                      "css"  "text/css"
                      "js"   "text/javascript"
                      "html" "text/html"
                      "jpg"  "image/jpeg"
                      "gif"  "image/gif"
                      "png"  "image/png"}
                     extension)]
      (if (and (nil? (get-in response [:headers "Content-Type"]))
               exttype)
        (assoc-in response [:headers "Content-Type"] exttype)
        response))))

(do-once ::init
  (noir.server/add-middleware #(wrap-utf-8 %))
  (noir.server/add-middleware #(wrap-base-uri %))
  (noir.server/add-middleware #(wrap-auth-token %))
  (noir.server/add-middleware #(wrap-cache-control %))
  (noir.server/add-middleware #(ring.middleware.file/wrap-file % "static"))
  (noir.server/add-middleware #(wrap-extension-mimetype %)))

(defn run-server []
  (let [mode         (or (:mode @benki-config) :production)
        noir-handler (noir.server/gen-handler {:mode mode})]
    (reset! server
            (ahttp/start-http-server (ahttp/wrap-ring-handler noir-handler)
                                     {:port      (:web-port @benki-config)
                                      :websocket true}))))

(defonce server
  (run-server))

(defn -main [& args]
  (loop []
    (Thread/sleep 1000000)
    (recur)))
