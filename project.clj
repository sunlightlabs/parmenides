(defproject parmenides "0.1.0-SNAPSHOT"
  :description "Record Linkage Service"
  :url "https://github.com/sunlightlabs/parmenides"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [;;Basics
                 [org.clojure/clojure "1.7.0-alpha4"]

                 ;;Webserver and requests
                 [aleph "0.4.0-beta1"]           ;;HTTP Server
                 [ring/ring-defaults "0.1.3"]    ;;Hooks compojure up to
                                                 ;;aleph (via the ring
                                                 ;;specification)
                 [compojure "1.3.1"]             ;;Router
                 [hiccup "1.0.5"]                ;;HTML generation
                 [org.clojure/data.json "0.2.5"] ;;Working with JSON

                 ;;Data transformations
                 [instaparse "1.3.5"]                      ;;General parsing
                 [org.jordanlewis/data.union-find "0.1.0"] ;;Union-find implementation
                 [clj-time "0.7.0"]                        ;;Timestamp parsers
                 [prismatic/schema "0.3.3"]
                 [schema-contrib "0.1.3"]

                 ;;Miscellaneous
                 [com.taoensso/timbre "3.2.1"]         ;;Logging
                 [org.clojure/tools.analyzer.jvm "0.6.5"]

                 ;;Data storage
                 [com.datomic/datomic-free "0.9.5130" :exclusions [joda-time]]

                 ;;Frontend tools
                 [org.clojure/clojurescript "0.0-2511"]
                 [org.om/om "0.8.0"]
                 [racehub/om-bootstrap "0.3.3"]
                 [datascript "0.8.0"]
                 [secretary "1.2.1"]]
  :profiles {:dev {:dependencies [[org.clojure/test.check "0.5.9"]
                                  [expectations "2.0.9"]]}})
