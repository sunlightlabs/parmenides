(ns parmenides.util-test
  (:require
   [jordanlewis.data.union-find :refer :all]
   [datomic.api :as d :refer [db q]]
   [parmenides.util :refer :all]
   [parmenides.resolution :refer [attributes dbfn]]))
(alter-var-root (var clojure.pprint/*print-right-margin*) (constantly 140))

;; Taken from simple-expectations
;; https://github.com/jaycfields/simple-expectations/blob/master/test/simple_expectations/core_test.clj
#_(defrecord SimpleCheck []
  CustomPred
  (expect-fn [e a] (:result a))
  (expected-message [e a str-e str-a] (format "%s of %s failures"
                                              (:failing-size a)
                                              (:num-tests a)))
  (actual-message [e a str-e str-a] (format "fail: %s" (:fail a)))
  (message [e a str-e str-a] (format "shrunk: %s" (get-in a [:shrunk :smallest]))))

#_(def checked (->SimpleCheck))

(defn id-attr-and-cupid [id]
  (let [id-id (d/tempid :db.part/db)]
    [{:db/id id-id
      :db/ident (keyword (str "test/id-" id))
      :db/valueType :db.type/long
      :db/cardinality :db.cardinality/one
      :db.install/_attribute :db.part/db}
     [:new-cupid
      {:cupid/attributes [id-id]
       :cupid/transform-combinator (nth (cycle [:identity-combinator :inc-combinator])
                                        id)}]]))

(defn get-fresh-conn [n]
  (let [url (str "datomic:mem://parmenides"  (d/squuid))
        conn (do (d/create-database url)
                 (d/connect url))]
    @(d/transact conn attributes)
    @(d/transact conn (vec (mapcat id-attr-and-cupid (range 1 (inc n)))))
    conn))

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

(defn union-lst [uf lst]
  (let [pairs (partition 2 (interleave lst (rest lst)))]
    (reduce (fn [uf [a b]] (union uf a b))
            uf pairs)))

(defn project-ids [id-tuples]
  (map (partial map-indexed #(hash-map :schema %1 :val %2)) id-tuples))

(defn number-of-souls [id-tuples]
  (let [mapped-tuples (project-ids id-tuples)
        independent-uf (apply union-find (apply concat mapped-tuples))
        merged-uf
        (reduce union-lst independent-uf  mapped-tuples)]
    (count merged-uf)))

(defn number-of-matches [id-tuples]
  (count (distinct (apply concat (project-ids id-tuples)))))

(number-of-matches [[0 0] [0 1]])

(defn derive-characteristics [ids]
  {:number-of-souls (number-of-souls ids)
   :number-of-matches (number-of-matches ids)
   :number-of-records (count ids)})
(count (distinct (project-ids [[0 0]])))

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

(defn id-tuple->datom-map  [lst]
  (as-> lst $
        (map-indexed
         #(vector (keyword (str "test/id-" (inc %1)))
                  %2)
         $)
        (into {} $)
        (assoc $ :db/id (d/tempid :db.part/user)
               :soul/id (str (d/squuid)))))
