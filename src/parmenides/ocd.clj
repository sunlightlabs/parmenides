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
  {(s/required-key "bill") {(s/required-key "id") (ocd-id "bill")
                            (s/required-key "identifier") s/Str}
   (s/required-key "counts") [{(s/required-key "option") (s/enum "yes" "no" "other")
                               (s/required-key "value")  s/Int}]
   (s/required-key "motion_classification") [(s/enum "bill-passage" "amendment-passage")]
   (s/required-key "organization") {(s/required-key "id") (ocd-id "organization")
                                    (s/required-key "name") s/Str}
   (s/required-key "created_at") s/Str
   (s/required-key "updated_at") s/Str ;;valid date format
   (s/required-key "start_date") s/Str
   (s/required-key "result") (s/enum "pass" "fail")
   (s/required-key "id") (ocd-id "vote")
   (s/required-key "motion_text") s/Str
   (s/required-key "extras") s/Any})

(def Jurisdiction
  {(s/required-key "id") (ocd-id "jurisdiction")
   (s/optional-key "name") s/Str

   (s/optional-key "classification")
   (s/enum "government" "legislature" "executive" "school" "park" "sewer" "forest" "transit")})

(def Post
  {(s/required-key "id") (ocd-id "post")
   (s/required-key "label") s/Str
   (s/required-key "role") s/Str})

(def Organization
  {(s/required-key "name") s/Str
   (s/required-key "id") (ocd-id "organization")

   (s/optional-key "image") s/Str
   (s/optional-key "classification") s/Str
   (s/optional-key "jurisdiction") (s/maybe (s/recursive #'Jurisdiction))
   (s/optional-key "parent") (s/maybe (s/recursive #'Organization))})

(def Person
  {(s/required-key "gender") (s/enum "" "Male" "Female")
   (s/required-key "id") (ocd-id "person")
   (s/required-key "image") s/Str
   (s/required-key "memberships") [{(s/required-key "organization") (s/recursive #'Organization)
                                    (s/required-key "post") (s/maybe (s/recursive #'Post))}]
   (s/required-key "sort_name") s/Str
   (s/required-key "name") s/Str})

(def Bill
  {(s/required-key "title") s/Str
   (s/required-key "subject") [s/Str]
   (s/required-key "identifier") s/Str
   (s/required-key "id") (ocd-id "bill")
   (s/required-key "from_organization") (s/recursive #'Organization)
   (s/required-key "classification") s/Any
   (s/optional-key "name") s/Str})

(def Event
  {(s/required-key "description") s/Str
   (s/required-key "timezone") s/Str
   (s/required-key "name") s/Str
   (s/required-key "start_time") s/Str

   (s/required-key "classification")
   (s/enum "committee-meeting" "hearings" "floor_time" "redistricting" "special"
           ;;There are lots of old style classification that is held
           ;;on by some invalid db's. Waiting for the ocd databases to
           ;;correct itself and then will remove some old enums.
           "senate:session" "house:session" "joint:session"
           "committee:meeting" "committee meeting"
           "committee:hearing" )
   (s/required-key "end_time") (s/maybe s/Str)
   (s/required-key "agenda") [{(s/required-key "description") s/Str
                               (s/required-key "order") s/Str
                               (s/required-key "related_entities") s/Any ;;Is entity an abstract type?
                               (s/required-key "subjects") []}]
   (s/required-key "all_day") s/Bool
   (s/required-key "status") (s/enum "confirmed")
   (s/required-key "id") (ocd-id "event")
   (s/optional-key "extras") s/Any})

(def OCD
  (letfn [(p [s] #((starts-with? (str "ocd-" s)) (% "id")))]
    (s/conditional
     (p "person") Person
     (p "vote")   Vote
     (p "bill")   Bill
     (p "organization")   Organization
     (p "event")  Event)))
