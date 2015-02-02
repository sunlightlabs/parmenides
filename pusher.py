import requests
import os.path

url="http://ec2-54-174-143-132.compute-1.amazonaws.com/"

if not os.path.isfile("apikey.txt"):
    data = {"email" :  "zmaril@sunlightfoundation.com",
            "return-address" : "nowhere"}
    r = requests.post(url+"register", params=data)
    print(r)
    open("apikey.txt","w").write(r.apikey).close()

apikey = open("apikey.txt").read()
