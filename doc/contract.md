Entity resolution is a hard problem. If you follow these rules then maybe parmenides will solve it for you.

# Receivers & Registration

The first steps of interacting with a parmenides server is setting up
a receiver and registering your data source. Using the example files
found in doc, here is a simple example of starting up a receiver and
registering the receiver with parmenides.

``` bash
sed -i /
    -e "s/RETURN_URL/my-public-return-url" /
    -e "s/DATASET_DESCRIPTION/my super awesome dataset" /
    -e "s/MAINTAINER_NAME/my-name" /
    -e "s/MAINTAINER_EMAIL/my-email" /
    simple_registration.json
python simple_receiver.py &
curl -X POST -d @simple_registration.json http://parmenides-url:3000/register -o apikey.json 
```

A goal of parmenides is to
[integrate data](http://en.wikipedia.org/wiki/Data_integration) from
multiple sources. In the pursuit of this goal, parmenides has been
designed so that inferences about resolutions based off of one data
source can flow out to every other data source. The registration and
receivers decouples the ingestion of data from the resolution of that
data. If a better method is found which improves resolution
results, parmenides can immediately push the changes out to all the
data sources.

To register, one submits a POST request to "/register" with the following information:

* "dataset_description": A string describing the data set, its content and origins.
* "dataset_return_endpoint": A string for a url where parmenides can return results of resolutions.
* "dataset_maintainer_email": A string for an email where the maintainer of a data source can be contacted. 
* "dataset_maintainer_name": A string for the name of the maintainer. 

Once a registration has been submitted to parmenides, parmenides will
first check to ensure that there is in fact a receiver at
`dataset_return_endpoint`. It will submit a get request to
`dataset_return_endpoint+"/status"` and expect a JSON response of
`{"status":"listening"}`. If all this happens, then the registration
will be successful and an API key will be returned. This key must be
saved, as it will be used during the ingestion process to verify and
indicate which dataset new sources of data belong to.

(Tongue in check: should maintainers have to fill out an ocd style
specification for themselves before being able to register?)

# Open Civic Data and Parmenides

In academic terms, parmenides use a global premeditated schema as well
as materialization to prepare for entity resolution. All that this
jargon means is that there is a format that all incoming data must
conform to and that all data parmenides sees will be saved
forever. This means that any data you want to have resolved must be
mapped to the
[Open Civic Data Format](http://opencivicdata.org/). Open Civic Data
(OCD) provides solid standards for modeling people, organizations and
everything else you might want to perform entity resolution on. To
start resolving data, submit a POST request to parmenides at "/ingest"
in the format `{"apikey":"apikey-from-registration","data":[]}`, with
data being an array full of OCD objects. Parmenides will validate the
data and make sure it conform's to the OCD specification. It will try
and return reasons if any piece of data doesn't conform. If the data
conforms and is loaded successfully, then
`{"transaction":"sucessful"}` will be returned as a response.

There is only one extension to the standard OCD format that is
required by parmenides . This is the tag id. Every OCD object, even
those objects within nested data, must be tagged with an identifier
called "tag_id". Per OCD format, the tag id will be in the
`identifiers` array. The tag id for a piece of data should never
change and be totally unique to the object, i.e. no two objects should
ever have the same tag id unless they are the same. We recommend using
a UUID generator.

If you can convert your data into OCD format and can guarantee the
properties of tag id's, then parmenides might be able to help you with
entity resolution. By manipulating the main `ocd-id`, parmenides can
tell you which pieces of data represent the same thing in the real
world. It does this by submitting POST requests to the receiver of the
form
`{"resolutions":[["tag_id_1","ocd_id_1"],["tag_id_2","ocd_id_1"]]}`. The
`resolutions` array is a list of lists, where each inner list is a
pair of identifiers. The above example indicates that the pieces of
data with the respective tag ids should have the same OCD ids.

# Verified Data Sources

[TO DISCUSS]
