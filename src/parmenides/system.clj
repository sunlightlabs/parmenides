(ns parmenides.system
  "Components and their dependency relationships"
  (:refer-clojure :exclude (read))
  (:require
   [clojure.java.io :as io]
   [clojure.tools.reader :refer (read)]
   [clojure.string :as str]
   [clojure.tools.reader.reader-types :refer (indexing-push-back-reader)]
   [com.stuartsierra.component :refer (system-map system-using using)]
   [tangrammer.component.co-dependency :refer (co-using system-co-using)]
   [modular.maker :refer (make)]
   [modular.bidi :refer (new-router new-static-resource-service)]
   [modular.cljs :refer (new-cljs-builder new-cljs-module)]
   [modular.clostache :refer (new-clostache-templater)]
   [modular.http-kit :refer (new-webserver)]
   [modular.less :refer (new-bootstrap-less-compiler)]
   [modular.datomic :refer
    (new-datomic-database new-datomic-connection new-datomic-schema)]
   [parmenides.website :refer (new-website)]))

(defn ^:private read-file
  [f]
  (read
   ;; This indexing-push-back-reader gives better information if the
   ;; file is misconfigured.
   (indexing-push-back-reader
    (java.io.PushbackReader. (io/reader f)))))

(defn ^:private config-from
  [f]
  (if (.exists f)
    (read-file f)
    {}))

(defn ^:private user-config
  []
  (config-from (io/file (System/getProperty "user.home") ".parmenides.edn")))

(defn ^:private config-from-classpath
  []
  (if-let [res (io/resource "parmenides.edn")]
    (config-from (io/file res))
    {}))

(defn config
  "Return a map of the static configuration used in the component
  constructors."
  []
  (merge (config-from-classpath)
         (user-config)))



(defn http-listener-components
  [system config]
  (assoc system
    :http-listener-listener
    (make new-webserver config {:port [:http-server :port]})))

(defn modular-bidi-router-components
  [system config]
  (assoc system
    :modular-bidi-router-webrouter
    (make new-router config)))


(defn clostache-templater-components
  [system config]
  (assoc system
    :clostache-templater-templater
    (make new-clostache-templater config)))

(defn twitter-bootstrap-components
  "Serve Twitter Bootstrap CSS, Javascript and other resources from a
   web-jar."
  [system config]
  (assoc system
    :twitter-bootstrap-service
    (make new-static-resource-service config :uri-context "/bootstrap"
          :resource-prefix "META-INF/resources/webjars/bootstrap/3.3.0")))

(defn dashboard-resources-components
  [system config]
  (assoc system
    :dashboard-resources-static
    (make new-static-resource-service config :uri-context "/"
          :resource-prefix "public/")))

(defn single-cljs-module-components
  [system config]
  (assoc system
    :single-cljs-module-cljs-core
    (make new-cljs-module config :name :cljs-core :mains [(quote cljs.core)]
          :dependencies #{})
    :single-cljs-module-cljs-app
    (make new-cljs-module config :name :default :mains [(quote parmenides.core)]
          :dependencies #{:cljs-core})
    :single-cljs-module-cljs-builder
    (->
      (make new-cljs-builder config :source-path "src-cljs")
      (using [:single-cljs-module-cljs-core :single-cljs-module-cljs-app]))))

(defn dashboard-website-components
  [system config]
 (assoc system
    :dashboard-website-website
    (make new-website config)))

(defn jquery-components
  "Serve JQuery resources from a web-jar."
  [system config]
  (assoc system
    :jquery-resources
    (make new-static-resource-service config :uri-context "/jquery" :resource-prefix "META-INF/resources/webjars/jquery/2.1.0")))

(defn reactjs-components
  "Serve Facebook React resources from a web-jar."
  [system config]
  (assoc system
    :reactjs-resources
    (make new-static-resource-service config :uri-context "/reactjs" :resource-prefix "META-INF/resources/webjars/react/0.11.1")))

(defn bootstrap-less-compiler-components
  "Compile Bootstrap LESS files to CSS."
  [system config]
  (assoc system
    :bootstrap-less-compiler-compiler
    (make new-bootstrap-less-compiler config)))

(defn datomic-database-components
  "Datomic database"
  [system config]
  (assoc system
    :database
    (make new-datomic-database config :uri nil :ephemeral? nil)))

(defn datomic-connection-components
  "Compile Bootstrap LESS files to CSS."
  [system config]
  (assoc system
    :connection
    (make new-datomic-connection config)))

(defn datomic-schema-components
  "Compile Bootstrap LESS files to CSS."
  [system config]
  (assoc system
    :schema
    (new-datomic-schema (io/resource "schema.edn"))))

(defn new-system-map
  [config]
  (apply system-map
    (apply concat
      (-> {}
          (http-listener-components config)
          (modular-bidi-router-components config)
          (clostache-templater-components config)
          (twitter-bootstrap-components config)
          (dashboard-resources-components config)
          (single-cljs-module-components config)
          (dashboard-website-components config)
          (jquery-components config)
          (reactjs-components config)
          (bootstrap-less-compiler-components config)
          (datomic-database-components config)
          (datomic-connection-components config)
          (datomic-schema-components config)
          ))))

(defn new-dependency-map
  []
  {:http-listener-listener {:request-handler :modular-bidi-router-webrouter},:modular-bidi-router-webrouter {:twitter-bootstrap :twitter-bootstrap-service, :static :dashboard-resources-static, :cljs-builder :single-cljs-module-cljs-builder, :website :dashboard-website-website, :jquery :jquery-resources, :reactjs :reactjs-resources, :compiler :bootstrap-less-compiler-compiler}, :dashboard-website-website {:templater :clostache-templater-templater, :cljs-builder :single-cljs-module-cljs-builder}})

(defn new-co-dependency-map
  []
  {:dashboard-website-website {:router :modular-bidi-router-webrouter}})

(defn new-production-system
  "Create the production system"
  ([opts]
   (-> (new-system-map (merge (config) opts))
     (system-using (new-dependency-map))
     (system-co-using (new-co-dependency-map))))
  ([] (new-production-system {})))
