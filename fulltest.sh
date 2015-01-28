ls example-data/* | xargs -i curl -H "Content-Type: application/json" -d @{} http://localhost/ingest?save=false
