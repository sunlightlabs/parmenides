(ns parmenides.ocd
  (:require [schema.core :as s]))

(defn starts-with? [pre]
  (fn [s]
    (if (> (count s) (count pre))
      (= pre (apply str (take (count pre) s)))
      false)))

(defn ocd-id [pre]
  (s/both s/Str (s/pred (starts-with? (str "ocd-" pre "/")) 'ocd-format?)))

(def Vote
  {:bill {:id (ocd-id "bill")
          :identifier s/Str}
   :counts [{:option (s/enum "yes" "no" "other")
             :value  s/Int}]
   :motion_classification [(s/enum "bill-passage" "amendment-passage")]
   :organization {:id (ocd-id "organization")
                  :name s/Str}
   :created_at s/Str
   :updated_at s/Str;;valid date format
   :start_date s/Str
   :result (s/enum "pass" "fail")
   :id (ocd-id "vote")
   :motion_text s/Str
   :extras {s/Keyword s/Any}})

(def Jurisdiction
  {:id (ocd-id "jurisdiction")
   (s/optional-key :name) s/Str})

(def Post
  {:id (ocd-id "post")
   :label s/Str
   :role s/Str})

(def Organization
  {:name s/Str
   :id (ocd-id "organization")

   (s/optional-key :image) s/Str
   (s/optional-key :classification) s/Str
   (s/optional-key :jurisdiction) (s/maybe (s/recursive #'Jurisdiction))
   (s/optional-key :parent) (s/maybe (s/recursive #'Organization))})

(def Person
  {:gender (s/enum "" "Male")
   :id (ocd-id "person")
   :image s/Str
   :memberships [{:organization (s/recursive #'Organization)
                  :post (s/maybe (s/recursive #'Post))}]
   :sort_name s/Str
   :name s/Str})

(def Bill
  {:title s/Str
   :subject [s/Str]
   :identifier s/Str
   :id (ocd-id "bill")
   :from_organization (s/recursive #'Organization)
   :classification s/Any
   (s/optional-key :name) s/Str})

(def Event
  {:description s/Str
   :timezone s/Str
   :name s/Str
   :start_time s/Str
   :classification s/Str
   :end_time (s/maybe s/Str)
   :agenda [{:description s/Str
             :order s/Str
             :related_entities s/Any ;;Is entity an abstract type?
             :subjects []
             }]
   :all_day s/Bool
   :status (s/enum "confirmed")
   :id (ocd-id "event")})

(def OCD
  (letfn [(p [s] #((starts-with? (str "ocd-" s)) (:id %)))]
    (s/conditional
     (p "person") Person
     (p "vote")   Vote
     (p "bill")   Bill
     (p "organization")   Organization
     (p "event")  Event)))
