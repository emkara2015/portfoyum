import datetime
import re
import time
from concurrent.futures import ThreadPoolExecutor, as_completed

import requests


supabase_url = "https://pxgbiedahlssklfjzwor.supabase.co/rest/v1/tefas_funds"
anon_key = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InB4Z2JpZWRhaGxzc2tsZmp6d29yIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODQ0NzM5OTQsImV4cCI6MjEwMDA0OTk5NH0.h5_IBFoAT1tFAw29lcWLsF1yC4GfoADSQ8XTSWCK6L0"

patch_headers = {
    "apikey": anon_key,
    "Authorization": f"Bearer {anon_key}",
    "Content-Type": "application/json",
}

source_headers = {
    "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
    "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
}


def now_iso():
    return datetime.datetime.now(datetime.timezone.utc).isoformat()


def normalize_symbol(value):
    if not isinstance(value, str):
        return None
    code = value.strip().upper()
    # Some existing TEFAŞ/ETF-style codes contain a suffix such as .F or .F1.
    # Keep those codes; filtering them out would make an existing row invisible
    # to the synchronizer and could create duplicate catalog records.
    return code if re.fullmatch(r"[A-Z0-9]{2,10}(?:\.[A-Z0-9]{1,4})?", code) else None


def fetch_existing_rows():
    print("1. Fetching existing fund codes from Supabase (with pagination)...")
    rows = {}
    offset = 0
    limit = 1000

    while True:
        headers_range = {**patch_headers, "Range": f"{offset}-{offset + limit - 1}"}
        response = requests.get(
            f"{supabase_url}?select=symbol,catalog_miss_count,consecutive_failures,is_active&order=symbol.asc",
            headers=headers_range,
            timeout=30,
        )
        if response.status_code not in (200, 206):
            response.raise_for_status()

        data = response.json()
        if not data:
            break

        for row in data:
            code = normalize_symbol(row.get("symbol"))
            if code:
                rows[code] = row

        if len(data) < limit:
            break
        offset += limit

    return rows


def fetch_catalog():
    """Fetch today's prices from the official TEFAS bulk API.

    The old Fintables HTML/API endpoints return HTTP 403 from GitHub-hosted
    runners.  TEFAS's redesigned API returns up to 1,000 rows per page and
    supports both investment (YAT) and pension (EMK) funds.
    """
    try:
        from tefasmak import fonlar_gunluk_detay

        turkey_tz = datetime.timezone(datetime.timedelta(hours=3))
        today = datetime.datetime.now(datetime.timezone.utc).astimezone(turkey_tz).strftime("%Y%m%d")
        catalog = {}
        page_size = 1000

        for fund_type in ("YAT", "EMK"):
            start = 1
            while True:
                rows = fonlar_gunluk_detay(
                    fon_tipi=fund_type,
                    bas_tarih=today,
                    bit_tarih=today,
                    bas_sira=start,
                    bit_sira=start + page_size - 1,
                )
                if not rows:
                    break

                for item in rows:
                    code = normalize_symbol(item.get("fonKodu"))
                    try:
                        price = float(item.get("fiyat") or 0)
                    except (TypeError, ValueError):
                        price = 0.0
                    if code and price > 0:
                        catalog[code] = {
                            "name": (item.get("fonUnvan") or code).strip(),
                            "fund_type": fund_type,
                            "price": price,
                        }

                if len(rows) < page_size:
                    break
                start += page_size

        if not catalog:
            raise ValueError(f"TEFAS returned no prices for {today}")
        return catalog
    except Exception as exc:
        print(f"Catalog unavailable; existing codes will be preserved: {exc}")
        return None


def patch_existing(code, payload):
    response = requests.patch(
        f"{supabase_url}?symbol=eq.{code}",
        headers={**patch_headers, "Prefer": "return=representation"},
        json=payload,
        timeout=8,
    )
    if response.status_code not in (200, 204):
        return False
    if not response.content:
        return response.status_code == 204
    try:
        return bool(response.json())
    except ValueError:
        return False


