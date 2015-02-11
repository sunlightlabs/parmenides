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

   (dbfn :identity-combinator [] identity)

   (dbfn :new-cupid [db m]
         (let [id (str (d/squuid))]
           [(merge {:cupid/transform-combinator :identity-combinator
                    :db/id (d/tempid :db.part/user)
                    :cupid/id id}
                   m)

            {:db/id (d/tempid :db.part/db)
             :db/ident (keyword (str "match.cupid-" id "/result")) ;;TODO:
                                                             ;;Copy
                                                             ;;and
                                                             ;;pasted
                                                             ;;everywhere

             :db/valueType :db.type/string
             :db/unique :db.unique/identity
             :db/cardinality :db.cardinality/one
             :db.install/_attribute :db.part/db}]))
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

   ])

(defn run-a-cupid [db {db-id :db/id
                       cupid-id :cupid/id
                       attr :cupid/attributes
                       {fnc :db/fn} :cupid/transform-combinator
                       :as m}]
  ;;Assuming that cupids can only match based on one attribute right now
  (let [f (fnc)
        attr (:db/id (first attr))]
    (mapv #(do {:db/id (d/tempid :db.part/user)
                :match/cupid db-id

               (keyword (str "match.cupid-" cupid-id "/result"))
                (str (f (.v %)))

               :match/valid true
               :match/records (.e %)})
         (d/datoms db :aevt attr))))


(defn hunt-for-soulmates
  ([db] (hunt-for-soulmates
         db
         (d/q '[:find [?cupid ...] :where [?cupid :cupid/id]] db)))
  ([db cupids]
     (mapcat (partial run-a-cupid db)
             (d/pull-many db
                          '[:db/id :cupid/attributes :cupid/id {:cupid/transform-combinator [:db/fn]}]
                          cupids))))
