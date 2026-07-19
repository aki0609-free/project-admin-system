-- 既存の給与明細出力テーブルへ、REPORT_MAIL用の標準宛先項目を追加する。
-- 同じ環境で複数回実行しないこと。
--
-- 注意：PROCEDURE方式の環境では、このDDLを単独で実行しないこと。
-- 出力列を動的取得するプロシージャの場合、標準宛先項目を動的コピー対象から除外し、
-- INSERT後にrecipient_* / business_key / mail_*を設定する処理を同時に反映する必要がある。
-- 詳細は「REPORT_MAIL 個人別帳票生成・保存・メール連携 KB」を参照すること。

alter table monthly_pay_slip_output
    add column recipient_key varchar(255) null after employee_name,
    add column recipient_name varchar(255) null after recipient_key,
    add column recipient_email varchar(255) null after recipient_name,
    add column business_key varchar(255) null after recipient_email,
    add column mail_type varchar(100) null after business_key,
    add column mail_template_key varchar(100) null after mail_type;

create index idx_monthly_pay_slip_output_delivery
    on monthly_pay_slip_output (execution_id, business_key);

alter table daily_pay_slip_output
    add column recipient_key varchar(255) null after employee_name,
    add column recipient_name varchar(255) null after recipient_key,
    add column recipient_email varchar(255) null after recipient_name,
    add column business_key varchar(255) null after recipient_email,
    add column mail_type varchar(100) null after business_key,
    add column mail_template_key varchar(100) null after mail_type;

create index idx_daily_pay_slip_output_delivery
    on daily_pay_slip_output (execution_id, business_key);
