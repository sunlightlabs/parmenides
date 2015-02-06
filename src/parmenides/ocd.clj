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

(defn no-empty [arg]
  (println arg)
  (cond
   (map? arg) (into {} (for [[k v] arg] (when-not (empty? v) [k (no-empty v)])))
   (coll? arg) (vec (map no-empty arg))
   (and (or (string? arg) (coll? arg)) (empty? arg)) nil
   :default arg
   ))

(no-empty {:t []
           :tz ["abc" "def"]
           :v [{:a "" :b "c"}]
           :a ""
           :b "z"
           :c {:d "" :e "z"}
           })

(do
  (doseq [file files]
    (as-> file $
          (slurp $)
          (clojure.data.json/read-str $ :key-fn keyword)
          (no-empty $)
          (s/validate DisclosureReport $)))
  (println "succeses"))

(def example
  {:disclosure
   {:disclosed_events
    [{:description "",
      :timezone "America/New_York",
      :participants
      [{:note "registrant",
        :id "ocd-organization/3f4c1ddc-ab0c-11e4-ac4a-22000b5182c6",
        :name "Evertz Group LLC",
        :entity_type "organization"}
       {:note "client",
        :id "ocd-organization/3f4c294e-ab0c-11e4-ac4a-22000b5182c6",
        :name "Gilead Sciences",
        :entity_type "organization"}
       {:note "lobbyist",
        :id "person/3f4c2d9a-ab0c-11e4-ac4a-22000b5182c6",
        :name "Scott Evertz ",
        :entity_type "person"}
       {:note "lobbyist",
        :id "person/3f4c33f8-ab0c-11e4-ac4a-22000b5182c6",
        :name "Scott Evertz ",
        :entity_type "person"}],
      :name "New Client, New Registrant",
      :start_time "2009-07-15T00:00:00Z",
      :classification "registration",
      :end_time nil,
      :agenda [],
      :documents
      [{:note "submitted filing",
        :date "2009-07-15T00:00:00Z",
        :links []}],
      :all_day false,
      :status "",
      :id "ocd-event/3f4c3aa6-ab0c-11e4-ac4a-22000b5182c6",
      :location nil,
      :media nil,
      :links ""}],
    :identifiers [],
    :reporting_period "",
    :authority "Office of Public Record, US Senate",
    :registrant "ocd-organization/3f4c1ddc-ab0c-11e4-ac4a-22000b5182c6",
    :effective_date "2009-07-15T00:00:00Z",
    :updated_at nil,
    :documents
    [{:note "submitted filing",
      :date "2009-07-15T00:00:00Z",
      :links []}],
    :id "ocd-disclosure/385c30ad-ceb5-40df-a3bb-a817aae77dc9",
    :registrant_id "",
    :authority_id
    "ocd-organization/13dfdd58-a8b6-11e4-b6bf-3c970e91567b",
    :related_entities [],
    :created_at "2015-02-02T18:49:41.786602",
    :extras
    {:client
     {:general_description
      "Discovers, develops and commercializes therapeutics in areas of unmet medical need.",
      :same_as_registrant false},
     :registrant
     {:general_description "Government Relations",
      :self_employed_individual false,
      :signature
      {:signature "Digitally Signed By: Scott Evertz, Managing Member",
       :signature_date "2009-11-04T00:00:00Z"}},
     :registration_type
     {:is_amendment false,
      :new_registrant true,
      :new_client_for_existing_registrant false}}},
   :affiliated_organizations [],
   :client
   {:founding_date "",
    :identifiers [],
    :jurisdiction "",
    :name "Gilead Sciences",
    :dissolution_date "",
    :classification "",
    :parent_id "",
    :id "ocd-organization/3f4c294e-ab0c-11e4-ac4a-22000b5182c6",
    :memberships [],
    :image "",
    :jurisdiction_id "",
    :contact_details
    [[{:note "Gilead Sciences",
       :type "address",
       :value "333 Lakeside Drive; Foster City; CA; 94404; USA",
       :label "contact address"}
      {:note "Gilead Sciences",
       :type "address",
       :value "",
       :label "principal place of business"}]],
    :other_names [],
    :links [],
    :extras
    {:contact_details_structured
     [{:note "client contact on SOPR LD-1",
       :parts
       [{:value "333 Lakeside Drive", :label "address"}
        {:value "Foster City", :label "city"}
        {:value "CA", :label "state"}
        {:value "94404", :label "zip"}
        {:value "USA", :label "country"}],
       :type "address",
       :label "contact address"}
      {:note "client contact on SOPR LD-1",
       :parts
       [{:value "", :label "city"}
        {:value "", :label "state"}
        {:value "", :label "zip"}
        {:value "", :label "country"}],
       :type "address",
       :label "principal place of business"}]}},
   :main_contact
   {:founding_date "",
    :identifiers [],
    :jurisdiction "",
    :name "Mr. Scott Evertz",
    :dissolution_date "",
    :classification "",
    :parent_id "",
    :id "ocd-person/3f4c22b4-ab0c-11e4-ac4a-22000b5182c6",
    :memberships
    [{:organization
      {:name "Evertz Group LLC",
       :classification "corporation",
       :id "ocd-organization/3f4c1ddc-ab0c-11e4-ac4a-22000b5182c6"},
      :post
      {:role "main_contact",
       :id "ocd-post/3f4c25ac-ab0c-11e4-ac4a-22000b5182c6",
       :start_date "2009-07-15T00:00:00Z"}}],
    :image "",
    :jurisdiction_id "",
    :contact_details
    [{:note "Evertz Group LLC",
      :type "phone",
      :value "2024783020",
      :label "contact phone"}
     {:note "Evertz Group LLC",
      :type "email",
      :value "scott@evertzgroup.com",
      :label "contact email"}],
    :other_names [],
    :links [],
    :extras {:contact_details_structured []}},
   :lobbyists
   [{:death_date "",
     :identifiers [],
     :name "Scott Evertz ",
     :national_identity "",
     :summary "",
     :birth_date "",
     :id "person/3f4c2d9a-ab0c-11e4-ac4a-22000b5182c6",
     :memberships
     [{:organization
       {:name "Evertz Group LLC",
        :classification "corporation",
        :id "ocd-organization/3f4c1ddc-ab0c-11e4-ac4a-22000b5182c6"},
       :post
       {:role "lobbyist",
        :id "ocd-post/3f4c3074-ab0c-11e4-ac4a-22000b5182c6",
        :start_date "2009-07-15T00:00:00Z"}}],
     :image "",
     :gender "",
     :biography "",
     :contact_details [],
     :other_names [],
     :links [],
     :extras
     {:lda_covered_official_positions
      [{:disclosure_id
        "ocd-disclosure/385c30ad-ceb5-40df-a3bb-a817aae77dc9",
        :covered_official_position
        "Director of the Office of National AIDS Policy",
        :date_reported "2009-07-15T00:00:00Z"}]}}
    {:death_date "",
     :identifiers [],
     :name "Scott Evertz ",
     :national_identity "",
     :summary "",
     :birth_date "",
     :id "person/3f4c33f8-ab0c-11e4-ac4a-22000b5182c6",
     :memberships
     [{:organization
       {:name "Evertz Group LLC",
        :classification "corporation",
        :id "ocd-organization/3f4c1ddc-ab0c-11e4-ac4a-22000b5182c6"},
       :post
       {:role "lobbyist",
        :id "ocd-post/3f4c36b4-ab0c-11e4-ac4a-22000b5182c6",
        :start_date "2009-07-15T00:00:00Z"}}],
     :image "",
     :gender "",
     :biography "",
     :contact_details [],
     :other_names [],
     :links [],
     :extras
     {:lda_covered_official_positions
      [{:disclosure_id
        "ocd-disclosure/385c30ad-ceb5-40df-a3bb-a817aae77dc9",
        :covered_official_position
        "Spec. Asst. to the Sec. of HHS for Global AIDS Initiatives",
        :date_reported "2009-07-15T00:00:00Z"}]}}],
   :registrant
   {:founding_date "",
    :identifiers
    [{:scheme "LDA/registrant_house_id", :identifier ""}
     {:scheme "LDA/registrant_senate_id", :identifier "400497446"}],
    :jurisdiction "",
    :name "Evertz Group LLC",
    :dissolution_date "",
    :classification "corporation",
    :parent_id "",
    :id "ocd-organization/3f4c1ddc-ab0c-11e4-ac4a-22000b5182c6",
    :memberships [],
    :image "",
    :jurisdiction_id "",
    :contact_details
    [{:note "Mr. Scott Evertz",
      :type "address",
      :value "2029 K St. NW; 7th Floor; WASHINGTON; DC; 20006; USA",
      :label "contact address"}
     {:note "Mr. Scott Evertz",
      :type "address",
      :value "",
      :label "principal place of business"}
     {:note "Mr. Scott Evertz",
      :type "phone",
      :value "2024783020",
      :label "contact phone"}
     {:note "Mr. Scott Evertz",
      :type "email",
      :value "scott@evertzgroup.com",
      :label "contact email"}],
    :other_names [],
    :links [],
    :extras
    {:contact_details_structured
     [{:note "registrant contact on SOPR LD-1",
       :parts
       [{:value "2029 K St. NW", :label "address_one"}
        {:value "7th Floor", :label "address_two"}
        {:value "WASHINGTON", :label "city"}
        {:value "DC", :label "state"}
        {:value "20006", :label "zip"}
        {:value "USA", :label "country"}],
       :type "address",
       :label "contact address"}
      {:note "registrant contact on SOPR LD-1",
       :parts
       [{:value "", :label "city"}
        {:value "", :label "state"}
        {:value "", :label "zip"}
        {:value "", :label "country"}],
       :type "address",
       :label "principal place of business"}]}},
   :foreign_entities []})
