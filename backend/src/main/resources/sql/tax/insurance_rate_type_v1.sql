-- 社会保険種別をJava側の文字列Enumと整合させる。
--
-- 初期リリース時にMySQL ENUMとして作成された環境では、後から追加した
-- CARE_INSURANCE等を外部データ取込で保存できない。VARCHARへ統一することで、
-- JavaのInsuranceTypeを正本とし、将来の制度追加でもDB定義の取り残しを防ぐ。
ALTER TABLE insurance_rate_master
    MODIFY COLUMN insurance_type VARCHAR(50) NOT NULL;
