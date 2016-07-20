(defproject slack-archive "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [clj-http "2.2.0"]
                 [com.apa512/rethinkdb "0.15.26"]
                 [org.clojure/data.json "0.2.6"]
                 [mount "0.1.10"]
                 [clj-time "0.12.0"]]
  :main ^:skip-aot slack-archive.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
