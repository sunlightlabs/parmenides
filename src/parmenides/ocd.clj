(ns parmenides.ocd
  (:require [schema.core :as s]
            [schema-contrib.core :as sc]
            [instaparse.core :as insta]
            [parmenides.util :refer [update-in*]]
            ))

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

(declare Organization Person Event Post Jurisdiction)

(def Image (s/either sc/URI (s/enum "") sc/URI-Reference s/Str))

(def ContactDetail
  {:label s/Str
   :value s/Str
   :type  s/Str
   :note  s/Str})

(def Identifier
  {:identifier s/Str
   :scheme (s/enum "LDA/registrant_house_id" "LDA/registrant_senate_id")})

(def Organization
  {:name s/Str
   :id (Id "organization")

   (s/optional-key :identifiers) [(s/recursive #'Identifier)]
   (s/optional-key :jurisdiction) s/Str,
   (s/optional-key :dissolution_date) (s/either s/Str Date)
   (s/optional-key :parent_id) s/Str,
   (s/optional-key :memberships) []
   (s/optional-key :jurisdiction_id) s/Str,
   (s/optional-key :contact_details) [(s/recursive #'ContactDetail)]
   (s/optional-key :other_names) []
   (s/optional-key :links) []
   (s/optional-key :extras) s/Any

   (s/optional-key :founding_date) (s/either s/Str Date)
   (s/optional-key :image) Image
   (s/optional-key :classification)
   (s/enum "party" "committee" "lower" "upper" "legislature" "executive" "corporation")
   (s/optional-key :jursidiction) (s/maybe (s/recursive #'Jurisdiction))
   (s/optional-key :parent) (s/maybe (s/recursive #'Organization))})

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
  {:name s/Str
   :id (Id "person")
   :memberships [{:post   (s/maybe (s/recursive #'Post))
                  :organization (s/recursive #'Organization)}]
   :contact_details (s/recursive #'ContactDetail)
   (s/optional-key :links) []
   (s/optional-key :identifiers) [(s/recursive #'Identifier)]
   (s/optional-key :image) Image
   (s/optional-key :gender) (s/enum "Male" "Female")
   (s/optional-key :sort_name) s/Str
   (s/optional-key :other_names) []
   (s/optional-key :extras) s/Any})

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
  {:id (Id "disclosure")
   :disclosed_events [(s/recursive #'Event)]
   :identifiers [(s/recursive #'Identifier)]
   :reporting_period s/Str
   :authority s/Str
   :registrant s/Str
   :effective_date Date
   :updated_at (s/maybe s/Str)
   :documents [{:links [s/Str]
                :date Date
                :note s/Str}]
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
             :same_as_registrant s/Bool}}})

(def DisclosureReport
  {:disclosure (s/recursive #'Disclosure)
   :foreign_entities [(s/recursive #'Organization)]
   :lobbyists [(s/recursive #'Person)]
   :affiliated_organizations [(s/recursive #'Organization)]
   :client (s/recursive #'Organization)
   :main_contact (s/recursive #'Person)
   :registrant (s/recursive #'Organization)})

(def OCD
  (letfn [(p [s] #((starts-with? (str "ocd-" s)) (% :id)))]
    (s/conditional
     (p "person") Person
     (p "organization")   Organization
     (p "event")  Event
     (p "disclosure") Disclosure)))

(def OCD? (s/checker [OCD]))

(def files
  ["data/transformed/sopr_html/2009/REG/385c30ad-ceb5-40df-a3bb-a817aae77dc9.json", "data/transformed/sopr_html/2009/REG/2799d7f7-0fca-4eab-84d7-45501a55a5ac.json", "data/transformed/sopr_html/2009/REG/c575cb33-2260-493a-929d-27401a2eb600.json", "data/transformed/sopr_html/2009/REG/63b48082-2b18-4f38-81bb-e4f06ec2a8b2.json", "data/transformed/sopr_html/2009/REG/42fb008b-969c-4b81-baab-87590d25b525.json", "data/transformed/sopr_html/2009/REG/2ef335fc-3c88-4d2d-ba75-557732a4ff7f.json", "data/transformed/sopr_html/2009/REG/018895ac-5ef4-4747-92d6-7d55701370a4.json", "data/transformed/sopr_html/2009/REG/5638b2d4-a608-42e5-a984-ff23f74d54d4.json", "data/transformed/sopr_html/2009/REG/375a37a6-1b85-4a10-855e-a219711c018d.json"])

(->
 (clojure.data.json/read-str (slurp (first files)) :key-fn keyword)
 (dissoc :registrant :registrant_id :authority_id))




#_(do
  (doseq [file files]
    (as-> file $
          (slurp $)
          (clojure.data.json/read-str $ :key-fn keyword)
          (s/validate DisclosureReport $)))
  (println "succeses"))
