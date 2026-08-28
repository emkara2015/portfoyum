-- TEFAŞ katalog/senkron durumunu fiyat verisinden bağımsız izler.
-- Başarısız bir çekim mevcut geçerli price/updated_at değerini değiştirmez.

ALTER TABLE public.tefas_funds
    ADD COLUMN IF NOT EXISTS is_active boolean NOT NULL DEFAULT true,
    ADD COLUMN IF NOT EXISTS last_seen_at timestamptz,
    ADD COLUMN IF NOT EXISTS last_attempt_at timestamptz,
    ADD COLUMN IF NOT EXISTS last_success_at timestamptz,
    ADD COLUMN IF NOT EXISTS consecutive_failures integer NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS catalog_miss_count integer NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS last_sync_status text NOT NULL DEFAULT 'unknown',
    ADD COLUMN IF NOT EXISTS last_error text;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conrelid = 'public.tefas_funds'::regclass
          AND conname = 'tefas_funds_consecutive_failures_nonnegative'
    ) THEN
        ALTER TABLE public.tefas_funds
            ADD CONSTRAINT tefas_funds_consecutive_failures_nonnegative
            CHECK (consecutive_failures >= 0);
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conrelid = 'public.tefas_funds'::regclass
          AND conname = 'tefas_funds_catalog_miss_count_nonnegative'
    ) THEN
        ALTER TABLE public.tefas_funds
            ADD CONSTRAINT tefas_funds_catalog_miss_count_nonnegative
            CHECK (catalog_miss_count >= 0);
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conrelid = 'public.tefas_funds'::regclass
          AND conname = 'tefas_funds_last_sync_status_valid'
    ) THEN
        ALTER TABLE public.tefas_funds
            ADD CONSTRAINT tefas_funds_last_sync_status_valid
            CHECK (last_sync_status IN ('unknown', 'success', 'failed'));
    END IF;
END;
$$;

CREATE INDEX IF NOT EXISTS idx_tefas_funds_active
    ON public.tefas_funds (is_active);

CREATE INDEX IF NOT EXISTS idx_tefas_funds_sync_status
    ON public.tefas_funds (last_sync_status, last_attempt_at);

-- Mevcut fiyat kayıtları geçerli bir başlangıç noktasıdır; yeniden fiyat çekilene
-- kadar bunları başarısız veya pasif olarak işaretleme.
UPDATE public.tefas_funds
SET last_success_at = COALESCE(last_success_at, updated_at),
    last_sync_status = CASE
        WHEN updated_at IS NOT NULL AND price IS NOT NULL AND price > 0
            THEN 'success'
        ELSE last_sync_status
    END
WHERE last_success_at IS NULL
   OR (last_sync_status = 'unknown' AND updated_at IS NOT NULL AND price > 0);
