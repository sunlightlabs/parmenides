(ns parmenides.core-test
  (:require
   [clojure.test.check.clojure-test :as ct :refer (defspec)]
   [clojure.test.check :as tc]
   [clojure.test.check.generators :as gen]
   [clojure.test.check.properties :as prop]
   [com.gfredericks.test.chuck :as chuck]
   [com.gfredericks.test.chuck.generators :as genc]
   [com.gfredericks.test.chuck.properties :as propc]
   [datomic.api :as d :refer [db q]]
   [parmenides.util-test :refer :all]
   [parmenides.util :refer :all]
   [parmenides.resolution :refer [hunt-for-soulmates]]
   [clojure.pprint :refer [pprint]]))
;;TODO: profile this and see if it can be speed up somehow
;;TODO: spec out an options map for this so that there aren't magic numbers
;;TODO: work out if test.check can shrink the data down further
;;TODO: check that records retain an id from one of the given records if possible

(defn datoms-generator
  [db & {:keys [min-number-of-vectors max-id-val
                max-number-of-ids]
         :or {min-number-of-vectors 1
              max-id-val 10
              max-number-of-ids 5}}]
  (genc/for [number-of-ids   gen/s-pos-int
             number-of-vectors  gen/s-pos-int
             ids (gen/vector (gen/vector (gen/resize max-id-val gen/pos-int)
                                         (min  max-number-of-ids number-of-ids))
                             (max min-number-of-vectors number-of-vectors))
             :let [records (mapv id-tuple->datom-map ids)]]
    [(d/with db records)
     records]))

(defn apply-matches [db soulmates]
  (reduce (fn [{db :db-after} match] (d/with db [match]))
          {:db-after db}
          soulmates))

(defn run-matches
  [db]
  (let [soulmates (hunt-for-soulmates db)]
    [(apply-matches db soulmates)
     soulmates]))

(defn result-attr? [k]
  (= "result" (name k)))

(defn removal-generator [db]
  (genc/for [matches (gen/return (d/q '[:find [?match-id]
                                        :where
                                        [?match :match/id ?match-id]]
                                      db))
             to-remove (gen/elements matches)
             :let [data [[:invalidate-match to-remove]]]]
    [(d/with db data)
     data
     (d/pull db '[*] [:match/id to-remove])]))

(defspec simple-matching 10
  (propc/for-all
   [db           (gen/return (get-fresh-db 10))
    [{db :db-after} records] (datoms-generator db)
    [{db :db-after} matched] (gen/return (run-matches db))]
   (= (derive-characteristics records)
      (characteristics db))))

(defspec simple-matching-with-removal 10
  (propc/for-all
   [fresh-db           (gen/return (get-fresh-db 10))

    [{populated-db :db-after} records]
    (datoms-generator fresh-db
                      :min-number-of-vectors 3
                      :max-number-of-ids 1
                      :max-id-val 2)

    [{matched-db :db-after} matched]
    (gen/return (run-matches populated-db))

    [{cleaned-db :db-after} removed removed-match]
    (removal-generator matched-db)
    :let [left-over-matches
          (filter (fn [[_ e _ v _]]
                    (and (not= e (get-in matched [:match/cupid :db/id]))
                         (not= v (->> (filter (comp result-attr? first) removed-match)
                                      first
                                      second))))
                  matched)
          actual (characteristics cleaned-db)
          calculated
          (->  (apply-matches populated-db left-over-matches)
                :db-after
                characteristics
                (update-in [:number-of-matches :invalid] inc))]]
   (= calculated
      actual)))


(let [m (-> (simple-matching-with-removal)
          (get-in  [:shrunk :smallest])
          first)
      pp (fn [sym] (println (str sym)) (pprint (m sym)) (println ""))]
  (pp 'actual)
  (pp 'calculated)
  (pp 'records)
  (pp 'matched)
  (pp 'removed-match)

  (pprint m))
