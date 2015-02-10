(ns parmenides.website
  (:require
   [bidi.bidi :refer (path-for)]
   [bidi.ring :refer (redirect)]
   [clojure.pprint :refer (pprint)]
   [clojure.tools.logging :refer :all]
   [com.stuartsierra.component :refer (using)]
   [hiccup.core :as hiccup]
   [modular.bidi :refer (WebService as-request-handler)]
   [modular.ring :refer (WebRequestHandler)]
   [modular.template :refer (render-template template-model)]
   [modular.cljs :refer (get-javascript-paths)]
   [ring.util.response :refer (response)]
   [tangrammer.component.co-dependency :refer (co-using)]
   [clojure.data.json :refer [read-str]]
   [ring.middleware.json :refer [wrap-json-body wrap-json-response]]
   [schema.core :as s]
   [schema-contrib.core :as sc]
   [datomic.api :as d]
   [parmenides.ocd :refer [DisclosureReport]]))


(defn page [{:keys [templater router cljs-builder]} req]
  (response
   (render-template
    templater
    "templates/dashboard.html.mustache" ; our Mustache template
    {:javascripts (get-javascript-paths cljs-builder)})))

(def Registration?
  (s/checker {(s/required-key :dataset_name) s/Str
              (s/required-key :dataset_endpoint) sc/URI}))

(defn register [conn {:keys [dataset_name dataset_endpoint] :as json}]
  (println "register " json)
  (if-let [error-message (Registration? json)]
    {:status 400 :body (str error-message)}
    (let [key (str (d/squuid)) id (str (d/squuid))]
      @(d/transact conn
                   [{:db/id (d/tempid :db.part/user)
                     :dataset/name dataset_name
                     :dataset/endpoint (java.net.URI. dataset_endpoint)
                     :dataset/api-key key
                     :dataset/id id}])
      (response {:dataset_apikey key
                 :dataset_id id}))))

;;Add in disclosure here
(def Morsel  {(s/required-key :dataset_apikey) s/Str
              (s/required-key :dataset_id) s/Str
              (s/optional-key :datum) DisclosureReport
              (s/optional-key :data) [DisclosureReport]})

(def Morsel?  (s/checker Morsel))

(defn valid-creds? [db id key]
  (println "incoming creds " id key)
  (-> (d/q '[:find ?e :in $ ?id ?key :where
             [?e :dataset/id ?id]
             [?e :dataset/api-key ?key]]
           db id key)
      ffirst
      nil?
      not))

(defn ingest [conn {:keys [dataset_id dataset_apikey] :as json}]
  (println "Ingesting")
  (s/validate Morsel json)
  (if-let [error-message (s/check Morsel json)]
    (response {:status 400 :body (str error-message)})
    (if-not (valid-creds? (d/db conn) dataset_id dataset_apikey)
      (response {:status 400 :body "No dataset corresponding to that id/apikey pair."})
      (response {:status 200 :body "Valid data."}))))


(defrecord Website [templater router cljs-builder connection]
  WebService
  (request-handlers [this]
    ;;TODO: figure out how to apply handlers better
    (letfn [(f* [f] (-> f
                        (comp :body)
                        (wrap-json-body {:keywords? true})
                        wrap-json-response))]
      {::dashboard (fn [req] (page this req))
       ::ingest    (f* (partial ingest connection))
       ::register  (f* (partial register connection))}))

  (routes [_] ["/" {:get {"dashboard/" ::dashboard
                          "" (redirect ::dashboard)}
                    :post {"ingest" ::ingest
                           "register" ::register}}])

  ;; A WebService can be 'mounted' underneath a common uri context
  (uri-context [_] ""))

(defn new-website []
  (-> (map->Website {})
      (using [:templater :cljs-builder :connection])
      (co-using [:router])))
