(ns parmenides.ocd
  (:require [schema.core :as s]
            [schema-contrib.core :as sc]
            [instaparse.core :as insta]
            [instaparse.combinators :refer :all])
  (:import [instaparse.core Parser]))

(defn starts-with?
  "A combinator that takes a string and returns a function which
  checks whether it's argument starts with the first string."
  [pre]
  (fn [s]
    (if (> (count s) (count pre))
      (= pre (apply str (take (count pre) s)))
      false)))

(defn Id
  "A combinator which takes a string and returns a schema that checks
  if a string is in the right format to be an ocd identifier."
  [pre]
  (s/both s/Str (s/pred (starts-with? (str "ocd-" pre "/")) 'ocd-format?)))

;;Have to hotpatch the schema-contrib date parser to accept ocd style
;;underrepresented dates.
(def date-parser
  (-> sc/date-parser
      (assoc-in [:grammar :ocd-underspecified]
                {:red {:reduction-type :hiccup, :key :ocd-underspecified},
                 :tag :cat,
                 :parsers
                 '({:tag :nt, :keyword :date}
                   {:tag :opt,
                    :parser {:tag :cat, :parsers
                             ({:tag :string, :string " "} {:tag :nt, :keyword :time})}})})
      (assoc :start-production :ocd-underspecified)))

(def date? (comp not insta/failure? date-parser))

(def Date
  (s/either sc/ISO-Date-Time (s/pred date? 'Date)))

(declare Organization Bill Vote Person Event Post Jurisdiction)

(def Organization
  {(s/required-key "name") s/Str
   (s/required-key "id") (Id "organization")

   (s/optional-key "image") s/Str
   (s/optional-key "classification") s/Str
   (s/optional-key "jurisdiction") (s/maybe (s/recursive #'Jurisdiction))
   (s/optional-key "parent") (s/maybe (s/recursive #'Organization))})

(def Bill
  {(s/required-key "id") (Id "bill")
   (s/required-key "identifier") s/Str

   (s/optional-key "name") s/Str
   (s/optional-key "title") s/Str
   (s/optional-key "subject") [s/Str]
   (s/optional-key "from_organization") (s/recursive #'Organization)
   (s/optional-key "classification")
   [(s/enum "bill" "resolution" "concurrent resolution" "joint resolution"
            "memorial" "proposed bill" "contract" "appropriation"
            "constitutional amendment" "nomination" "appointment" "commemoration")]})

(def Vote
  {(s/required-key "id") (Id "vote")

   (s/required-key "created_at") Date
   (s/required-key "updated_at") Date
   (s/required-key "start_date") Date

   (s/required-key "bill") (s/recursive #'Bill)

   (s/required-key "counts")
   [{(s/required-key "option") (s/enum "yes" "no" "other")
     (s/required-key "value")  s/Int}]

   (s/required-key "motion_classification") [(s/enum "bill-passage" "amendment-passage")]
   (s/required-key "organization") (s/recursive #'Organization)

   (s/required-key "result") (s/enum "pass" "fail")

   (s/required-key "motion_text") s/Str
   (s/required-key "extras") s/Any})

(def Jurisdiction
  {(s/required-key "id") (Id "jurisdiction")

   (s/optional-key "name") s/Str
   (s/optional-key "classification")
   (s/enum "government" "legislature" "executive" "school" "park" "sewer" "forest" "transit")})

(def Post
  {(s/required-key "id") (Id "post")
   (s/required-key "label") s/Str
   (s/required-key "role") s/Str})


(def Person
  {(s/required-key "gender") (s/enum "" "Male" "Female")
   (s/required-key "id") (Id "person")
   (s/required-key "image") s/Str
   (s/required-key "memberships") [{(s/required-key "organization") (s/recursive #'Organization)
                                    (s/required-key "post") (s/maybe (s/recursive #'Post))}]
   (s/required-key "sort_name") s/Str
   (s/required-key "name") s/Str})

(def Event
  {(s/required-key "description") s/Str
   (s/required-key "timezone") s/Str
   (s/required-key "name") s/Str
   (s/required-key "start_time")  Date

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
   (s/required-key "id") (Id "event")
   (s/optional-key "extras") s/Any})

(def OCD
  (letfn [(p [s] #((starts-with? (str "ocd-" s)) (% "id")))]
    (s/conditional
     (p "person") Person
     (p "vote")   Vote
     (p "bill")   Bill
     (p "organization")   Organization
     (p "event")  Event)))
