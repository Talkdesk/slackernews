(ns slackernews.db
  (:require [mount.core :refer [defstate]]
            [rethinkdb.query :as r]
            [rethinkdb.core :as rc]
            [environ.core :refer [env]]))

(defn parse-rethinkdb-uri
  "Parses an URI in the format rethinkdb://:auth@host:port/database
  to a map to be passed on to the rethinkdb.query/connect function"
  [uri]
  (let [uri (new java.net.URI uri)
        host (.getHost uri)
        port (if (= (.getPort uri) -1) 28015 (.getPort uri))
        db (clojure.string/join (rest (.getPath uri)))]
    {:host host :port port :db db}))

(defstate conn
  :start (apply r/connect (apply concat (parse-rethinkdb-uri (env :database-uri))))
  :stop  (rc/close conn))

(defn ensure-db
  ""
  [db-name]
  (-> (r/branch (r/contains (r/db-list) db-name)
                {:config_changes [] :dbs_created 0}
                (r/db-create db-name))
      (r/run conn)))

(defn ensure-table
  ""
  [table-name]
  (-> (r/branch (r/contains (r/table-list) table-name)
                {:config_changes [] :tables_created 0}
                (r/table-create table-name))
      (r/run conn)))

(defn ensure-index
  ""
  [table index-name]
  (-> (r/branch (r/contains (r/index-list table) index-name)
                {:created 0}
                (r/index-create table index-name))
      (r/run conn)))

(defstate db-migration
  :start (let [db (:db (parse-rethinkdb-uri (env :database-uri)))]
           (ensure-db db)
           (ensure-table "users")
           (ensure-table "channels")
           (ensure-table "messages")
           (ensure-table "links")
           (let [t (r/table "links")]
             (ensure-index t "ts")
             (ensure-index t "channel")
             (ensure-index t "user")
             (ensure-index t "host"))
           (let [t (r/table "messages")]
             (ensure-index t "ts")
             (ensure-index t "channel")
             (ensure-index t "user"))))

(defn get-all-users
  "Fetches all users from the database"
  []
  (-> (r/table "users")
      (r/run conn)))

(defn get-user-by-id
  "Fetches a user given its id"
  [user-id]
  (-> (r/table "users")
      (r/get user-id)
      (r/run conn)))

(defn get-user-by-name
  "Fetches a user given its name"
  [user-name]
  (-> (r/table "users")
      (r/filter (r/fn [row]
                  (r/eq user-name (r/get-field row :name))))
      (r/run conn)))

(defn insert-user
  "Inserts a user into the database"
  [user]
  (-> (r/table "users")
      (r/insert user {:conflict :update :durability :hard})
      (r/run conn)))

(defn get-channel-by-id
  "Fetches a channel by its id"
  [channel-id]
  (-> (r/table "channels")
      (r/filter (r/fn [row]
                  (r/eq channel-id (r/get-field row :id))))
      (r/run conn)
      first))

(defn get-channel-by-name
  "Fetches a channel by its name"
  [channel-name]
  (-> (r/table "channels")
      (r/filter (r/fn [row]
                  (r/eq channel-name (r/get-field row :name))))
      (r/run conn)
      first))

(defn insert-channel
  "Inserts a channel into the database"
  [channel]
  (-> (r/table "channels")
      (r/insert channel {:conflict :update :durability :hard})
      (r/run conn)))

(defn insert-message
  "Inserts a message into the database"
  [message]
  (-> (r/table "messages")
      (r/insert message {:conflict :update :durability :hard})
      (r/run conn)))

(defn get-last-message-from-channel
  "Fetches the last message of the channel given by its id"
  [channel-id]
  (-> (r/table "messages")
      (r/order-by {:index (r/desc :ts)})
      (r/filter (r/fn [row]
                  (r/eq channel-id (r/get-field row :channel))))
      (r/limit 1)
      (r/run conn)
      first))

(defn get-links
  "Fetches all links detected by slack within messages"
  [& {:keys [page] :or {page 0}}]
  (let [per-page 25
        skip (* page per-page)]
    (-> (r/table "links")
        (r/order-by {:index (r/desc :ts)})
        (r/skip skip)
        (r/limit per-page)
        (r/run conn))))

(defn get-links-from-channel
  "Fetches all links in a given channel detected by slack"
  [channel-id]
  (-> (r/table "messages")
      (r/order-by {:index (r/desc :ts)})
      (r/filter (r/fn [row]
                  (r/and
                   (r/eq channel-id (r/get-field row :channel))
                   (r/gt (r/count (r/get-field row :attachments)) 0))))
      (r/run conn)))

(defn get-all-messages
  []
  (-> (r/table "messages")
      (r/order-by {:index (r/desc :ts)})
      (r/filter (r/fn [row]
                  (-> ["bot_message"]
                      (r/contains (r/get-field row :subtype))
                      (r/default true))))
      (r/run conn)))

(defn insert-link
  "Inserts a link into the database"
  [link]
  (-> (r/table "links")
      (r/insert link {:conflict :update :durability :hard})
      (r/run conn)))
