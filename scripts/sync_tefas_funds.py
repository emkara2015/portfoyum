import requests
import re
import json
import time
from concurrent.futures import ThreadPoolExecutor, as_completed

supabase_url = "https://pxgbiedahlssklfjzwor.supabase.co/rest/v1/tefas_funds"
anon_key = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InB4Z2JpZWRhaGxzc2tsZmp6d29yIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODQ0NzM5OTQsImV4cCI6MjEwMDA0OTk5NH0.h5_IBFoAT1tFAw29lcWLsF1yC4GfoADSQ8XTSWCK6L0"

patch_headers = {
    "apikey": anon_key,
    "Authorization": f"Bearer {anon_key}",
    "Content-Type": "application/json"
}

headers = {
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
    'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8'
}

print("1. Fetching all fund codes from Supabase...")
r = requests.get(f"{supabase_url}?select=symbol&limit=3000", headers=patch_headers)
if r.status_code != 200:
    print("Failed to fetch symbols:", r.status_code)
    exit(1)

symbols = [item['symbol'] for item in r.json() if item.get('symbol')]
print(f"Total symbols to enrich: {len(symbols)}")

def process_fund(code):
    try:
        url = f"https://fintables.com/fonlar/{code}"
        r_fund = requests.get(url, headers=headers, timeout=8)
        if r_fund.status_code != 200:
            return code, False
            
        html = r_fund.text
        
        # Price
        pm = re.search(r'\\"price\\":\s*([0-9]+\.[0-9]+)', html)
        price = float(pm.group(1)) if pm else 0.0
        if price <= 0.0:
            return code, False
            
        # Tax
        tm = re.search(r'\\"tax\\":\s*([0-9\.]+)', html)
        tax = float(tm.group(1)) * 100 if tm else 0.0
        
        # Risk
        rm = re.search(r'\\"risk\\":\s*([0-9]+)', html)
        risk = int(rm.group(1)) if rm else None
        
        # Yields
        y1m = re.search(r'\\"1m\\":\s*\{[^}]*\\"yield\\":\s*([0-9\.-]+)', html)
        y3m = re.search(r'\\"3m\\":\s*\{[^}]*\\"yield\\":\s*([0-9\.-]+)', html)
        y6m = re.search(r'\\"6m\\":\s*\{[^}]*\\"yield\\":\s*([0-9\.-]+)', html)
        y1y = re.search(r'\\"1y\\":\s*\{[^}]*\\"yield\\":\s*([0-9\.-]+)', html)
        yytd = re.search(r'\\"ytd\\":\s*\{[^}]*\\"yield\\":\s*([0-9\.-]+)', html)
        
        payload = {
            "price": price,
            "tax_percent": tax,
            "risk_level": risk,
            "yield_1m": float(y1m.group(1)) if y1m else None,
            "yield_3m": float(y3m.group(1)) if y3m else None,
            "yield_6m": float(y6m.group(1)) if y6m else None,
            "yield_1y": float(y1y.group(1)) if y1y else None,
            "yield_ytd": float(yytd.group(1)) if yytd else None
        }
        
        res = requests.patch(f"{supabase_url}?symbol=eq.{code}", headers=patch_headers, json=payload, timeout=8)
        return code, res.status_code in (200, 204)
    except Exception:
        return code, False

print("\n2. Processing ALL funds via multi-threading...")
success_count = 0
failed_count = 0

start_time = time.time()

with ThreadPoolExecutor(max_workers=20) as executor:
    futures = [executor.submit(process_fund, code) for code in symbols]
    for future in as_completed(futures):
        code, ok = future.result()
        if ok:
            success_count += 1
        else:
            failed_count += 1

elapsed = time.time() - start_time
print(f"\nFINISHED IN {elapsed:.1f} seconds!")
print(f"Total updated: {success_count}, Failed/No-data: {failed_count}")
