(ns parmenides.core
  (:require [datomic.api :as d]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [aleph.http :as http]))

(declare server)

(defroutes app
  (GET "/index.html" [] "test"))

(when (bound? (var server)) (.close server))

(def server (http/start-server (wrap-defaults app site-defaults) {:port 8080}))
