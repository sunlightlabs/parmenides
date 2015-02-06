import requests
import os.path
import json
import glob

url="http://127.0.0.1:3000"
headers = {'content-type': 'application/json'}

if not os.path.isfile("creds.txt"):
    registration = {"dataset_name" :  "Zack's Practice Set",
                    "dataset_endpoint" : "http://localhost:3001"}
    r = requests.post(url+"/register", data=json.dumps(registration), headers=headers)
    print("New registration: "+r.text)
    open("creds.txt","w").write(r.text)

creds = json.loads(open("creds.txt").read())
for f in glob.glob("data/transformed/sopr_html/*/REG/*")[1:10]:
    morsel = {"dataset_id" : creds["dataset_id"],
              "dataset_apikey" : creds["dataset_apikey"],
              "datum": json.loads(open(f).read())}
    r = requests.post(url+"/ingest", data=json.dumps(morsel), headers=headers)
    if r.status_code != 200:
        print(r.text)
