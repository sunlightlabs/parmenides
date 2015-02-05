;; (ns parmenides.test)

;; (def parser "A='a'")

;; (defn slice  [db e v]
;;   parser)

;; (defn dice [db e v]
;;    (slice parser 1 2))


;; ;;;

;; args

;; {:db/id #db/id [:db.part/user]
;;  :db/ident :inc
;;  :db/namespace
;;  :db/doc "Data function that increments value of attribute a by amount."
;;  :db/fn #db/fn {:lang "clojure"
;;                 :params [db e a amount]
;;                 :code [[:db/add e a
;;                         (-> (d/entity db e) a (+ amount))]]}}

;; {:db/id #db/id [:db.part/user]
;;  :db/ident :inc
;;  :db/doc "Data function that increments value of attribute a by amount."
;;  :db/fn #db/fn {:lang "clojure"
;;                 :params [db e a amount]
;;                 :code [[:db/add e a
;;                         (-> (d/entity db e) a (+ amount))]]}}
