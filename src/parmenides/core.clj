(ns parmenides.core
  (:require [datomic.api :as d]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
            [aleph.http :as http]
            [schema.core :as s]
            [parmenides.ocd :refer [OCD]]
            [clojure.data.json :refer [read-str]
                                        ;[clojure.java.shell :refer [sh]]
             ]))


(def last-requests (atom []))

(defn ingest [{body :body params :params}]
  ;;(println (str params) (quot (System/currentTimeMillis) 1000))
  (when-not (:save params)
    (let [filename (str "example-data/"(str (d/squuid)))]
      (spit filename (slurp body))
      (println filename)))
  (let [{data "data" apikey "apikey"} (read-str (slurp body))]
    (println apikey)
    (swap! last-requests concat data)))

(defroutes app
  (GET "/index.html" [] "test")
  (POST "/ingest" request (ingest request) "HELLO PAULTAG SHALL WE PLAY A GAME?\n")
  (route/not-found "OH SNAP SON"))

(declare server)
(when (bound? (var server)) (.close server))

(def server (http/start-server (wrap-defaults app api-defaults) {:port 80}))

(count (clojure.java.shell/sh "./test.sh"))
(count (map (partial s/validate OCD) @last-requests))
