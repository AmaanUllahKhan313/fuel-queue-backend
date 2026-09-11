import re
import csv
import time
import json
from pathlib import Path
from urllib.parse import quote

from playwright.sync_api import sync_playwright, TimeoutError as PlaywrightTimeoutError


# ============================================================
# CONFIGURATION
# ============================================================

OUTPUT_SQL = "cng_pumps_maharashtra.sql"
OUTPUT_CSV = "cng_pumps_maharashtra.csv"
OUTPUT_JSON = "cng_pumps_maharashtra.json"

GEOfENCE_RADIUS = 200.0

MAX_SCROLLS = 20
SCROLL_WAIT = 1.5
PAGE_WAIT = 2.0

HEADLESS = False

# Set to None to process everything.
# Example:
# DISTRICT_LIMIT = 3
DISTRICT_LIMIT = None


# ============================================================
# MAHARASHTRA DISTRICTS
# ============================================================

DISTRICTS = [
    "Ahmednagar",
    "Akola",
    "Amravati",
    "Aurangabad",
    "Beed",
    "Bhandara",
    "Buldhana",
    "Chandrapur",
    "Dhule",
    "Gadchiroli",
    "Gondia",
    "Hingoli",
    "Jalgaon",
    "Jalna",
    "Kolhapur",
    "Latur",
    "Mumbai City",
    "Mumbai Suburban",
    "Nagpur",
    "Nanded",
    "Nandurbar",
    "Nashik",
    "Osmanabad",
    "Palghar",
    "Parbhani",
    "Pune",
    "Raigad",
    "Ratnagiri",
    "Sangli",
    "Satara",
    "Sindhudurg",
    "Solapur",
    "Thane",
    "Wardha",
    "Washim",
    "Yavatmal",
]


# Alternate/current names used by Google Maps
DISTRICT_ALIASES = {
    "Ahmednagar": [
        "Ahmednagar",
        "Ahilyanagar",
    ],
    "Aurangabad": [
        "Aurangabad",
        "Chhatrapati Sambhajinagar",
    ],
    "Osmanabad": [
        "Osmanabad",
        "Dharashiv",
    ],
}


# ============================================================
# HELPERS
# ============================================================

def clean_text(value):
    if not value:
        return ""

    value = re.sub(r"\s+", " ", value)
    return value.strip()


def sql_escape(value):
    return str(value).replace("'", "''")


def extract_coordinates(url):
    """
    Extract coordinates from common Google Maps URL formats.

    Examples:

        !3d18.5204!4d73.8567

        /@18.5204,73.8567,15z
    """

    if not url:
        return None, None

    # Format:
    # !3dLAT!4dLON
    match = re.search(
        r"!3d(-?\d+(?:\.\d+)?)!4d(-?\d+(?:\.\d+)?)",
        url
    )

    if match:
        return float(match.group(1)), float(match.group(2))

    # Format:
    # /@LAT,LON
    match = re.search(
        r"/@(-?\d+(?:\.\d+)?),(-?\d+(?:\.\d+)?)",
        url
    )

    if match:
        return float(match.group(1)), float(match.group(2))

    # Generic @LAT,LON fallback
    match = re.search(
        r"@(-?\d+(?:\.\d+)?),(-?\d+(?:\.\d+)?)",
        url
    )

    if match:
        return float(match.group(1)), float(match.group(2))

    return None, None


def is_valid_maharashtra_coordinate(latitude, longitude):
    """
    Rough Maharashtra bounding box.

    This is only a sanity check, not an official boundary.
    """

    if latitude is None or longitude is None:
        return False

    return (
        15.5 <= latitude <= 22.1
        and
        72.5 <= longitude <= 80.9
    )


# ============================================================
# GOOGLE MAPS CONSENT
# ============================================================

def handle_consent(page):

    buttons = [
        "Accept all",
        "I agree",
        "Reject all",
    ]

    for text in buttons:

        try:
            button = page.get_by_role(
                "button",
                name=re.compile(
                    rf"^{re.escape(text)}$",
                    re.IGNORECASE
                )
            )

            if button.count() > 0 and button.first.is_visible():
                print(f"  Clicking consent button: {text}")

                button.first.click(
                    timeout=3000
                )

                time.sleep(1)

                return

        except Exception:
            pass


