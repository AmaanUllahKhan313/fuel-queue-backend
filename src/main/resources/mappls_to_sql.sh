#!/bin/bash
set -e

#######################################
# CONFIG
#######################################
CLIENT_ID="YOUR_CLIENT_ID"
CLIENT_SECRET="YOUR_CLIENT_SECRET"

GEOFENCE_RADIUS="200.0"
REGION="IND"

CITIES=(
  "Aurangabad Maharashtra"
  "Nanded Maharashtra"
  "Latur Maharashtra"
  "Parbhani Maharashtra"
  "Hingoli Maharashtra"
  "Beed Maharashtra"
  "Jalna Maharashtra"
  "Dharashiv Maharashtra"
)

BRANDS=(
  "Hindustan Petroleum"
  "Bharat Petroleum"
)

#######################################
# DEPENDENCIES
#######################################
command -v curl >/dev/null || exit 1
command -v jq >/dev/null || exit 1

#######################################
# GET ACCESS TOKEN
#######################################
ACCESS_TOKEN=$(curl -s -X POST "https://outpost.mappls.com/api/security/oauth/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=client_credentials" \
  -d "client_id=${CLIENT_ID}" \
  -d "client_secret=${CLIENT_SECRET}" | jq -r '.access_token')

[ -z "$ACCESS_TOKEN" ] && exit 1

#######################################
# GENERATE INSERTS
#######################################
for CITY in "${CITIES[@]}"; do
  for BRAND in "${BRANDS[@]}"; do

    curl -s \
      "https://atlas.mappls.com/api/places/textsearch/json?query=${BRAND}%20${CITY}&region=${REGION}" \
      -H "Authorization: Bearer ${ACCESS_TOKEN}" |

    jq -r '
      .suggestedLocations[]? |
      select(.latitude != null and .longitude != null) |
      "INSERT INTO fuel_stations (name, address, latitude, longitude, geofence_radius_meters, active, is_live)
VALUES (''" +
      (.placeName | gsub("''"; "''''")) + "'', ''" +
      (.placeAddress | gsub("''"; "''''")) + "'', " +
      (.latitude|tostring) + ", " +
      (.longitude|tostring) + ", 200.0, TRUE, FALSE);"
    '

    sleep 1
  done
done