(ns parmenides.util-test
  (:require
   [jordanlewis.data.union-find :refer :all]
   [datomic.api :as d :refer [db q]]
   [parmenides.util :refer :all]
   [parmenides.resolution :refer [attributes dbfn]]))

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

(defn get-fresh-conn
  [n]
  (let [url (str "datomic:mem://parmenides"  (d/squuid))
        conn (do (d/create-database url)
                 (d/connect url))]
    @(d/transact conn attributes)
    @(d/transact conn (vec (mapcat id-attr-and-cupid (range 0 n))))
    conn))

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
  [id-tuples]
  (let [mapped-tuples (project-ids id-tuples)
        independent-uf (apply union-find (apply concat mapped-tuples))
        merged-uf
        (reduce union-lst independent-uf  mapped-tuples)]
    (count merged-uf)))

(defn number-of-matches
  [id-tuples]
  (count (distinct (apply concat (project-ids id-tuples)))))

(defn derive-characteristics
  [ids]
  {:number-of-souls (number-of-souls ids)
   :number-of-matches (number-of-matches ids)
   :number-of-records (count ids)})

(defn characteristics
  [db]
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

(defn id-tuples->datom-map
  [lst]
  (assoc (into {} lst)
    :db/id (d/tempid :db.part/user)
    :soul/id (str (d/squuid))))
