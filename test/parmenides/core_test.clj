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
   [parmenides.resolution :refer [unleash-the-cupids!]]
   [jordanlewis.data.union-find :refer :all]))

(defn print-db [dbc]
  (as-> dbc $
        (d/datoms $ :eavt)
        (seq $ )
        (drop 215 $)
        (map (partial pretty-datom dbc) $)
        (group-by last $)
        (update-in* $ [:all :all] (partial take 3))
        (into (sorted-map) $)))

(def or-zero #(or % 0))

(defn number-of-souls [id-tuples]
  (let [mapped-tuples
        (map (partial map-indexed #(hash-map :schema %1 :val %2)) id-tuples)
        uf       (apply union-find (apply concat mapped-tuples))]
    (count (reduce (fn [uf [a b]] (union uf a b)) uf mapped-tuples))))

(defn characteristics [db]
  {:number-of-souls
   (or-zero (d/q '[:find (count ?soul-id) .
              :where [_ :soul/id ?soul-id]]
            db))
   :number-of-matches
   (or-zero (d/q '[:find (count ?match) .
              :where [?match :match/cupid _]]
            db))
   :number-of-records
   (or-zero (d/q '[:find (count ?record) .
              :where [?record :soul/id _]]
            db))})

(defspec no-souls-on-no-idss 1
  (prop/for-all
   []
   (let [conn (get-fresh-conn)]
     (= {:number-of-souls 0
         :number-of-records 0
         :number-of-matches 0}
        (characteristics (d/db conn))))))

(defspec one-soul-on-one-id 10
  (prop/for-all
   [id-1 gen/int
    n (gen/such-that (complement zero?) gen/pos-int)]
   (let [conn (get-fresh-conn)]
     @(d/transact conn
                  (take n (repeatedly (fn [] {:db/id (d/tempid :db.part/user)
                                              :soul/id (str (d/squuid))
                                              :test/id-1 id-1}))))
     (unleash-the-cupids! conn)
     (= {:number-of-souls 1
         :number-of-records n
         :number-of-matches 1}
        (characteristics (d/db conn))))))

(defspec many-souls-on-one-id 10
  (prop/for-all
   [numbers (gen/vector (gen/tuple
                         gen/int (gen/such-that (complement zero?) gen/pos-int)))]
   (let [conn (get-fresh-conn)]
     (doseq [[id n] numbers]
       @(d/transact conn
                    (take n (repeatedly (fn [] {:db/id (d/tempid :db.part/user)
                                                :soul/id (str (d/squuid))
                                                :test/id-1 id})))))
     (unleash-the-cupids! conn)
     (= {:number-of-souls (count (distinct (map first numbers)))
         :number-of-records (reduce + (map second numbers))
         :number-of-matches (count (distinct (map first numbers)))}
        (characteristics (d/db conn))))))

(defspec one-soul-on-two-ids 40
  (prop/for-all
   [ids (gen/vector (gen/tuple gen/int gen/int))]
   ;(println ids)
   (let [conn (get-fresh-conn)
         outcome
         {:number-of-souls (number-of-souls ids)
          :number-of-matches (+ (count (distinct (map first ids)))
                                (count (distinct (map second ids))))
          :number-of-records (count ids)}]
     @(d/transact conn
                  (mapv (fn [[a b]]
                          {:db/id (d/tempid :db.part/user)
                           :soul/id (str (d/squuid))
                           :test/id-1 a
                           :test/id-2 b})
                        ids))

     (unleash-the-cupids! conn)
     (= outcome
        (characteristics (d/db conn))))))
