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
   [datomic.api :as d]))


(defn page [{:keys [templater router cljs-builder]} req]
  (response
   (render-template
    templater
    "templates/dashboard.html.mustache" ; our Mustache template
    {:javascripts (get-javascript-paths cljs-builder)})))

(def Registration?
  (s/checker {(s/required-key "dataset_name") s/Str
              (s/required-key "dataset_endpoint") s/Str}))

(defn register [conn {:keys [dataset_name dataset_endpoint] :as json}]
  (println json)
  (if-let [error-message (Registration? json)]
    {:status 400 :body (str error-message)}
    (let [key (d/squuid) id (d/squuid)]
      (d/transact conn
                  [{:dataset/name dataset_name
                     :dataset/endpoint dataset_endpoint
                     :dataset/api-key key
                     :dataset/id id}])
      (response {:apikey key
                 :id id}))))


(def Morsel?  (s/checker {(s/required-key "apikey") s/Str
                          (s/required-key "data") [{}]}))

(defn ingest [conn {:keys [apikey data] :as json}]
  (if-let [error-message (Morsel? json)]
    {:status 400 :body error-message}
    {:status 202 :body "Ingested. Processing."}))


(defrecord Website [templater router cljs-builder connection]
  WebService
  (request-handlers [this]
    ;;TODO: figure out how to apply handlers better
    (letfn [(f* [f] (wrap-json-response (wrap-json-body (comp f :body))))]
      {::dashboard (fn [req] (page this req))
       ::ingest    (partial ingest connection)
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
