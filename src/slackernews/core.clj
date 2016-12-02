(ns slackernews.core
  (:require [mount.core :as mount]
            [slackernews.app :as app]
            [clojure.tools.logging :as log]
            [clojure.tools.cli :refer [parse-opts]]
            [environ.core :refer [env]]
            [clojure.core.async :refer [<!! >!! alts!! chan close! go-loop timeout thread]])
  (:gen-class))

(mount/defstate http-server
  :start (app/start-server {:handler (app/handler) :port 3000})
  :stop (app/stop-server http-server))

(defn stop-app []
  (doseq [component (-> (mount/stop)
                        :stopped)]
    (log/info component "stopped")))

(defn start-app [& args]
  (doseq [component (-> args
                        mount/start-with-args
                        :started)]
    (log/info component "started"))
  (.addShutdownHook (Runtime/getRuntime) (Thread. stop-app)))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (start-app args))