# ============================================================
# SEARCH BOX
# ============================================================

def find_search_box(page):

    selectors = [
        'input[aria-label="Search Google Maps"]',
        'input[placeholder="Search Google Maps"]',
        'input[aria-label*="Search"]',
        'input[placeholder*="Search"]',
        'input[type="text"]',
    ]

    for selector in selectors:

        try:
            locator = page.locator(selector)

            if locator.count() > 0:

                for i in range(min(locator.count(), 5)):

                    candidate = locator.nth(i)

                    try:
                        if candidate.is_visible():
                            return candidate
                    except Exception:
                        pass

        except Exception:
            pass

    return None


# ============================================================
# SEARCH GOOGLE MAPS
# ============================================================

def search_google_maps(page, query):

    print()
    print("=" * 80)
    print(f"SEARCH: {query}")
    print("=" * 80)

    search_box = find_search_box(page)

    if search_box:

        try:
            search_box.fill(query)
            search_box.press("Enter")

            time.sleep(PAGE_WAIT)

            handle_consent(page)

            return

        except Exception as e:
            print(f"  Search-box method failed: {e}")

    # Fallback: direct Google Maps search URL
    encoded_query = quote(query)

    url = (
        "https://www.google.com/maps/search/"
        + encoded_query
    )

    print("  Using direct Maps search URL")

    page.goto(
        url,
        wait_until="domcontentloaded",
        timeout=30000
    )

    time.sleep(PAGE_WAIT)

    handle_consent(page)


# ============================================================
# SCROLL SEARCH RESULTS
# ============================================================

def scroll_results(page):

    print("  Scrolling search results...")

    feed_selectors = [
        'div[role="feed"]',
        'div.m6QErb[aria-label]',
        'div[aria-label*="Results"]',
    ]

    feed = None

    for selector in feed_selectors:

        try:
            locator = page.locator(selector)

            if locator.count() > 0:

                for i in range(min(locator.count(), 5)):

                    candidate = locator.nth(i)

                    if candidate.is_visible():
                        feed = candidate
                        break

                if feed:
                    break

        except Exception:
            pass

    for i in range(MAX_SCROLLS):

        try:

            if feed:
                feed.evaluate(
                    "(element) => element.scrollTop = element.scrollHeight"
                )
            else:
                page.mouse.wheel(0, 5000)

        except Exception:
            page.mouse.wheel(0, 5000)

        time.sleep(SCROLL_WAIT)

        print(
            f"    scroll {i + 1}/{MAX_SCROLLS}",
            end="\r"
        )

    print()


# ============================================================
# GET SEARCH RESULT LINKS
# ============================================================

def get_result_links(page):

    links = set()

    selectors = [
        'a[href*="/maps/place/"]',
        'a[href*="google.com/maps/place"]',
    ]

    for selector in selectors:

        try:

            anchors = page.locator(selector)

            count = anchors.count()

            for i in range(count):

                try:

                    href = anchors.nth(i).get_attribute("href")

                    if not href:
                        continue

                    if "/maps/place/" not in href:
                        continue

                    links.add(href)

                except Exception:
                    continue

        except Exception:
            continue

    print(f"  Found {len(links)} unique place links")

    return list(links)


# ============================================================
# EXTRACT STATION DETAILS
# ============================================================

