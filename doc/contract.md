Entity resolution is a hard problem. If you follow these rules then maybe parmenides will solve it for you.

# Receivers & Registration

The first steps of interacting with a parmenides server is setting up a
receiver and registering your data source. Here is an example of
setting up a simple receiver and registering it with parmenides. 

``` bash
sed -i /
    -e "s/RETURN_URL/my-public-return-url" /
    -e "s/DATASET_NAME/my-super-awesome-dataset-name" /
    -e "s/MAINTAINER_NAME/my-name" /
    -e "s/MAINTAINER_EMAIL/my-email" /
    simple_registration.json
python simple_receiver.py &
curl -X POST -d @simple_registration.json http://parmenides-url:3000/register -o apikey.json 
```

