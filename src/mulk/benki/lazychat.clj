(ns mulk.benki.lazychat
  (:refer-clojure)
  (:use [clojure     repl]
        [hiccup      core page-helpers]
        [noir        core]
        [noir-async  core]
        [mulk.benki  auth config db util webutil]
        ;;
        [clojure.core.match :only [match]]
        [hiccup.core        :only [escape-html]]
        [ring.util.codec    :only [url-encode]]
        [lamina.core        :only [channel enqueue enqueue-and-close receive-all
                                   map* filter*]])
  (:require [clojure.algo.monads  :as m]
            [clojure.java.jdbc    :as sql]
            [clojure.string       :as string]
            [noir.request         :as request]
            [noir.response        :as response]
            [noir.session         :as session]
            hiccup.core
            [lamina.core          :as lamina]
            [aleph.http           :as ahttp]
            [aleph.formats        :as aformats]
            [clojure.data.json    :as json])
  (:import [org.apache.abdera Abdera]))


(defonce abdera (Abdera.))
(defonce lafargue-events (channel))


(defn create-lazychat-message! [{content  :content,  visibility :visibility
                                 format   :format,   targets    :targets,
                                 referees :referees, id         :id}]
  {:pre [*user*]}
  (with-dbt
    (when id
      ;; FIXME: Is this assertion sufficient?  Is it too strict?
      (assert (query1 "SELECT 't' WHERE currval('lazychat_messages_id_seq') >= ?" id)))
    (let [id (or id
                 (:id (query1 "SELECT nextval('lazychat_messages_id_seq')::INTEGER AS id")))]
      (sql/with-query-results ids
        ["INSERT INTO lazychat_messages(id, author, content, format, visibility)
               VALUES (?, ?, ?, ?, ?)
            RETURNING id"
         id *user* content format visibility]
        (doseq [referee referees]
          (sql/insert-values :lazychat_references
                             [:referrer :referee]
                             [id (int referee)]))
        (doseq [target targets]
          (sql/insert-values :lazychat_targets
                             [:message :target]
                             [id (int target)]))
        (enqueue lafargue-events
                 {:content  content,  :visibility visibility,
                  :format   format,   :targets    targets,
                  :referees referees, :id         id,
                  :author   *user*,   :date       (java.util.Date.)})))))

(defn select-message [id]
  (let [message  (query1 "SELECT author, content, format, visibility, date
                            FROM lazychat_messages
                           WHERE id = ?"
                         id)
        referees (map :referee (query  "SELECT referee FROM lazychat_references WHERE referrer = ?" id))
        targets  (map :target  (query  "SELECT target  FROM lazychat_targets    WHERE message = ?" id))]
    (and message
         (assoc message
           :referees referees
           :targets  targets))))

(defn may-read? [user message]
  (or (= (:visibility message) "public")
      (and user (= (:visibility message) "protected"))
      (and user
           (= (:visibility message) "personal")
           (contains? (:targets message) user))))

(defn may-post? [user]
  user)


(def lafargue-list-page
  {:head (list
          [:link {:rel "stylesheet"
                  :href (resolve-uri "/style/hammer-and-sickle.css")
                  :type "text/css"}]
          [:link {:rel "stylesheet"
                  :href (resolve-uri "/style/lafargue.css")
                  :type "text/css"}]
          [:script {:src (resolve-uri "/js/lafargue.js")
                    :type "text/javascript"}])})

