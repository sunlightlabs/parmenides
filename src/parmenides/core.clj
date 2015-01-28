(ns parmenides.core
  (:require [datomic.api :as d]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
            [aleph.http :as http]
            [schema.core :as s]
            [parmenides.ocd :refer [OCD]]
            [clojure.data.json :refer [read-str]]
            [taoensso.timbre :as timbre]))
(timbre/refer-timbre)

(def last-requests (atom []))
(defn ingest [{body :body params :params}]
  (info "recieved file")
  (let [body (slurp body)]
    (when-not (:save params)
      (let [filename (str "example-data/"(str (d/squuid)))]
        (spit filename body)
        (info filename)))
    (let [{data "data" apikey "api-key"} (read-str body)]
      (swap! last-requests concat data))))

(defroutes app
  (GET "/index.html" [] "test")
  (POST "/ingest" request (ingest request) "*burp*\n")
  (route/not-found "OH SNAP SON"))

(declare server)
(when (bound? (var server)) (.close server))

(def server (http/start-server (wrap-defaults app api-defaults) {:port 80}))

#_(count (clojure.java.shell/sh "./smalltest.sh"))
#_(count (clojure.java.shell/sh "./fulltest.sh"))


(count (map (partial s/validate OCD) @last-requests))
1