def get_station_details(page, url, district, query):

    try:

        page.goto(
            url,
            wait_until="domcontentloaded",
            timeout=30000
        )

        time.sleep(PAGE_WAIT)

    except Exception as e:

        print(f"    Could not open place: {e}")

        return None

    try:

        handle_consent(page)

        current_url = page.url

        # ----------------------------------------------------
        # NAME
        # ----------------------------------------------------

        name = ""

        selectors = [
            "h1.DUwDvf",
            "h1.fontHeadlineLarge",
            "h1",
        ]

        for selector in selectors:

            try:

                locator = page.locator(selector)

                if locator.count() > 0:

                    for i in range(min(locator.count(), 3)):

                        text = clean_text(
                            locator.nth(i).inner_text()
                        )

                        if text:

                            name = text
                            break

                if name:
                    break

            except Exception:
                pass

        # ----------------------------------------------------
        # ADDRESS
        # ----------------------------------------------------

        address = ""

        address_selectors = [
            'button[data-item-id="address"]',
            'div[data-item-id="address"]',
            '[data-item-id="address"]',
        ]

        for selector in address_selectors:

            try:

                locator = page.locator(selector)

                if locator.count() > 0:

                    text = clean_text(
                        locator.first.inner_text()
                    )

                    if text:
                        address = text
                        break

            except Exception:
                pass

        # ----------------------------------------------------
        # COORDINATES
        # ----------------------------------------------------

        latitude, longitude = extract_coordinates(
            current_url
        )

        if latitude is None or longitude is None:

            print(
                f"    Coordinates not found: {name}"
            )

            return None

        # ----------------------------------------------------
        # SANITY CHECK
        # ----------------------------------------------------

        if not is_valid_maharashtra_coordinate(
            latitude,
            longitude
        ):

            print(
                f"    Outside Maharashtra bounds: "
                f"{name} ({latitude}, {longitude})"
            )

            return None

        station = {
            "name": name,
            "address": address,
            "latitude": latitude,
            "longitude": longitude,
            "district": district,
            "source_query": query,
            "maps_url": current_url,
        }

        print(
            f"    ✓ {name} | "
            f"{latitude}, {longitude}"
        )

        return station

    except Exception as e:

        print(
            f"    Error extracting station: {e}"
        )

        return None


# ============================================================
# DEDUPLICATION
# ============================================================

def deduplicate_stations(stations):

    unique = {}

    for station in stations:

        latitude = station.get("latitude")
        longitude = station.get("longitude")

        if latitude is None or longitude is None:
            continue

        # 6 decimal places gives very fine coordinate precision
        key = (
            round(latitude, 6),
            round(longitude, 6),
        )

        if key not in unique:

            unique[key] = station

        else:

            existing = unique[key]

            # Prefer the record with an address
            if (
                not existing.get("address")
                and station.get("address")
            ):
                unique[key] = station

    result = list(unique.values())

    result.sort(
        key=lambda x: (
            x.get("district", ""),
            x.get("name", ""),
        )
    )

    return result


# ============================================================
# GENERATE SQL
# ============================================================

def generate_sql(stations):

    lines = []

    lines.append(
        "INSERT INTO fuel_stations "
        "(name, address, latitude, longitude, "
        "geofence_radius_meters, active, is_live) VALUES"
    )

    values = []

    for station in stations:

        name = sql_escape(
            station.get("name", "CNG Station")
        )

        address = sql_escape(
            station.get("address", "")
        )

        latitude = station["latitude"]
        longitude = station["longitude"]

        values.append(
            "    "
            f"('{name}', "
            f"'{address}', "
            f"{latitude:.7f}, "
            f"{longitude:.7f}, "
            f"{GEOfENCE_RADIUS:.1f}, "
            "TRUE, "
            "FALSE)"
        )

    if not values:
        return "-- No stations found"

    lines.append(
        ",\n".join(values)
        + ";"
    )

    return "\n".join(lines)


# ============================================================
# WRITE CSV
# ============================================================

def write_csv(stations):

    fields = [
        "name",
        "address",
        "latitude",
        "longitude",
        "district",
        "source_query",
        "maps_url",
    ]

    with open(
        OUTPUT_CSV,
        "w",
        newline="",
        encoding="utf-8"
    ) as file:

        writer = csv.DictWriter(
            file,
            fieldnames=fields
        )

        writer.writeheader()

        for station in stations:
            writer.writerow(station)


# ============================================================
# WRITE JSON
# ============================================================

def write_json(stations):

    with open(
        OUTPUT_JSON,
        "w",
        encoding="utf-8"
    ) as file:

        json.dump(
            stations,
            file,
            indent=2,
            ensure_ascii=False
        )


# ============================================================
# MAIN
# ============================================================