(defmacro with-messages-visible-by-user [[messages user] & body]
  `(sql/with-query-results ~messages
       ["SELECT m.id, m.author, m.date, m.content, m.format, u.first_name, u.last_name
           FROM lazychat_messages m
           JOIN users u ON (author = u.id)
          WHERE (visibility = 'public'
                 OR (visibility = 'protected' AND (?::INTEGER) IS NOT NULL)
                 OR (visibility = 'personal'
                     AND EXISTS (SELECT *
                                   FROM lazychat_targets t
                                  WHERE t.target = (?::INTEGER)
                                        AND message = m.id)))
          ORDER BY m.date DESC"
        ~user ~user]
    ~@body))


(defn render-message [message]
  (html
   [:li {:class "lafargue-message"}
    [:h2 {:class "lafargue-message-title"}]
    [:p {:class "lafargue-message-date-and-owner"}
     [:span {:class "lafargue-message-date"}
      (escape-html (format-date (:date message)))]
     [:span {:class "lafargue-message-owner"}
      " by " (escape-html (:first_name message))]]
    [:div {:class "lafargue-message-body"}
     (sanitize-html (markdown->html (:content message)))]]))

(defn render-message-as-json [message]
  (json/json-str (assoc message
                   :html (render-message message)
                   :date nil)))


(defpage "/lafargue" {}
  (with-dbt
    (layout lafargue-list-page "Lafargue Lazy Chat"
      [:div {:id "notifications"
             :class "notifications"}
       (login-message)]
      [:div
       [:div {:id "lafargue-main-input-box" :class "lafargue-input-box"}
        [:form {:method "POST" :action (link :lafargue :post)}
         [:div [:textarea {:name "content", :rows 3, :cols 100}]]
         [:div [:input {:type "hidden", :name "format",  :value "markdown"}]]
         [:div
          [:input {:type "radio", :name "visibility", :value "protected" :checked "checked"} "Semi-private"]
          [:input {:type "radio", :name "visibility", :value "public"} "Public"]]
         [:div [:input {:type "submit"}]]]]
       [:ul {:class "lafargue-list"}
        (with-messages-visible-by-user [messages *user*]
          (doall
           (for [message messages]
             (render-message message))))
        [:div {:id "lafargue-footer"}
         (let [feed-link (linkrel :lafargue :feed)]
           [:span {:id "lafargue-footer-text"}
            "[" [:a {:href (resolve-uri feed-link)} "Atom"] "]"
            (when *user*
              (list
               " [" [:a {:href (resolve-uri (authlink feed-link))} "Atom auth"] "]"
               " [" [:a {:href (authlink (:uri (request/ring-request)))} "authlink"] "]"))])]]])))


(defn lazychat-feed-for-user [user]
  (with-dbt
    (with-messages-visible-by-user [messages user]
      (let [last-updated (sql/with-query-results results
                           ["SELECT MAX(date) AS maxdate FROM lazychat_messages"]
                           (:maxdate (first results)))
            feed   (doto (.newFeed abdera)
                     (.setId      (fmt nil "tag:~A,2012:/lafargue"
                                       (:tag-base @benki-config)))
                     (.setTitle   "Lafargue Lazy Chat")
                     (.setUpdated last-updated)
                     (.addLink    (link :lafargue)))]
        (doseq [item messages]
          (doto (.addEntry feed)
            (.setId            (fmt nil "tag:~A,2012:/lafargue/~D"
                                    (:tag-base benki-config)
                                    (:id item)))
            (.setSummaryAsHtml (sanitize-html (markdown->html (:content item))))
            (.setPublished     (:date item))
            ;;(.setAuthor        (fmt nil "~A ~A" (:first_name item) (:last_name item)))
            ;;(.addLink        (link :lafargue (:id item)))
            ))
        (.toString feed)))))


(defpage "/lafargue/feed" {}
  (response/content-type "application/atom+xml; charset=UTF-8"
    (lazychat-feed-for-user *user*)))

(defpage-async "/lafargue/events" {} conn
  (if (websocket? conn)
    (let [messages (filter* #(may-read? *user* %) lafargue-events)]
      (receive-all messages
                   (fn [msg]
                     (async-push conn (render-message-as-json msg)))))
    (async-push conn {:status 426})))

(defpage [:any "/lafargue/post"] {content  :content, visibility :visibility
                                  format   :format,  targets    :targets,
                                  referees :referees}
  (with-auth
    (create-lazychat-message! {:content  content, :visibility visibility,
                               :format   format,  :targets    targets,
                               :referees referees})
    (redirect (referrer))))


(defpage [:any "/lafargue/post"] {content  :content, visibility :visibility
                                  format   :format,  targets    :targets,
                                  referees :referees}
  (with-auth
    (create-lazychat-message! {:content  content, :visibility visibility,
                               :format   format,  :targets    targets,
                               :referees referees})
    (redirect (referrer))))

(defpage [:put "/lafargue/messages/:id"] {id :id}
  (if (may-post? *user*)
    (let [message (assoc (slurp (:body (request/ring-request))) :id id)]
      (create-lazychat-message! message)
      {:status 200})
    {:status 403}))

(defpage [:get "/lafargue/messages/:id"] {id :id}
  (with-dbt
    (let [message (select-message id)]
      (if (may-read? *user* message)
        (response/json message)
        {:status 403}))))

(defpage [:post "/lafargue/messages/genid"] {id :id}
  (with-auth
    (response/json
      (with-dbt (query1 "SELECT NEXTVAL('lazychat_messages_id_seq')")))))
