#!/bin/bash

URL="https://fuel-queue-backend-production.up.railway.app/api/gps/ping"

# All stations (lat lng)
stations=(
"18.5756 73.8057"
"18.5601 73.8075"
"18.5862 73.8323"
"18.6298 73.7997"
"18.6513 73.7705"
"18.5974 73.7620"
"18.6182 73.7455"
"18.6265 73.7398"
"18.5912 73.7389"
"18.5590 73.7868"
"18.4875 73.8077"
"18.5004 73.8167"
"18.4516 73.8585"
"18.4574 73.8238"
"18.4698 73.8182"
"18.5793 74.0150"
"18.5519 73.9476"
"18.5089 73.9260"
"18.5325 73.9271"
"18.5167 73.9250"
"18.5018 73.8636"
"18.4766 73.8735"
"18.4762 73.8900"
"18.5204 73.8567"
"18.6293 73.8417"
"18.6738 73.8500"
"18.7600 73.8600"
"18.7350 73.6750"
"18.5308 73.8475"
"18.5525 73.8790"
"18.5665 73.8793"
)

TOTAL_VEHICLES=100   # change load here
SLEEP_INTERVAL=5     # seconds between batches

echo "Starting simulation with $TOTAL_VEHICLES vehicles..."

while true
do
  for ((i=1; i<=TOTAL_VEHICLES; i++))
  do
    # Pick random station
    idx=$((RANDOM % ${#stations[@]}))
    coords=(${stations[$idx]})

    base_lat=${coords[0]}
    base_lng=${coords[1]}

    # Add GPS noise (~±50m)
    lat=$(awk -v base=$base_lat 'BEGIN{srand(); print base + (rand()-0.5)/1000}')
    lng=$(awk -v base=$base_lng 'BEGIN{srand(); print base + (rand()-0.5)/1000}')

    # Random speed (0–5 km/h → queue-like)
    speed=$(awk 'BEGIN{srand(); print rand()*5}')

    timestamp=$(date +%s)

    curl -s -X POST "$URL" \
      -H "Content-Type: application/json" \
      -d "{\"userId\":$i,\"latitude\":$lat,\"longitude\":$lng,\"speedKmh\":$speed,\"timestamp\":$timestamp}" &

  done

  wait
  echo "Batch sent at $(date)"

  sleep $SLEEP_INTERVAL
done