(def parmenides-attributes
  [{:db/ident :parmenides.record/type
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one
    :db.install/_attribute :db.part/db
    :db/id #db/id[:db.part/db]}

   {:db/ident :parmenides.record/represents
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one
    :db.install/_attribute :db.part/db
    :db/id #db/id[:db.part/db]}

   {:db/ident :parmenides.type/being :db/id #db/id[:db.part/user]}

   {:db/ident :parmenides.being/tenuously_connected
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one
    :db.install/_attribute :db.part/db
    :db/id #db/id[:db.part/db]}

   {:db/ident :parmenides.being/id
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db.install/_attribute :db.part/db
    :db/id #db/id[:db.part/db]}

   {:db/ident :parmenides/individual :db/id #db/id[:db.part/user]}

   {:db/ident :parmenides.attributes/forward-ownership
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one
    :db/doc
    "The number of entities that can share ownership of the value of
    this attribute. The first option is individual ownership, like a
    government assigned identifier. The second option is shared
    ownership, like a home address or phone number for a family. The
    last option is nobody, in that nobody can own a value, i.e. a type
    of organization, a color or particular make of a car."
    :db.install/_attribute :db.part/db
    :db/id #db/id[:db.part/db]}

   {:db/ident :parmenides.attributes/reverse-ownership
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one
    :db/doc
    "When the attribute is a reference to another entity, reverse
    ownership indicates the number of child entities that can share
    ownership of the parent entity. The first option is individual
    ownership, like in the instance where a child entity is the sole
    owner of the parent entity which is a sole proprietorship. The
    second option is shared ownership, like the child entities are
    employees of the parent entity which is a large organization. The
    last option, the default option, is nobody, in that none of the
    children can own the parent. Unsure whether this is actually
    needed or not but seems potenially useful."
    :db.install/_attribute :db.part/db
    :db/id #db/id[:db.part/db]}

   {:db/ident :parmenides.ownership/individual :db/id #db/id[:db.part/user]}
   {:db/ident :parmenides.ownership/shared :db/id #db/id[:db.part/user]}
   {:db/ident :parmenides.ownership/nobody :db/id #db/id[:db.part/user]}

   {:db/ident :parmenides.attributes/multiplicity
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one
    :db/doc "How many different values should an entity own for this
    attribute? The first option is one and the second option is
    many. A person should only have one social security but might have
    many different driver's license numbers."
    :db.install/_attribute :db.part/db
    :db/id #db/id[:db.part/db]}

   {:db/ident :parmenides.multiplicity/one :db/id #db/id[:db.part/user]}
   {:db/ident :parmenides.multiplicity/many :db/id #db/id[:db.part/user]}

   {:db/ident :parmenides.attributes/reliable
    :db/valueType :db.type/boolean
    :db/cardinality :db.cardinality/one
    :db/doc
    "Is the value of this attribute reliably entered? Pessimistically,
    the default is false."
    :db.install/_attribute :db.part/db
    :db/id #db/id[:db.part/db]}

;;; Built in attributes that are handled as special cases
   {:db/ident :parmenides.person/name
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db.install/_attribute :db.part/db
    :db/id #db/id[:db.part/db]}

   {:db/ident :parmenides.organization/name
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db.install/_attribute :db.part/db
    :db/id #db/id[:db.part/db]}])
