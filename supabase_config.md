# Supabase TEFAŞ Entegrasyonu Konfigürasyonu

## 📌 Supabase Bağlantı Bilgileri
- **Supabase Project ID:** `pxgbiedahlssklfjzwor`
- **Supabase URL:** `https://pxgbiedahlssklfjzwor.supabase.co`
- **Supabase Anon Key:** `eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InB4Z2JpZWRhaGxzc2tsZmp6d29yIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODQ0NzM5OTQsImV4cCI6MjEwMDA0OTk5NH0.h5_IBFoAT1tFAw29lcWLsF1yC4GfoADSQ8XTSWCK6L0`

---

## 🗄️ Veritabanı Tablo Şeması (`tefas_funds`)

```sql
-- 1. tefas_funds tablosunu oluşturun
CREATE TABLE IF NOT EXISTS public.tefas_funds (
    symbol VARCHAR(10) PRIMARY KEY,
    name TEXT NOT NULL,
    fund_type TEXT,
    management_company TEXT,
    price NUMERIC(14, 6) NOT NULL,
    risk_level INT2,
    tax_percent NUMERIC(5, 2),
    management_fee NUMERIC(5, 2),
    buy_valor INT2 DEFAULT 0,
    sell_valor INT2 DEFAULT 0,
    investor_count INT4,
    market_share NUMERIC(6, 2),
    yield_1m NUMERIC(8, 4),
    yield_3m NUMERIC(8, 4),
    yield_6m NUMERIC(8, 4),
    yield_1y NUMERIC(8, 4),
    yield_ytd NUMERIC(8, 4),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 2. Hızlı Arama İndeksi
CREATE INDEX IF NOT EXISTS idx_tefas_funds_name ON public.tefas_funds USING btree (name);

-- 3. Okuma İzni (RLS)
ALTER TABLE public.tefas_funds ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Herkes fonları okuyabilir"
ON public.tefas_funds FOR SELECT
TO anon, authenticated
USING (true);
```

---

## ⏰ Güncelleme Zamanlaması (Cron Job)
TEFAŞ fon fiyatları resmi olarak hafta içi sabahları değerlendiği için, Supabase Edge Function / Cron Job her sabah **saat 10:00 (TRT)** çalışarak veritabanını günceller.
