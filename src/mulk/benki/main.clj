(ns mulk.benki.main
  (:refer-clojure)
  (:use [clojure         core repl pprint]
        noir.core
        [hiccup          core page]
        [mulk.benki      config db util])
  (:require [noir.core]
            [noir server options]
            [ring.middleware.file]
            [ring.middleware.file-info]
            [noir.session      :as session]
            [noir.request      :as request]
            [clojure.java.jdbc :as sql]
            [lamina.core       :as lamina]
            [aleph.http        :as ahttp]
            [aleph.formats     :as aformats]
            [ring.util.codec   :as codec]
            [clojure.algo.monads :as m]
            [clojure.data.json   :as json])
  (:import [java.math BigDecimal BigInteger])
  (:gen-class))


(defn wrap-missing-status-code [handler]
  (fn [request]
    (let [response (handler request)]
      ;; NB. This is a work-around for aleph.http not accepting
      ;; responses that do not contain a :status property.  This
      ;; includes `nil` responses.
      (assoc response :status (get response :status 404)))))

(defn wrap-utf-8 [handler]
  (fn [request]
    (let [response  (handler request)
          ctype     (get-in response [:headers "Content-Type"])
          utf8ctype (str ctype "; charset=utf-8")]
      (if (and ctype
               (re-matches #"^(text/html|text/plain|application/xhtml+xml|text/xml|application/atom+xml)$" ctype))
        (assoc-in response [:headers "Content-Type"] utf8ctype)
        response))))

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

(defn parse-certificate [cert-data]
  (let [{modulus :modulus,
         exponent :exponent,
         fingerprint :fingerprint,
         valid-to :valid_to
         valid-from :valid_from
         subject-alt-name :subjectaltname
         subject :subject
         }
        cert-data]
    (if modulus
      {:modulus          (bigint (BigInteger. modulus 16))
       :exponent         (bigint (BigInteger. exponent 16))
       :fingerprint      fingerprint
       :valid-to         (org.joda.time.DateTime. (Long. valid-to))
       :valid-from       (org.joda.time.DateTime. (Long. valid-from))
       :subject          subject
       :subject-alt-name subject-alt-name}
      nil)))

(defn wrap-client-cert [handler]
  (fn [request]
    (binding [*client-cert*
              (m/domonad m/maybe-m
                         [cert-json (get-in request [:headers "x-mulk-peer-certificate"])
                          cert-data (json/read-json cert-json)
                          cert      (parse-certificate cert-data)]
                cert)]
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

(defn init-config! []
  (reset! benki-config (read-string (slurp (.getFile (clojure.java.io/resource "config.sexp"))))))

(defn init-middleware! []
  (noir.server/add-middleware #(ring.middleware.file-info/wrap-file-info %))
  (noir.server/add-middleware #(hiccup.middleware/wrap-base-url % (:base-uri @benki-config)))
  (noir.server/add-middleware #(wrap-missing-status-code %))
  (noir.server/add-middleware #(wrap-utf-8 %))
  (noir.server/add-middleware #(wrap-auth-token %))
  (noir.server/add-middleware #(wrap-client-cert %))
  (noir.server/add-middleware #(wrap-cache-control %))
  (noir.server/add-middleware #(ring.middleware.file/wrap-file % "static"))
  (noir.server/add-middleware #(wrap-extension-mimetype %)))

(defn run-server []
  (let [mode         (or (:mode @benki-config) :production)
        noir-handler (noir.server/gen-handler {:mode mode,
                                               :ns 'mulk.benki,
                                               :base-url (:base-uri @benki-config)})]
    (ahttp/start-http-server (ahttp/wrap-ring-handler noir-handler)
                             {:port      (:web-port @benki-config)
                              :websocket true})))

(defonce server (atom nil))

(defn init-security! []
  (java.security.Security/addProvider
   (org.bouncycastle.jce.provider.BouncyCastleProvider.))
  (Class/forName "net.java.dev.sommer.foafssl.sesame.verifier.SesameFoafSslVerifier"))

(defn run-immutant-swank!
  "Call `immutant.repl/start-swank` with the Swank configuration
  specified in `benki-config`.  If loading the immutant.repl namespace
  fails, ignore the error and return `nil` without doing anything.  In
  case of success, return true."  []
  (when-let [swank-config (get @benki-config :swank)]
    (when (and (get swank-config :enabled true)
               (try
                 (require 'immutant.repl)
                 true
                 (catch Exception e
                   false)))
      ((ns-resolve 'immutant.repl 'start-swank)
       (:bind swank-config) (:port swank-config))
      true)))

(defn run-immutant-nrepl!
  "Call `immutant.repl/start-nrepl` with the Swank configuration
  specified in `benki-config`.  If loading the immutant.repl namespace
  fails, ignore the error and return `nil` without doing anything.  In
  case of success, return true."  []
  (when-let [swank-config (get @benki-config :nrepl)]
    (when (and (get swank-config :enabled true)
               (try
                 (require 'immutant.repl)
                 true
                 (catch Exception e
                   false)))
      ((ns-resolve 'immutant.repl 'start-nrepl)
       (:bind swank-config) (:port swank-config))
      true)))

(defn init! []
  (init-config!)
  (init-middleware!)
  (init-security!)
  ;;(noir.server/load-views-ns 'mulk.benki) ;doesn't work on Immutant
  (doseq [ns '(mulk.benki.book_marx mulk.benki.lazychat mulk.benki.id)]
    (require ns))
  (future (run-immutant-swank!))
  (future (run-immutant-nrepl!))
  (future (require 'mulk.benki.xmpp)
          ((ns-resolve 'mulk.benki.xmpp 'init-xmpp!)))
  (future (require 'mulk.benki.lazychat)
          ((ns-resolve 'mulk.benki.lazychat 'init-lazychat!)))
  (future (reset! server (run-server))))

(defn -main [& args]
  (init!)
  (loop []
    (Thread/sleep 1000000)
    (recur)))
