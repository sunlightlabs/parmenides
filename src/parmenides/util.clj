(ns parmenides.util
  (:require [datomic.api :as d]))

(def uri "datomic:free://127.0.0.1:4334/parmenides")

(defn breakout
  ([vs] (breakout -1 vs))
  ([n vs]
     (->> (group-by first vs)
          (map (fn [[k v]]
                 [k (let [rst (distinct (map rest v))]
                      (if (or (= 1 (count (first rst)))
                              (zero? n))
                        (apply concat rst)
                        (breakout (dec n) rst)))]))
          (into {}))))


(defn prune [m]
  (->> m
       (map
        (fn [[k vs]]
          (if (seq? vs)
            (if (= 1 (count vs))
              [k (first vs)]
              [k vs])
            [k (prune vs)])))
       (into {})))

(prune (breakout
        [[1 1 1]
         [1 2 1]
         [2 1 1]
         [2 3 1]]))



(defn pretty-datom [db datom]
  [(.e datom)
   (:db/ident (d/pull db '[:db/ident] (.a datom)))
   (.v datom)])


;;Got tired of falling back to a map when I needed to multiple things
;;update things the same way in nested data structures. update-in* is
;;like update-in but expanded to allow for iteration over all the
;;elements in a collection. Inspired by the pull api in datomic.

(defn keys* [m]
  (if (map? m) (keys m) (range 0 (count m))))

(defn update-in*
  ([m [k & ks] f & args]
     (cond
      (and (not (nil? ks))  (= k :all))
      (reduce (fn [m k] (assoc m k (apply update-in* (get m k) ks f args))) m (keys* m))

      (and (nil? ks) (= k :all))
      (reduce (fn [m k] (assoc m k (apply f (get m k)  args))) m (keys* m))

      (not (nil? ks))
      (assoc m k (apply update-in* (get m k) ks f args))

      (nil? ks)
      (assoc m k (apply f (get m k) args)))))

(update-in* [1 2] [:all] inc)
;; [2 3]
(update-in* {:a [1 2]} [:a :all] inc)
;; {:a [2 3}
(update-in* {:a [{:id 1} {:id 2}]} [:a :all :id] inc)
;;{:a [{:id 2} {:id 3}]}
