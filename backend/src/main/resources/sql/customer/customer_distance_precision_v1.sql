-- 顧客現場と日別配置の距離を小数第1位まで保持するV1互換変更。
-- 既存の整数値は同値のDECIMALへ安全に変換される。

ALTER TABLE customer_sites
    MODIFY COLUMN distance_from_company_km DECIMAL(10,1) NULL;

ALTER TABLE daily_preparation_dispatches
    MODIFY COLUMN distance_from_company_km DECIMAL(10,1) NULL;
