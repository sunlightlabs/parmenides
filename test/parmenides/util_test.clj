(ns parmenides.util-test
  (:use expectations)
  (:require
   [expectations :refer :all]
   [datomic.api :as d :refer [db q]]
   [parmenides.resolution :refer [attributes dbfn]]))
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

(def test-attributes
  (let [id-1 (d/tempid :db.part/db)
        id-2 (d/tempid :db.part/db)
        fm (dbfn :inc [] inc)]
    [{:db/id id-1
      :db/ident :test/id-1
      :db/valueType :db.type/long
      :db/cardinality :db.cardinality/one
      :db.install/_attribute :db.part/db}
     {:db/id id-2
      :db/ident :test/id-2
      :db/valueType :db.type/long
      :db/cardinality :db.cardinality/one
      :db.install/_attribute :db.part/db}
     [:new-cupid
      {:cupid/description "Match entities based on their id-1 values."
       :cupid/attributes [id-1]}]
     fm
     [:new-cupid
      {:cupid/description "Match entities based on their id-1 values."
       :cupid/attributes [id-2]
       :cupid/transform-combinator (:db/id fm)
       }]]))

(defn get-fresh-conn []
  (let [url (str "datomic:mem://parmenides"  (d/squuid))
        conn (do (d/create-database url)
                 (d/connect url))]
    @(d/transact conn attributes)
    @(d/transact conn test-attributes)
    conn))
