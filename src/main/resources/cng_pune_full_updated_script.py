import requests
import csv

OUTPUT_CSV = "cng_pune_osm.csv"
OUTPUT_SQL = "cng_pune_osm.sql"

# Pune bounding box
bbox = "18.35,73.65,18.80,74.10"

OVERPASS_URL = "https://overpass-api.de/api/interpreter"
OVERPASS_URLS = [
    "https://overpass-api.de/api/interpreter",
    "https://overpass.kumi.systems/api/interpreter",
    "https://lz4.overpass-api.de/api/interpreter"
]

query = f"""
[out:json][timeout:25];
(
  node["amenity"="fuel"]({bbox});
  way["amenity"="fuel"]({bbox});
  relation["amenity"="fuel"]({bbox});
);
out center;
"""

import time
import requests

def fetch_data():
    for url in OVERPASS_URLS:
        print(f"Trying server: {url}")

        try:
            response = requests.get(
                url,
                params={"data": query},
                headers={"User-Agent": "fuel-queue-app/1.0"},
                timeout=60
            )

            if response.status_code != 200:
                print("Failed:", response.status_code)
                continue

            data = response.json()
            elements = data.get("elements", [])

            if elements:
                print(f"✅ Success from {url} | Elements: {len(elements)}")
                return elements

        except Exception as e:
            print(f"❌ Error with {url}:", e)

        time.sleep(2)

    print("🚨 All Overpass servers failed")
    return []

def process_data(elements):
    stations = []

    for el in elements:
        tags = el.get("tags", {})

        name = tags.get("name", "")
        fuel_types = str(tags)

        # 🔥 Filter CNG manually
        if "cng" not in fuel_types.lower() and "gas" not in fuel_types.lower():
            continue

        addr = tags.get("addr:full") or tags.get("addr:street") or "Pune"

        lat = el.get("lat") or el.get("center", {}).get("lat")
        lng = el.get("lon") or el.get("center", {}).get("lon")

        if not lat or not lng:
            continue

        is_live = True if "MNGL" in name.upper() else False
        radius = 300.0 if "HIGHWAY" in addr.upper() else 200.0

        stations.append({
            "name": name if name else "CNG Station",
            "address": addr,
            "lat": lat,
            "lng": lng,
            "radius": radius,
            "is_live": is_live
        })

    return stations

def write_csv(data):
    with open(OUTPUT_CSV, "w", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(
            f,
            fieldnames=["name", "address", "lat", "lng", "geofence_radius_meters", "active", "is_live"]
        )
        writer.writeheader()

        for d in data:
            writer.writerow({
                "name": d["name"],
                "address": d["address"],
                "lat": d["lat"],
                "lng": d["lng"],
                "geofence_radius_meters": d["radius"],
                "active": True,
                "is_live": d["is_live"]
            })


def write_sql(data):
    with open(OUTPUT_SQL, "w", encoding="utf-8") as f:
        f.write("INSERT INTO fuel_stations (name, address, latitude, longitude, geofence_radius_meters, active, is_live)\n")
        f.write("VALUES\n")

        values = []
        for d in data:
            name = d["name"].replace("'", "''")
            addr = d["address"].replace("'", "''")
            is_live = "TRUE" if d["is_live"] else "FALSE"

            values.append(
                f"  ('{name}', '{addr}', {d['lat']}, {d['lng']}, {d['radius']}, TRUE, {is_live})"
            )

        f.write(",\n".join(values))
        f.write(";\n")


def main():
    print("Fetching CNG stations from OpenStreetMap...")
    elements = fetch_data()

    print(f"Raw elements: {len(elements)}")

    data = process_data(elements)

    print(f"Processed stations: {len(data)}")

    write_csv(data)
    write_sql(data)

    print("✅ DONE: CSV + SQL generated")


if __name__ == "__main__":
    main()