(ns parmenides.core
  (:require [datomic.api :as d]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
            [aleph.http :as http]))

(declare server)

(defroutes app
  (GET "/index.html" [] "test")
  (POST "/ingest" [body] (println body) "HELLO PAULTAG SHALL WE PLAY A GAME?")
  (route/not-found "OH SNAP SON"))

(when (bound? (var server)) (.close server))

(def server (http/start-server (wrap-defaults app api-defaults) {:port 80}))