def main():

    all_stations = []

    with sync_playwright() as p:

        browser = p.chromium.launch(
            headless=HEADLESS
        )

        context = browser.new_context(
            viewport={
                "width": 1440,
                "height": 1000,
            },
            locale="en-IN",
        )

        page = context.new_page()

        # ----------------------------------------------------
        # INITIAL GOOGLE MAPS PAGE
        # ----------------------------------------------------

        page.goto(
            "https://www.google.com/maps",
            wait_until="domcontentloaded",
            timeout=30000
        )

        time.sleep(PAGE_WAIT)

        handle_consent(page)

        # ----------------------------------------------------
        # DISTRICT LOOP
        # ----------------------------------------------------

        districts = DISTRICTS

        if DISTRICT_LIMIT:
            districts = districts[:DISTRICT_LIMIT]

        total_districts = len(districts)

        for district_index, district in enumerate(
            districts,
            start=1
        ):

            print()
            print("#" * 90)
            print(
                f"DISTRICT {district_index}/{total_districts}: "
                f"{district}"
            )
            print("#" * 90)

            aliases = DISTRICT_ALIASES.get(
                district,
                [district]
            )

            queries = []

            # Search each name using both terminology variants
            for alias in aliases:

                queries.append(
                    f"CNG pump {alias}, Maharashtra"
                )

                queries.append(
                    f"CNG station {alias}, Maharashtra"
                )

            # Remove duplicate queries
            queries = list(dict.fromkeys(queries))

            district_links = set()

            # ------------------------------------------------
            # SEARCH
            # ------------------------------------------------

            for query in queries:

                try:

                    search_google_maps(
                        page,
                        query
                    )

                    scroll_results(page)

                    links = get_result_links(page)

                    for link in links:
                        district_links.add(link)

                except Exception as e:

                    print(
                        f"  Search failed: {query}"
                    )

                    print(
                        f"  Error: {e}"
                    )

            print()
            print(
                f"  Total unique links for {district}: "
                f"{len(district_links)}"
            )

            # ------------------------------------------------
            # OPEN EACH STATION
            # ------------------------------------------------

            for station_index, url in enumerate(
                district_links,
                start=1
            ):

                print(
                    f"\n  Station "
                    f"{station_index}/{len(district_links)}"
                )

                station = get_station_details(
                    page=page,
                    url=url,
                    district=district,
                    query="Maharashtra CNG"
                )

                if station:

                    all_stations.append(
                        station
                    )

        # ----------------------------------------------------
        # CLOSE BROWSER
        # ----------------------------------------------------

        browser.close()

    # ========================================================
    # DEDUPLICATE
    # ========================================================

    print()
    print("=" * 90)
    print("DEDUPLICATING")
    print("=" * 90)

    before = len(all_stations)

    stations = deduplicate_stations(
        all_stations
    )

    after = len(stations)

    print(f"Before deduplication : {before}")
    print(f"After deduplication  : {after}")
    print(f"Duplicates removed   : {before - after}")

    # ========================================================
    # WRITE SQL
    # ========================================================

    sql = generate_sql(stations)

    Path(OUTPUT_SQL).write_text(
        sql,
        encoding="utf-8"
    )

    # ========================================================
    # WRITE CSV
    # ========================================================

    write_csv(stations)

    # ========================================================
    # WRITE JSON
    # ========================================================

    write_json(stations)

    # ========================================================
    # SUMMARY
    # ========================================================

    print()
    print("=" * 90)
    print("COMPLETED")
    print("=" * 90)

    print(
        f"Total Maharashtra stations: {len(stations)}"
    )

    print(
        f"SQL file  : {OUTPUT_SQL}"
    )

    print(
        f"CSV file  : {OUTPUT_CSV}"
    )

    print(
        f"JSON file : {OUTPUT_JSON}"
    )

    print()
    print("District-wise counts:")

    district_counts = {}

    for station in stations:

        district = station.get(
            "district",
            "Unknown"
        )

        district_counts[district] = (
            district_counts.get(district, 0) + 1
        )

    for district in sorted(district_counts):

        print(
            f"  {district:<25} "
            f"{district_counts[district]}"
        )


if __name__ == "__main__":
    main()