(ns parmenides.core-test
  (:use expectations)
  (:require ;[clojure.test :refer :all]
   [expectations :refer :all]
   [parmenides.core :refer :all]
   [clojure.test.check :as tc]
   [clojure.test.check.generators :as gen]
   [clojure.test.check.properties :as prop]
   [clojure.test.check.clojure-test :as ct :refer (defspec)]
   [datomic.api :as d :refer [db q]]
   [clojure.core.async :refer [close! <!!]]
   ))

(alter-var-root (var clojure.pprint/*print-right-margin*) (constantly 140))

;; Taken from simple-expectations
;; https://github.com/jaycfields/simple-expectations/blob/master/test/simple_expectations/core_test.clj
(defrecord SimpleCheck []
  CustomPred
  (expect-fn [e a] (:result a))
  (expected-message [e a str-e str-a] (format "%s of %s failures"
                                              (:failing-size a)
                                              (:num-tests a)))
  (actual-message [e a str-e str-a] (format "fail: %s" (:fail a)))
  (message [e a str-e str-a] (format "shrunk: %s" (get-in a [:shrunk :smallest]))))

(def checked (->SimpleCheck))

;;


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


(defn get-fresh-conn []
  (let [url (str "datomic:mem://parmenides"  (d/squuid))
        conn (do (d/create-database url)
                 (d/connect url))]
    @(d/transact conn parmenides-attributes)
    @(d/transact conn test-attributes)
    conn))


(let [dbc (d/db (get-fresh-conn))]
  (map (partial clarify-datom dbc) (seq (d/datoms dbc :eavt))))

(defn simple-test-first [n]
)

;(expect checked (tc/quick-check 10 (prop/for-all [n gen/nat] (simple-test-first n))))


(simple-test-first 1)
