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
                                   MessageListener
                                   ChatManagerListener]))


(defonce xmpp        (atom nil))
(defonce messages    (channel))
(defonce messages-in (channel))


(defn- connect []
  (let [xmpp-config       (:xmpp @benki-config)
        connection-config (doto (ConnectionConfiguration. (:server       xmpp-config)
                                                          (:port         xmpp-config)
                                                          (:service-name xmpp-config))
                            (.setSecurityMode (case (:tls xmpp-config)
                                                true      ConnectionConfiguration$SecurityMode/enabled
                                                false     ConnectionConfiguration$SecurityMode/disabled
                                                :required ConnectionConfiguration$SecurityMode/required))
                            (.setVerifyRootCAEnabled (:verify-cert xmpp-config))
                            (.setVerifyChainEnabled  (:verify-cert xmpp-config))
                            ;;(.setCompressionEnabled  true)
                            (.setSASLAuthenticationEnabled (:sasl xmpp-config)))]
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
                                    (when-let [body (.getBody message)]
                                      (when-not (re-find #"^\?OTR:" body)
                                        (enqueue messages-in {:sender recipient, :body body}))))))]
          (.sendMessage chat notification))))))

(defn- startup-client []
  (receive-all messages
               (fn [{targets :targets, msg :message}]
                 (push-message targets msg))))

(defn- handle-incoming-chats []
  (doto (.getChatManager @xmpp)
    (.addChatListener
     (reify ChatManagerListener
       (chatCreated [self chat local?]
         (when-not local?
           (.addMessageListener
            chat
            (reify MessageListener
              (processMessage [self chat message]
                (when-let [body (.getBody message)]
                  (when-not (re-find #"^\?OTR:" body)
                    (enqueue messages-in {:sender (.getParticipant chat) :body body}))))))))))))

(defn init-xmpp! []
  (when-let [xmpp-config (get @benki-config :xmpp)]
    (when (get xmpp-config :enabled true)
      (future
        (reconnect!)
        (handle-incoming-chats)
        (startup-client)))))
