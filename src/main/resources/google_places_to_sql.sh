#!/bin/bash
set -e

#####################################
# CONFIG
#####################################
API_KEY="YOUR_GOOGLE_API_KEY"
GEOFENCE_RADIUS=200.0
SEARCH_RADIUS=50000   # 50 km

# Maharashtra grid points (lat,lng)
# Covers entire MH safely
POINTS=(
  "19.7515,75.7139"   # Central MH
  "18.5204,73.8567"   # Pune
  "19.9975,73.7898"   # Nashik
  "21.1458,79.0882"   # Nagpur
  "19.0760,72.8777"   # Mumbai
  "20.9042,74.7749"   # Jalgaon
  "16.7050,74.2433"   # Kolhapur
  "18.4088,76.5604"   # Latur
  "19.1500,77.3333"   # Nanded
  "17.6599,75.9064"   # Solapur
  "20.3893,78.1300"   # Wardha
)

KEYWORDS=(
  "Hindustan Petroleum petrol pump"
  "Bharat Petroleum petrol pump"
)

#####################################
# FUNCTIONS
#####################################
fetch_places () {
  local LOCATION="$1"
  local KEYWORD="$2"
  local NEXT_PAGE=""

  while true; do
    if [ -z "$NEXT_PAGE" ]; then
      RESP=$(curl -s \
        "https://maps.googleapis.com/maps/api/place/nearbysearch/json?location=${LOCATION}&radius=${SEARCH_RADIUS}&keyword=${KEYWORD}&key=${API_KEY}")
    else
      sleep 2
      RESP=$(curl -s \
        "https://maps.googleapis.com/maps/api/place/nearbysearch/json?pagetoken=${NEXT_PAGE}&key=${API_KEY}")
    fi

    echo "$RESP" | jq -r '
      .results[]? |
      "INSERT INTO fuel_stations (name, address, latitude, longitude, geofence_radius_meters, active, is_live)
VALUES (" +
      "'"'"'" + (.name | gsub("'"'"'"; "''")) + "'"'"', " +
      "'"'"'" + (.vicinity | gsub("'"'"'"; "''")) + "'"'"', " +
      (.geometry.location.lat|tostring) + ", " +
      (.geometry.location.lng|tostring) + ", '"$GEOFENCE_RADIUS"', TRUE, FALSE);"
    '

    NEXT_PAGE=$(echo "$RESP" | jq -r '.next_page_token // empty')
    [ -z "$NEXT_PAGE" ] && break
  done
}

#####################################
# MAIN
#####################################
for POINT in "${POINTS[@]}"; do
  for KEYWORD in "${KEYWORDS[@]}"; do
    fetch_places "$POINT" "$KEYWORD"
  done
done