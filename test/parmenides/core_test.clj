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
   ))

;;It used to generate a variable number of id's for each
;;vector. Should it go back to doing that?
(defn gen-id-tuples
  ([] (gen-id-tuples 1))
  ([min]
     (genc/for [number-of-ids   gen/s-pos-int
                number-of-vectors  gen/s-pos-int
                ids (gen/vector (gen/vector gen/int number-of-ids)
                                (max min number-of-vectors))]
       (map (partial map-indexed id-tuple->kv ) ids))))

(def gen-id-tuples-with-match-to-invalidate
  (genc/for [id-tuples (gen-id-tuples 2)
             match (gen/elements (distinct (apply concat id-tuples)))]
    {:id-tuples id-tuples
     :match-to-invalidate match}))

;;TODO: profile this and see if it can be speed up somehow
(defspec many-soul-on-many-ids 10
  (prop/for-all
   [ids (gen-id-tuples)]
   (let [max-number-of-ids (apply max (map count ids))
         conn (get-fresh-conn max-number-of-ids)
         outcome (derive-characteristics ids)]
     @(d/transact conn (mapv id-tuples->datom-map ids))
     (unleash-the-cupids! conn)
     (= outcome
        (characteristics (d/db conn))))))

#_(defspec many-soul-on-many-ids-with-one-divorce 10
  (prop/for-all [data (gen-id-tuples-with-pair-to-divorce)]
                (println ids)
   (let [max-number-of-ids (apply max (map count ids))
         conn (get-fresh-conn max-number-of-ids)
         outcome (derive-characteristics ids)]
     @(d/transact conn (mapv id-tuple->datom-map ids))
     (unleash-the-cupids! con)
     (= outcome
        (characteristics (d/db conn))))))
