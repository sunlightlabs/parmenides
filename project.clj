(defproject parmenides "0.1.0-SNAPSHOT"
  :description "Record Linkage Service"
  :url "https://github.com/sunlightlabs/parmenides"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [;;Basics
                 [org.clojure/clojure "1.7.0-alpha4"]

                 ;;Backend
                 [aleph "0.4.0-beta1"]
                 [org.clojure/data.json "0.2.5"]
                 [instaparse "1.3.5"]
                 [org.jordanlewis/data.union-find "0.1.0"]
                 [clj-time "0.7.0"]
                 [com.taoensso/timbre "3.2.1"]
                 [com.velisco/herbert "0.6.6"]
                 [com.datomic/datomic-free "0.9.5130" :exclusions [joda-time]]

                 ;;Crossover
                 [bidi "1.12.0"]

                 ;;Frontend
                 [org.clojure/clojurescript "0.0-2511"]
                 [org.om/om "0.8.0"]
                 [datascript "0.8.0"]
                 [racehub/om-bootstrap "0.3.3"]]
  :profiles {:dev {:dependencies [[org.clojure/test.check "0.5.9"]
                                  [expectations "2.0.9"]]}})
