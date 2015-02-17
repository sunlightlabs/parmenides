(ns parmenides.core-test
  (:require
   [clojure.test.check :as tc]
   [clojure.test.check.generators :as gen]
   [clojure.test.check.properties :as prop]
   [clojure.test.check.clojure-test :as ct :refer (defspec)]
   [datomic.api :as d :refer [db q]]
   [parmenides.util-test :refer :all]
   [parmenides.util :refer :all]
   [parmenides.resolution :refer [unleash-the-cupids!]]
   [com.gfredericks.test.chuck :as chuck]
   [com.gfredericks.test.chuck.generators :as genc]
   [clojure.pprint :refer [pprint]]))

(defn gen-id-tuples
  ([] (gen-id-tuples 1 10))
  ([min-number-of-vectors max-id-val]
     (genc/for [number-of-ids   gen/s-pos-int
                number-of-vectors  gen/s-pos-int
                ids (gen/vector (gen/vector (gen/resize max-id-val gen/int)
                                            number-of-ids)
                                (max min-number-of-vectors number-of-vectors))]
       (map (partial map-indexed id-tuple->kv ) ids))))

(def gen-id-tuples-with-match-to-invalidate
  (genc/for [id-tuples (gen-id-tuples 2 1)
             match (gen/elements (distinct (apply concat id-tuples)))]
    {:id-tuples id-tuples
     :match-to-invalidate [match]}))

(defn many-soul-on-many-ids
  [ids]
  (let [max-number-of-ids (apply max (map count ids))
        conn (get-fresh-conn max-number-of-ids)
        outcome (derive-characteristics ids)]
    @(d/transact conn (mapv id-tuples->datom-map ids))
    (unleash-the-cupids! conn)
    (= outcome
       (characteristics (d/db conn)))))

;;TODO: profile this and see if it can be speed up somehow
(defspec many-soul-on-many-ids* 10
  (prop/for-all [ids (gen-id-tuples)] (many-soul-on-many-ids ids)))

(defn many-soul-on-many-ids-with-one-removal
  [{ids :id-tuples [[attr v] :as matches] :match-to-invalidate}]
  (let [max-number-of-ids (apply max (map count ids))
        conn (get-fresh-conn max-number-of-ids)
        outcome (derive-characteristics ids matches)]
    @(d/transact conn (mapv id-tuples->datom-map ids))
    (unleash-the-cupids! conn)
;    @(d/transact conn [[:invalidate-match (:match/id matches)]])
    (println "shouldbe" outcome)
    (println "actual"(characteristics (d/db conn)))
    (println "")
    (= outcome
       (characteristics (d/db conn)))))

(defspec many-soul-on-many-ids-with-one-removal* 10
  (prop/for-all [data gen-id-tuples-with-match-to-invalidate]
                (many-soul-on-many-ids-with-one-removal data)))

(gen/sample gen-id-tuples-with-match-to-invalidate)
;(many-soul-on-many-ids-with-one-removal*)

(many-soul-on-many-ids-with-one-removal
 '{:id-tuples (([:test/id-0 1]) ([:test/id-0 1])),
  :match-to-invalidate [[:test/id-0 1]]})
