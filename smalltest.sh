ls example-data/* | head -n 1 | xargs -i curl -H "Content-Type: application/json" -d @{} http://localhost/ingest?save=false
