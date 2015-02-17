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

(defn test-datoms-generator
  [db & {:keys [min-number-of-vectors max-id-val
                max-number-of-ids]
         :or {min-number-of-vectors 1
              max-id-val 10
              max-number-of-ids 5}}]
  (genc/for [number-of-ids   gen/s-pos-int
             number-of-vectors  gen/s-pos-int
             ids (gen/vector (gen/vector (gen/resize max-id-val gen/int)
                                         (min  max-number-of-ids number-of-ids))
                             (max min-number-of-vectors number-of-vectors))]
    (let [records (mapv id-tuple->datom-map ids)]
      [(:db-after (d/with db records))
       records])))

(defn test-matches
  [db]
  (let [matches (hunt-for-soulmates db)]
    [(reduce (fn [db match] (:db-after (d/with db [match]))) db matches)
     matches]))

;;TODO: profile this and see if it can be speed up somehow
;;TODO: spec out an options map for this so that there aren't magic numbers

(defspec many-soul-on-many-ids* 20
  (propc/for-all
   [db           (gen/return (get-fresh-db 10))
    [db records] (test-datoms-generator db)
    [db matches] (gen/return (test-matches db))]
   (= (derive-characteristics records)
      (characteristics db))))

(defspec many-soul-on-many-ids* 20
  (propc/for-all
   [db           (gen/return (get-fresh-db 10))
    [db records] (test-datoms-generator db)
    [db matches] (gen/return (test-matches db))]
   (= (derive-characteristics records)
      (characteristics db))))
