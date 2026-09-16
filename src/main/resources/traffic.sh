#!/bin/bash

URL="http://localhost:8080/api/gps/ping"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DATA_SQL="$SCRIPT_DIR/data.sql"
EXTRACT_SCRIPT="$SCRIPT_DIR/extract_stations.py"

if [[ ! -f "$DATA_SQL" ]]; then
  echo "Unable to find data.sql at $DATA_SQL" >&2
  exit 1
fi

if [[ ! -f "$EXTRACT_SCRIPT" ]]; then
  echo "Unable to find extract_stations.py at $EXTRACT_SCRIPT" >&2
  exit 1
fi

# Load station coordinates using Python script
mapfile -t stations < <(python3 "$EXTRACT_SCRIPT" "$DATA_SQL")

if [[ ${#stations[@]} -eq 0 ]]; then
  echo "No station coordinates found" >&2
  exit 1
fi

CARS_PER_STATION=21
SLEEP_INTERVAL=5

TOTAL_VEHICLES=$(( ${#stations[@]} * CARS_PER_STATION ))

echo "Starting simulation with $TOTAL_VEHICLES vehicles across ${#stations[@]} nearby stations..."

while true
do
  user_id=1

  for coords in "${stations[@]}"
  do
    base_lat=${coords% *}
    base_lng=${coords#* }

    for ((car=1; car<=CARS_PER_STATION; car++))
    do
      # Add GPS noise (~±50m)
      lat=$(awk -v base=$base_lat 'BEGIN{srand(); print base + (rand()-0.5)/1000}')
      lng=$(awk -v base=$base_lng 'BEGIN{srand(); print base + (rand()-0.5)/1000}')

      # Random speed (0–5 km/h → queue-like)
      speed=$(awk 'BEGIN{srand(); print rand()*5}')

      timestamp=$(date +%s)

      curl -s -X POST "$URL" \
        -H "Content-Type: application/json" \
        -d "{\"userId\":$user_id,\"latitude\":$lat,\"longitude\":$lng,\"speedKmh\":$speed,\"timestamp\":$timestamp}" &

      user_id=$((user_id + 1))
    done

  done

  wait
  echo "Batch sent at $(date)"

  sleep $SLEEP_INTERVAL
done
