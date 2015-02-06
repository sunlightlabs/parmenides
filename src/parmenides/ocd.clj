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

   (s/optional-key :image) Image
   (s/optional-key :classification)
   (s/enum "party" "committee" "lower" "upper" "legislature" "executive" "corporation")
   (s/optional-key :jursidiction) (s/maybe (s/recursive #'Jurisdiction))
   (s/optional-key :parent) (s/maybe (s/recursive #'Organization))})

(def Bill
  {:id (Id "bill")
   :identifier s/Str

   (s/optional-key :name) s/Str
   (s/optional-key :title) s/Str
   (s/optional-key :subject) [s/Str]
   (s/optional-key :from_organization) (s/recursive #'Organization)
   (s/optional-key :classification)
   [(s/enum "bill" "resolution" "concurrent resolution" "joint resolution"
            "memorial" "proposed bill" "contract" "appropriation"
            "constitutional amendment" "nomination" "appointment" "commemoration"
            "joint memorial")]})

(def Vote
  {:id (Id :vote)

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

   (s/optional-key :name) s/Str

   (s/optional-key :classification)
   (s/enum "government" "legislature" "executive" "school" "park" "sewer" "forest" "transit")})

(def Post
  {:id (Id "post")
   :start_date Date
   :role s/Str
   (s/optional-key :label) s/Str})


(def Person
  {:gender (s/enum "" "Male" "Female")
   :id (Id "person")
   :image Image
   :memberships [{(s/required-key "organization") (s/recursive #'Organization)
                                    (s/required-key "post") (s/maybe (s/recursive #'Post))}]
   :sort_name s/Str
   :name s/Str})

(def Event
  {:id (Id "event")
   :name s/Str
   :start_time Date

   :description s/Str
   :timezone s/Str ;;todo check for timezones?
   :participants [{:entity_type s/Str
                   :name s/Str
                   :id s/Str ;; This should be change to `(Id "")`
                             ;; after people get the proper identifiers
                   :note s/Str}]
   :links (s/either s/Str [])
   :documents [{:links (s/either s/Str [])
                :date Date
                :note s/Str}]
   :location (s/maybe s/Str)
   :media (s/maybe s/Num)

   :classification s/Str
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
   :status (s/enum "confirmed" "")

   (s/optional-key :extras) s/Any})

(def Disclosure
  {:disclosure
   {:disclosed_events [(s/recursive #'Event)]
    :identifiers [{}]
    :reporting_period s/Str
    :authority s/Str
    :registrant s/Str
    :effective_date Date
    :updated_at (s/maybe s/Str)
    :documents [{:links [s/Str]
                 :date Date
                 :note s/Str}]
    :id (Id "disclosure")
    :registrant_id s/Str
    :authority_id s/Str
    :related_entities [{}]
    :created_at Date
    :extras {:registration_type
             {:is_amendment s/Bool
              :new_registrant s/Bool
              :new_client_for_existing_registrant s/Bool}
             :registrant
             {:general_description s/Str
              :self_employed_individual s/Bool
              :signature
              {:signature s/Str
               :signature_date Date}}
             :client
             {:general_description s/Str
              :same_as_registrant s/Bool}}}
   :foreign_entities s/Any
   :registrant s/Any
   :lobbyists s/Any
   :affiliated_organizations s/Any
   :client s/Any
   :main_contact
   {:id s/Str
    :name s/Str
    (s/optional-key :identifiers) [s/Str]
    (s/optional-key :jurisdiction) s/Any
    (s/optional-key :dissolution_date) s/Str
    (s/optional-key :classification) (s/enum "")
    (s/optional-key :parent_id) s/Str
    (s/optional-key :memberships)
    [{:post (s/recursive #'Post), :organization (s/recursive #'Organization)}],
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

(do
  (as-> "data/transformed/sopr_html/2009/REG/385c30ad-ceb5-40df-a3bb-a817aae77dc9.json" $
        (slurp $)
        (clojure.data.json/read-str $ :key-fn keyword)
        (s/validate Disclosure $))
  (println "success")
  )
