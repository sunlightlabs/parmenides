(ns parmenides.core-test
  (:require
   [clojure.test.check :as tc]
   [clojure.test.check.generators :as gen]
   [com.gfredericks.test.chuck.properties :as prop']
   [clojure.test.check.properties :as prop]
   [clojure.test.check.clojure-test :as ct :refer (defspec)]
   [datomic.api :as d :refer [db q]]
   [parmenides.util-test :refer :all]
   [parmenides.util :refer :all]
   [parmenides.resolution :refer [hunt-for-soulmates]]))

(defn print-db [dbc]
  (as-> dbc $
        (d/datoms $ :eavt)
        (seq $ )
        (drop 215 $)
        (map (partial pretty-datom dbc) $)
        (group-by last $)
        (update-in* $ [:all :all] (partial take 3))
        (into (sorted-map) $)
  ))

(defn characteristics [db]
  {:number-of-souls
   (or (d/q '[:find (count ?soul-id) .
              :where [_ :soul/id ?soul-id]]
            db)
       0)
   :number-of-matches
   (or (d/q '[:find (count ?match) .
              :where [?match :match/cupid _]]
            db)
       0)
   :number-of-records
   (or (d/q '[:find (count ?record) .
              :where [?record :soul/id _]]
            db)
       0)})

#_(defspec zero-entities 1
  (prop/for-all
   []
   (let [conn (get-fresh-conn)]
     (= 0 (number-of-souls (d/db conn))))))

#_(defspec one-entity 10
  (prop/for-all
   [id-1 gen/int
    n (gen/such-that (complement zero?) gen/pos-int)]
   (let [conn (get-fresh-conn)]
     @(d/transact conn
                  (take n (repeatedly (fn [] {:db/id (d/tempid :db.part/user)
                                              :soul/id (str (d/squuid))
                                              :test/id-1 id-1}))))
     @(d/transact conn (hunt-for-soulmates (d/db conn)))
     (= 1 (number-of-souls (d/db conn))))))

#_(one-entity)

(let [conn (get-fresh-conn)
      id-1 10
      n 2]
  @(d/transact conn
               (take n (repeatedly (fn [] {:db/id (d/tempid :db.part/user)
                                           :soul/id (str (d/squuid))
                                           :test/id-1 id-1}))))
  (doseq [data (hunt-for-soulmates (d/db conn))]
    (println data)
    @(d/transact conn [data])
    )
  (print-db (d/db conn))
  )
