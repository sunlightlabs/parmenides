(ns parmenides.ocd
  (:require [schema.core :as s]
            [schema-contrib.core :as sc]
            [instaparse.core :as insta]))

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
                             ({:tag :string, :string " "}
                              {:tag :nt, :keyword :time})}})})
      (assoc :start-production :ocd-underspecified)))

(def date? (comp not insta/failure? date-parser))

(def Date
  (s/either sc/ISO-Date-Time (s/pred date? 'Date)))

(declare Organization Bill Vote Person Event Post Jurisdiction)

(def Image (s/either sc/URI (s/enum "") sc/URI-Reference s/Str))

(def Organization
  {:name s/Str
   :id (Id "organization")

   (s/optional-key "image") Image
   (s/optional-key "classification")
   (s/enum "party" "committee" "lower" "upper" "legislature" "executive")
   (s/optional-key "jurisdiction") (s/maybe (s/recursive #'Jurisdiction))
   (s/optional-key "parent") (s/maybe (s/recursive #'Organization))})

(def Bill
  {:id (Id "bill")
   :identifier s/Str

   (s/optional-key "name") s/Str
   (s/optional-key "title") s/Str
   (s/optional-key "subject") [s/Str]
   (s/optional-key "from_organization") (s/recursive #'Organization)
   (s/optional-key "classification")
   [(s/enum "bill" "resolution" "concurrent resolution" "joint resolution"
            "memorial" "proposed bill" "contract" "appropriation"
            "constitutional amendment" "nomination" "appointment" "commemoration"
            "joint memorial")]})

(def Vote
  {:id (Id "vote")

   :created_at Date
   :updated_at Date
   :start_date Date

   :bill (s/recursive #'Bill)

   :counts
   [{:option (s/enum "yes" "no" "other")
     :value  s/Int}]

   :motion_classification [(s/enum "bill-passage" "amendment-passage")]
   :organization (s/recursive #'Organization)

   :result (s/enum "pass" "fail")

   :motion_text s/Str
   :extras s/Any})

(def Jurisdiction
  {:id (Id "jurisdiction")

   (s/optional-key "name") s/Str

   (s/optional-key "classification")
   (s/enum "government" "legislature" "executive" "school" "park" "sewer" "forest" "transit")})

(def Post
  {:id (Id "post")
   :label s/Str
   :role s/Str})


(def Person
  {:gender (s/enum "" "Male" "Female")
   :id (Id "person")
   :image Image
   :memberships [{(s/required-key "organization") (s/recursive #'Organization)
                                    (s/required-key "post") (s/maybe (s/recursive #'Post))}]
   :sort_name s/Str
   :name s/Str})

(def Event
  {:description s/Str
   :timezone s/Str ;;todo check for timezones?
   :name s/Str
   :start_time Date

   :classification
   (s/enum "committee-meeting" "hearings" "floor_time" "redistricting" "special"
           ;;There are lots of old style classification that is held
           ;;on by some invalid db's. Waiting for the ocd databases to
           ;;correct itself and then will remove some old enums.
           "senate:session" "house:session" "joint:session"
           "committee:meeting" "committee meeting"
           "committee:hearing" )
   :end_time (s/maybe s/Str)
   :agenda [{(s/required-key "description") s/Str
                               (s/required-key "order") s/Str
                               (s/required-key "related_entities")
                               [{(s/required-key "note") (s/maybe s/Str)
                                 (s/required-key "entity_type")
                                 (s/enum "bill" "organization" "person")
                                 (s/required-key "entity_id") (s/maybe (Id "")) ;;Hack
                                 (s/required-key "entity_name") s/Str}]
                               (s/required-key "subjects") []}]
   :all_day s/Bool
   :status (s/enum "confirmed")
   :id (Id "event")
   :extras s/Any})

(def Disclosure
  {:disclosure s/Any
   :foreign_entities s/Any
   :registrant s/Any
   :lobbyists s/Any
   :affiliated_organizations s/Any
   :client s/Any
   :main_contact
   {:id s/Str
    :name s/Str

    {:role "lobbyist", :id "ocd-post/3f4c36b4-ab0c-11e4-ac4a-22000b5182c6", :start_date "2009-07-15T00:00:00Z"}
    (s/optional-key :identifiers) [s/Str]
    (s/optional-key :jurisdiction) s/Any
    (s/optional-key :dissolution_date) s/Str
    (s/optional-key :classification) (s/enum "")
    (s/optional-key :parent_id) s/Str
    (s/optional-key :memberships)
    [{:post s/Str, :organization (s/recursive #'Organization)}],
    (s/optional-key :sort_name) s/Str
    (s/optional-key :gender) s/Str
    (s/optional-key :jurisdiction_id) s/Any
    (s/optional-key :contact_details) s/Any
    (s/optional-key :other_names) s/Any
    (s/optional-key :links) s/Any
    (s/optional-key :extras) s/Any
    (s/optional-key :founding_date) s/Str
    (s/optional-key :image) s/Str
    }

   })

(def OCD
  (letfn [(p [s] #((starts-with? (str "ocd-" s)) (% :id)))]
    (s/conditional
     (p "person") Person
     (p "vote")   Vote
     (p "bill")   Bill
     (p "organization")   Organization
     (p "event")  Event
     (p "disclosure") Disclosure)))

(def OCD? (s/checker [OCD]))
