(ns parmenides.core-test
  (:require [clojure.test :refer :all]
            [parmenides.core :refer :all]
            [clojure.test.check :as tc]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [clojure.test.check.clojure-test :as ct :refer (defspec)]
            [datomic.api :as d :refer [db q]]))


(def test-attributes
  [{:db/id #db/id[:db.part/db]
    :db/ident :test/id
    :db/valueType :db.type/long
    :db/cardinality :db.cardinality/one
    :db.install/_attribute :db.part/db
    ;:parmenides/ownership :parmenides/individual
    ;:parmenides/reliable true
    ;:parmenides/multiplicity :parmendies/one
    }
   {:db/ident :test/record :db/id #db/id[:db.part/user]}])


(defn get-fresh-conn [s]
  (let [url (str "datomic:mem://parmenides" s)
        conn (do (d/create-database url)
                 (d/connect url))]
    (d/transact conn parmenides-attributes)
    (d/transact conn test-attributes)
    conn))

(defspec simple-test
  1
  (prop/for-all
   [n gen/nat
    random-postfix (gen/such-that #(< 2 (count %)) gen/string-alpha-numeric)]

   (let [conn (get-fresh-conn random-postfix)
         datoms (repeatedly n #(hash-map :test/id 1
                                         :db/id (d/tempid :db.part/user)
                                         :parmenides/record-type :test/record))]
     (continous-resolution conn)
     (d/transact conn datoms)
     (is (= n (count (d/q '[:find ?e
                            :where [?e :test/id ?v]]
                          (db conn)))))
     (is (= 1 (count (d/q '[:find ?e
                            :where [?e :parmenides.being/id ?v]]
                          (db conn)))))


     )))

(run-tests)
