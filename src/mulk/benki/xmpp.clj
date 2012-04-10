(ns mulk.benki.xmpp
  (:refer-clojure)
  (:use [clojure     repl]
        [noir        core]
        [noir-async  core]
        [mulk.benki  auth config db util webutil]
        ;;
        [clojure.core.match :only [match]]
        [lamina.core        :only [channel enqueue enqueue-and-close receive-all
                                   map* filter*]])
  (:require [clojure.algo.monads  :as m]
            [clojure.java.jdbc    :as sql]
            [clojure.string       :as string]
            [lamina.core          :as lamina]
            [clojure.data.json    :as json])
  (:import [org.jivesoftware.smack ConnectionConfiguration
                                   ConnectionConfiguration$SecurityMode
                                   XMPPConnection
                                   MessageListener]))


(defonce xmpp     (atom nil))
(defonce messages (channel))


(defn- connect []
  (let [xmpp-config       (:xmpp @benki-config)
        connection-config (doto (ConnectionConfiguration. (:server       xmpp-config)
                                                          (:port         xmpp-config)
                                                          (:service-name xmpp-config))
                            (.setSecurityMode (case (:tls xmpp-config)
                                                true      ConnectionConfiguration$SecurityMode/enabled
                                                false     ConnectionConfiguration$SecurityMode/disabled
                                                :required ConnectionConfiguration$SecurityMode/required))
                            (.setVerifyRootCAEnabled true)
                            (.setVerifyChainEnabled  true)
                            ;;(.setCompressionEnabled  true)
                            (.setSASLAuthenticationEnabled true))]
    (doto (XMPPConnection. connection-config)
      (.connect)
      (.login (:user xmpp-config) (:password xmpp-config) (:resource xmpp-config)))))

(defn reconnect! []
  (swap! xmpp #(do (when %
                     (.disconnect %))
                   (connect))))

(defmulti format-message type)

(defn- ->pgarray [coll]
  (fmt nil "{~{~A~^,~}}" coll))

(defn- push-message [targets message]
  (let [chat-manager (.getChatManager @xmpp)
        notification (format-message message)
        recipients   (with-dbt
                       (map :jid
                            (query "SELECT jid FROM user_jids j INNER JOIN unnest(?::INTEGER[]) t ON j.user = t"
                                   (->pgarray targets))))]
    (doseq [recipient recipients]
      (future
        (let [chat (.createChat chat-manager
                                recipient
                                (reify MessageListener
                                  (processMessage [self chat message]
                                    nil)))]
          (.sendMessage chat notification))))))

(defn- startup-client []
  (receive-all messages
               (fn [{targets :targets, msg :message}]
                 (push-message targets msg))))

(defn init-xmpp! []
  (future
    (reconnect!)
    (startup-client)))
