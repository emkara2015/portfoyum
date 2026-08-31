import datetime
import re
import time
from concurrent.futures import ThreadPoolExecutor, as_completed

import requests


supabase_url = "https://pxgbiedahlssklfjzwor.supabase.co/rest/v1/tefas_funds"
fintables_catalog_url = "https://api.fintables.com/funds/"
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
    """Return the current catalog when available; never block a normal price sync."""
    try:
        response = requests.get(
            fintables_catalog_url,
            headers={**source_headers, "Accept": "application/json"},
            timeout=20,
        )
        response.raise_for_status()
        payload = response.json()

        if isinstance(payload, dict):
            for key in ("data", "funds", "results"):
                if isinstance(payload.get(key), list):
                    payload = payload[key]
                    break

        if not isinstance(payload, list):
            raise ValueError("catalog response is not a list")

        catalog = {}
        for item in payload:
            if not isinstance(item, dict):
                continue
            code = normalize_symbol(item.get("code") or item.get("symbol"))
            if not code:
                continue
            catalog[code] = {
                "name": (item.get("title") or item.get("name") or code).strip(),
                "fund_type": item.get("type"),
            }

        if not catalog:
            raise ValueError("catalog response did not contain fund codes")
        return catalog
    except Exception as exc:
        print(f"Catalog unavailable; existing codes will be preserved: {exc}")
        return None


def patch_existing(code, payload):
    response = requests.patch(
        f"{supabase_url}?symbol=eq.{code}",
        headers=patch_headers,
        json=payload,
        timeout=8,
    )
    return response.status_code in (200, 204)


def upsert_new(code, payload):
    response = requests.post(
        supabase_url,
        headers={
            **patch_headers,
            "Prefer": "resolution=merge-duplicates,return=minimal",
        },
        json=payload,
        timeout=8,
    )
    return response.status_code in (200, 201, 204)


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
        response = requests.get(
            f"https://fintables.com/fonlar/{code}",
            headers=source_headers,
            timeout=8,
        )
        if response.status_code != 200:
            mark_failure(code, f"fintables_http_{response.status_code}", existing_rows, catalog, timestamp)
            return code, False

        html = response.text

        price_match = re.search(r'\\"price\\":\s*([0-9]+\.[0-9]+)', html)
        price = float(price_match.group(1)) if price_match else 0.0
        if price <= 0.0:
            mark_failure(code, "fintables_price_missing", existing_rows, catalog, timestamp)
            return code, False

        tax_match = re.search(r'\\"tax\\":\s*([0-9\.]+)', html)
        risk_match = re.search(r'\\"risk\\":\s*([0-9]+)', html)
        y1m_match = re.search(r'\\"1m\\":\s*\{[^}]*\\"yield\\":\s*([0-9\.-]+)', html)
        y3m_match = re.search(r'\\"3m\\":\s*\{[^}]*\\"yield\\":\s*([0-9\.-]+)', html)
        y6m_match = re.search(r'\\"6m\\":\s*\{[^}]*\\"yield\\":\s*([0-9\.-]+)', html)
        y1y_match = re.search(r'\\"1y\\":\s*\{[^}]*\\"yield\\":\s*([0-9\.-]+)', html)
        ytd_match = re.search(r'\\"ytd\\":\s*\{[^}]*\\"yield\\":\s*([0-9\.-]+)', html)

        payload = {
            "price": price,
            "tax_percent": float(tax_match.group(1)) * 100 if tax_match else 0.0,
            "risk_level": int(risk_match.group(1)) if risk_match else None,
            "yield_1m": float(y1m_match.group(1)) if y1m_match else None,
            "yield_3m": float(y3m_match.group(1)) if y3m_match else None,
            "yield_6m": float(y6m_match.group(1)) if y6m_match else None,
            "yield_1y": float(y1y_match.group(1)) if y1y_match else None,
            "yield_ytd": float(ytd_match.group(1)) if ytd_match else None,
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
            metadata = catalog.get(code, {}) if catalog else {}
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
symbols = sorted(set(existing_rows) | catalog_codes)
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
