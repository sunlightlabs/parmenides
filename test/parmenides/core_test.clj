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
   [parmenides.resolution :refer [unleash-the-cupids!]]))

(defspec one-soul-on-one-id 10
  (prop/for-all
   [id-1 gen/int
    n (gen/such-that (complement zero?) gen/pos-int)]
   (let [conn (get-fresh-conn 1)]
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
   [numbers (gen/vector (gen/tuple gen/int gen/s-pos-int))]
   (let [conn (get-fresh-conn 1)]
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

(defspec many-soul-on-two-ids 40
  (prop/for-all
   [ids (gen/vector (gen/tuple gen/int gen/int))]
                                        ;(println ids)
   (let [conn (get-fresh-conn 2)
         outcome (derive-characteristics ids)]
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

(defspec many-soul-on-many-ids 20
  (prop/for-all
   [ids (gen/not-empty (gen/vector (gen/not-empty (gen/bind gen/pos-int
                                                             (partial gen/vector gen/int)))))]
   (let [max-number-of-ids (apply max (map count ids))
         conn (get-fresh-conn max-number-of-ids)
         outcome (derive-characteristics ids)]
     @(d/transact conn
                  (mapv (fn [lst]
                          (as-> lst $
                                (map-indexed
                                 #(vector (keyword (str "test/id-" (inc %1)))
                                          %2)
                                 $)
                                (into {} $)
                                (assoc $ :db/id (d/tempid :db.part/user)
                                       :soul/id (str (d/squuid)))))
                        ids))
     (unleash-the-cupids! conn)
     (= outcome
        (characteristics (d/db conn))))))
