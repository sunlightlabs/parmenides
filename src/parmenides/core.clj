(ns parmenides.core
  (:require [clojure.test.check :as tc]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [clojure.test.check.clojure-test :as ct :refer (defspec)]
            [datomic.api :as d :refer [db q]]))

(def parmenides-attributes
  [{:db/id #db/id[:db.part/db]
    :db/ident :parmenides/record-type
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one
    :db.install/_attribute :db.part/db}])

(def test-attributes
  [{:db/id #db/id[:db.part/db]
    :db/ident :id
    :db/valueType :db.type/long
    :db/cardinality :db.cardinality/one
    :db.install/_attribute :db.part/db}])


(defn get-fresh-dbc [random-postfix]
  (let [url (str "datomic:mem://parmenides" random-postfix)
        conn (do (d/create-database url)
                 (d/connect url))]

    (d/transact conn parmenides-attributes)
    (d/transact conn test-attributes)
    conn))

(resolve-log)
(resolve-range)
(contiually-resolve-transactions)
;; full-resolve scans the entire database
;; cont

(def simple-test
  (prop/for-all
   [random-postfix (gen/not-empty gen/string-alpha-numeric)
    v (gen/vector gen/int)
    n gen/nat]
   (let [conn (get-fresh-dbc random-postfix)]
     (full-resolve conn)
     (d/transact conn (take n (cycle [{:id 1} {:id 2}])))

     (= n (count (d/q '[:find ?e
                        :where [?e :id]]
                      (db conn))))

     (= n (count (d/q '[:find ?e
                        :where [?e :id]]
                      (db conn))))

     (= (sort v) (sort (sort v))))))

(tc/quick-check 10 simple-test)
