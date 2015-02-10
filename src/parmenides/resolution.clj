(ns parmenides.resolution
  (:require [datomic.api :as d]
            [clojure.pprint :refer [*print-right-margin*]]
            [clojure.pprint :refer [pprint]]
            [parmenides.util :refer [breakout prune pretty-datom]]))

(def uri  "datomic:mem://testing")
(d/delete-database uri)
(d/create-database uri)
(def conn (d/connect uri))

@(d/transact
 conn
 [{:db/id #db/id[:db.part/db]
   :db/ident :id-1
   :db/valueType :db.type/long
   :db/cardinality :db.cardinality/one
   :db/unique :db.unique/identity
   :db.install/_attribute :db.part/db}
  {:db/id #db/id[:db.part/db]
   :db/ident :id-2
   :db/valueType :db.type/long
   :db/cardinality :db.cardinality/one
   :db/unique :db.unique/identity
   :db.install/_attribute :db.part/db}
  {:db/id #db/id[:db.part/db]
   :db/ident :slot-1
   :db/valueType :db.type/ref
   :db/cardinality :db.cardinality/one
   :db.install/_attribute :db.part/db}
  {:db/id #db/id[:db.part/db]
   :db/ident :slot-2
   :db/valueType :db.type/ref
   :db/cardinality :db.cardinality/one
   :db.install/_attribute :db.part/db}
  ])

@(d/transact
  conn
  [{:db/id #db/id[:db.part/user]
    :slot-1 {:db/id #db/id[:db.part/user]
             :id-1 1
             :id-2 1}
    :slot-2 {:db/id #db/id[:db.part/user]
             :id-1 1
             :id-2 2}}]
  )
#_@(d/transact
  conn
  [{:db/id #db/id[:db.part/db]
    :db/ident :id-1
    :db/valueType :db.type/long
    :db/cardinality :db.cardinality/one
    :db.install/_attribute :db.part/db}
   {:db/id #db/id[:db.part/db]
    :db/ident :id-2
    :db/valueType :db.type/long
    :db/cardinality :db.cardinality/one
    :db.install/_attribute :db.part/db}
   {:db/id #db/id[:db.part/db]
    :db/ident :tag
    :db/valueType :db.type/long
    :db/cardinality :db.cardinality/one
    :db/unique :db.unique/identity
    :db.install/_attribute :db.part/db}

   {:db/id #db/id [:db.part/user]
    :db/ident :identity
    :db/fn
    (d/function '{:lang :clojure
                  :params [db arg]
                  :code (identity arg)} )}

   {:db/id #db/id[:db.part/db]
    :db/ident :match/name
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one
    :db.install/_attribute :db.part/db}

   {:watch/name  :match-on-1
    :watch/description "Match entities based on their id-1 values."
    :watch/watched-attributes [:id-1]
    :watch/transfrom :identity}

   {:match/watch 121312313
    :match/serializided-value "[:match-on-1 1]"
    :match/hashed-value (.hashCode "[:match-on-1 1]")
    :match/entites [123213213123]
    }
   ])
#_@(d/transact
  conn
  [{:db/id (d/tempid :db.part/user)
    :tag (int (* 100000000 (rand)))
    :id-1 1
    :id-2 2}
   {:db/id (d/tempid :db.part/user)
    :tag (int (* 100000000 (rand)))
    :id-1 2
    :id-2 2}
   {:db/id (d/tempid :db.part/user)
    :tag (int (* 100000000 (rand)))
    :id-1 2
    :id-2 1}
   {:db/id (d/tempid :db.part/user)
    :tag (int (* 100000000 (rand)))
    :id-1 3
    :id-2 3}
   {:db/id (d/tempid :db.part/user)
    :tag (int (* 100000000 (rand)))
    :id-1 (int (* 10 (rand)))
    :id-2 (int (* 10 (rand)))}])

#_(def queue (d/tx-report-queue conn))
#_(def taker
  (future (while true
            (->> (.take queue)
                 :tx-data
                 (map (partial pretty-datom (d/db conn)))
                 pprint
                 ))))

#_(as-> (d/log conn) $
     (d/tx-range $ nil nil)
     (seq $)
     (map :data $)
     (map (partial map (partial pretty-datom (d/db conn))) $)
     (rest $)
     (map (comp prune (partial breakout 1)) $)
     (filter (comp (partial not= 1) count) $))
