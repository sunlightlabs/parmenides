(ns parmenides.resolution
  (:require [datomic.api :as d]
            [clojure.pprint :refer [pprint]]))

(defmacro dbfn [name params & code]
  `{:db/id (d/tempid :db.part/user)
    :db/ident ~name
    :db/fn (d/function  {:lang :clojure
                         :requires '[[datomic.api :as d]]
                         :params '~params
                         :code '(do ~@code)})})

(def attributes
  [{:db/id #db/id[:db.part/db]
    :db/ident :soul/id
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/index true
    :db.install/_attribute :db.part/db}

   {:db/id #db/id[:db.part/db]
    :db/ident :cupid/id
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/index true
    :db.install/_attribute :db.part/db}

   {:db/id #db/id[:db.part/db]
    :db/ident :cupid/description
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db.install/_attribute :db.part/db}

   {:db/id #db/id[:db.part/db]
    :db/ident :cupid/attributes
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/many
    :db.install/_attribute :db.part/db}

   {:db/id #db/id[:db.part/db]
    :db/ident :cupid/transform-combinator
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one
    :db.install/_attribute :db.part/db}

   {:db/id #db/id[:db.part/db]
    :db/ident :match/id
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/unique :db.unique/identity
    :db.install/_attribute :db.part/db}

   {:db/id #db/id[:db.part/db]
    :db/ident :match/cupid
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one
    :db.install/_attribute :db.part/db}

   {:db/id #db/id[:db.part/db]
    :db/ident :match/records
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/many
    :db.install/_attribute :db.part/db}

   {:db/id #db/id[:db.part/db]
    :db/ident :match/valid
    :db/valueType :db.type/boolean
    :db/cardinality :db.cardinality/one
    :db.install/_attribute :db.part/db}

   (dbfn :identity-combinator [] identity)
   (dbfn :inc-combinator [] inc)

   (dbfn :cupid-id->keyword [id] (keyword (str "match.cupid-" id "/result")))

   (dbfn :new-cupid [db m]
     (let [id (str (d/squuid))]
       [(merge {:cupid/transform-combinator :identity-combinator
                :db/id (d/tempid :db.part/user)
                :cupid/id id} m)

        {:db/id (d/tempid :db.part/db)
         :db/ident (d/invoke db :cupid-id->keyword id)
         :db/valueType :db.type/string
         :db/unique :db.unique/identity
         :db/cardinality :db.cardinality/one
         :db.install/_attribute :db.part/db}]))

   (dbfn :propagate-plurality-id [db match-key value disallowed-values]
     (let [match (:db/id (d/entity db [match-key value]))
           ids-records (d/q '[:find ?id ?record
                              :in $ % ?match
                              :where
                              (affected-records ?match ?record)
                              [?record :soul/id ?id]]
                            db
                            '[[(affected-records ?match ?record)
                               [?match :match/records ?record]]
                              [(affected-records ?match ?record)
                               [?match :match/records ?in-between]
                               [?new-match :match/records ?in-between]
                               (affected-records ?new-match ?record)]]
                            match)
           filtered-ids-records
           (filter (comp (complement (set disallowed-values)) second)
                      ids-records)
           id-structure (->> filtered-ids-records
                       (group-by first))
           max-id (if (not (empty? id-structure))
                    (key (apply max-key (comp count val) id-structure))
                    (str (d/squuid)))
           entities  (distinct (map second ids-records))]
       (mapv #(vector :db/add % :soul/id max-id) entities)))

   (dbfn :expand-match [db cupid-db-id cupid-id value record]
     (let [match-key (d/invoke db :cupid-id->keyword cupid-id)
           match {:db/id (d/tempid :db.part/user)
                  :match/cupid cupid-db-id
                  :match/id  (str (d/squuid))
                  match-key value
                  :match/valid true
                  :match/records record}
           new-db (:db-after (d/with db [match]))
           data (vec (concat
                      [match]
                      (d/invoke new-db :propagate-plurality-id
                                new-db  match-key value [])))]
       data))

   (dbfn :invalidate-match [db match-id]
     (let []

       [{:db/id (d/tempid :db.part/user)
          :match/id match-id
          :match/valid false}]))])


(defn run-a-cupid [db {cupid-db-id :db/id
                       cupid-id :cupid/id
                       attr :cupid/attributes
                       {fnc :db/fn} :cupid/transform-combinator
                       :as m}]
  ;;Assuming that cupids can only match based on one attribute right now
  (let [f (fnc)
        attr (:db/id (first attr))]
    (for [datom (d/datoms db :aevt attr)]
      [:expand-match cupid-db-id cupid-id (str (f (.v datom))) (.e datom)])))

(defn hunt-for-soulmates
  ([db] (hunt-for-soulmates
         db
         (d/q '[:find [?cupid ...] :where [?cupid :cupid/id]] db)))
  ([db cupids]
     (mapcat (partial run-a-cupid db)
             (d/pull-many db
                          '[:db/id :cupid/attributes :cupid/id {:cupid/transform-combinator [:db/fn]}]
                          cupids))))

(defn unleash-the-cupids!
  [conn]
  (doall
   (map (comp deref #(d/transact conn [%]))
        (hunt-for-soulmates (d/db conn)))))
