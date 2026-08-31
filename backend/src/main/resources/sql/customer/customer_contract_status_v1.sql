-- 顧客の契約状態を自由入力文字列から管理コードへ統一する。
-- 空値は未契約、既存の「契約中」などは契約中へ移行する。
UPDATE customers
SET contract_flag = CASE
    WHEN contract_flag IS NULL OR TRIM(contract_flag) = '' THEN 'INACTIVE'
    WHEN UPPER(TRIM(contract_flag)) IN ('ACTIVE', 'INACTIVE', 'ENDED')
        THEN UPPER(TRIM(contract_flag))
    WHEN TRIM(contract_flag) IN ('契約中', '有', 'あり', '有り') THEN 'ACTIVE'
    WHEN TRIM(contract_flag) IN ('契約終了', '終了', '解約') THEN 'ENDED'
    ELSE 'INACTIVE'
END;

ALTER TABLE customers
    MODIFY COLUMN contract_flag VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';
