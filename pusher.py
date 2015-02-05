import requests
import os.path
import json

url="http://127.0.0.1:3000"
headers = {'content-type': 'application/json'}

if not os.path.isfile("creds.txt"):
    registration = {"dataset_name" :  "Zack's Practice Set",
                    "dataset_endpoint" : "localhost:3001"}
    r = requests.post(url+"/register", data=json.dumps(registration), headers=headers)
    print("New registration: "+r.text)
    open("creds.txt","w").write(r.text)

creds = json.loads(open("creds.txt").read())
