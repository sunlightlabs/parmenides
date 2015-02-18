(ns parmenides.util-test
  (:require
   [jordanlewis.data.union-find :refer :all]
   [datomic.api :as d :refer [db q]]
   [parmenides.util :refer :all]
   [parmenides.resolution :refer [attributes dbfn]]
   [clojure.pprint :refer [pprint]]))

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

(defn get-fresh-db
  [n]
  (let [url (str "datomic:mem://parmenides"  (d/squuid))
        conn (do (d/create-database url)
                 (d/connect url))]
    @(d/transact conn attributes)
    @(d/transact conn (vec (mapcat id-attr-and-cupid (range 0 n))))
    (d/db conn)))

(def or-zero #(or % 0))

(defn union-lst
  [uf lst]
  (let [pairs (partition 2 (interleave lst (rest lst)))]
    (reduce (fn [uf [a b]] (union uf a b))
            uf pairs)))

(defn id-tuple->kv
  [id v]
  [(keyword (str "test/id-" id)) v])

(defn project-ids
  [id-tuples]
  (map (partial map-indexed id-tuple->kv) id-tuples))

(defn number-of-souls
  [records removed-matches]
  (let [mapped-tuples  (map (partial filter (complement (set removed-matches)))
                            records)
        independent-uf (apply union-find (apply concat mapped-tuples))
        merged-uf
        (reduce union-lst independent-uf  mapped-tuples)]
    (+ (count merged-uf)
       (count (filter empty? mapped-tuples)))))

(defn number-of-matches
  [records removed-matches]
  (let [number-of-added (count (filter #(= (namespace (first %)) "test") (distinct (apply concat records))))
        number-of-removed (count (distinct removed-matches))]
    {:valid (- number-of-added number-of-removed)
     :invalid number-of-removed}))

(defn derive-characteristics
  ([records] (derive-characteristics records []))
  ([records removed-matches]
     {:number-of-souls (number-of-souls records removed-matches)
      :number-of-matches (number-of-matches records removed-matches)
      :number-of-records (count records)}))

(defn characteristics
  [db]
  {:number-of-souls
   (or-zero (d/q '[:find (count ?soul-id) .
                   :where [_ :soul/id ?soul-id]]
                 db))
   :number-of-matches
   {:valid
    (or-zero (d/q '[:find (count ?match) .
                    :where
                    [?match :match/cupid _]
                    [?match :match/valid true]]
                  db))
    :invalid
    (or-zero (d/q '[:find (count ?match) .
                    :where
                    [?match :match/cupid _]
                    [?match :match/valid false]]
                  db))}

   :number-of-records
   (or-zero (d/q '[:find (count ?record) .
                   :where [?record :soul/id _]]
                 db))})

(defn id-tuples->datom-map
  [lst]
  (assoc (into {} lst)
    :db/id (d/tempid :db.part/user)
    :soul/id (str (d/squuid))))

(def id-tuple->datom-map
  (comp id-tuples->datom-map (partial map-indexed id-tuple->kv )))
