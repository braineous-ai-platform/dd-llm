curl -v  -X POST http://localhost:8080/api/v1/query \
  -H "Content-Type: application/json" \
  -d '{
    "adapter": "openai",
    "queryKind": "validate_flight_airports",
    "query": "Validate that the selected flight has valid departure and arrival airport codes based on the airport nodes in the graph.",
    "fact": "Airport:AUS",
    "relatedFacts": ["Airport:DFW", "Airport:SFO"]
  }'
