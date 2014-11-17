(defproject parmenides "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]]
  :profiles {:dev {:dependencies [[com.datomic/datomic-free "0.9.5067"]
                                  [org.clojure/test.check "0.5.9"]
                                  [org.clojure/tools.reader "0.8.12"]]}})