def upsert_new(code, payload):
    response = requests.post(
        supabase_url,
        headers={
            **patch_headers,
            "Prefer": "resolution=merge-duplicates,return=representation",
        },
        json=payload,
        timeout=8,
    )
    if response.status_code not in (200, 201, 204):
        return False
    if not response.content:
        return response.status_code == 204
    try:
        return bool(response.json())
    except ValueError:
        return False


def catalog_fields(code, catalog, timestamp):
    if catalog is None or code not in catalog:
        return {}
    return {
        "is_active": True,
        "last_seen_at": timestamp,
        "catalog_miss_count": 0,
    }


def mark_failure(code, reason, existing_rows, catalog, timestamp):
    row = existing_rows.get(code)
    if row is None:
        return

    previous_failures = int(row.get("consecutive_failures") or 0)
    payload = {
        "last_attempt_at": timestamp,
        "last_sync_status": "failed",
        "last_error": reason[:500],
        "consecutive_failures": previous_failures + 1,
    }
    payload.update(catalog_fields(code, catalog, timestamp))
    try:
        patch_existing(code, payload)
    except Exception:
        pass


def process_fund(code, existing_rows, catalog):
    timestamp = now_iso()
    try:
        metadata = catalog.get(code)
        if not metadata or metadata.get("price", 0) <= 0:
            mark_failure(code, "tefas_price_missing", existing_rows, catalog, timestamp)
            return code, False
        price = float(metadata["price"])

        payload = {
            "price": price,
            "updated_at": timestamp,
            "last_attempt_at": timestamp,
            "last_success_at": timestamp,
            "last_sync_status": "success",
            "last_error": None,
            "consecutive_failures": 0,
        }
        payload.update(catalog_fields(code, catalog, timestamp))

        if code in existing_rows:
            ok = patch_existing(code, payload)
        else:
            new_payload = {
                "symbol": code,
                "name": metadata.get("name", code),
                "fund_type": metadata.get("fund_type"),
                **payload,
            }
            ok = upsert_new(code, new_payload)

        if not ok:
            mark_failure(code, "supabase_write_failed", existing_rows, catalog, timestamp)
        return code, ok
    except Exception as exc:
        mark_failure(code, f"{type(exc).__name__}: {exc}", existing_rows, catalog, timestamp)
        return code, False


def reconcile_catalog(existing_rows, catalog):
    """Mark a fund inactive only after three complete catalog misses."""
    if catalog is None:
        return 0

    missing_codes = set(existing_rows) - set(catalog)
    marked_inactive = 0
    for code in missing_codes:
        row = existing_rows[code]
        miss_count = int(row.get("catalog_miss_count") or 0) + 1
        payload = {"catalog_miss_count": miss_count}
        if miss_count >= 3:
            payload["is_active"] = False
            marked_inactive += 1
        try:
            patch_existing(code, payload)
        except Exception:
            pass
    return marked_inactive


existing_rows = fetch_existing_rows()
catalog = fetch_catalog()
catalog_codes = set(catalog) if catalog is not None else set()
# Only process funds for which TEFAS published a price today. Existing rows
# absent from today's catalog are reconciled below without being counted as
# transient price-fetch failures.
symbols = sorted(catalog_codes)
print(f"Total symbols to process: {len(symbols)}")
if catalog is not None:
    print(f"Catalog symbols discovered: {len(catalog)}")

print("\n2. Processing funds via multi-threading...")
success_count = 0
failed_count = 0
start_time = time.time()

with ThreadPoolExecutor(max_workers=20) as executor:
    futures = [executor.submit(process_fund, code, existing_rows, catalog) for code in symbols]
    for future in as_completed(futures):
        _, ok = future.result()
        if ok:
            success_count += 1
        else:
            failed_count += 1

inactive_count = reconcile_catalog(existing_rows, catalog)
elapsed = time.time() - start_time

print(f"\nFINISHED IN {elapsed:.1f} seconds!")
print(f"Total updated/inserted: {success_count}, Failed/No-data: {failed_count}")
if catalog is not None:
    print(f"Marked inactive after 3 catalog misses: {inactive_count}")

# A green GitHub Actions run must mean that the database was actually updated.
# The next scheduled run will retry transient source/API failures automatically.
if failed_count:
    raise SystemExit(f"TEFAS sync failed for {failed_count} fund(s)")
