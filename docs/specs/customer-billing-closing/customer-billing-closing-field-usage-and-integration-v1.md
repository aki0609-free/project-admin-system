# 顧客請求締め 項目用途・システム連携 V1

## 1. 顧客マスターから使う項目

| 項目 | 用途 |
|---|---|
| `contract_flag` | `ACTIVE`だけを締め対象にする |
| `closing_day_type/value/month_offset` | 請求期間の終了日と開始日を解決 |
| `payment_day_type/value/month_offset` | 顧客取引の入金予定日を解決 |
| `invoice_type` | 3種類の請求書report codeを解決 |
| 顧客ID・名称 | 対象識別、帳票Target、取引同期、ファイル選択表示 |

## 2. 日報請求Snapshot

顧客締めは日報保存時に確定した次の情報を使う。

- 顧客・現場
- 職種・現場役職
- 適用請求単価ID、請求単位
- 基準・残業・深夜・休日・通勤単価
- 勤務時間・各割増時間・走行距離
- 承認状態

現在の顧客/単価マスターを後から変更しても、過去日報の請求Snapshotを基準に再集計する。

## 3. `customer_billing_closings`

| 項目 | 用途 |
|---|---|
| `target_month` | 顧客請求業務月。月初日で保存 |
| `customer_id` | 顧客別締め単位 |
| `status` | `OPEN/CLOSED`等の状態 |
| `closing_version` | 初回1、再締めごとに+1 |
| `closed_at/by` | 締め日時・実行者 |

一意キーは`tenant_id + target_month + customer_id`であり、Versionごとの確定データは帳票history/file側へ保持する。

## 4. 保存帳票ファイル

`monthly_closing_report_files`には顧客締めID、Version、report code、target customer、Storage種別、ファイルキー、Batch log ID、scopeを保存する。共通月次ファイルAPIはscopeとTargetを使って顧客別ファイルを返す。

## 5. 顧客取引

確定請求historyから顧客取引へ次を連携する。

| 値 | 元 |
|---|---|
| 顧客・対象月 | 顧客締め |
| 締日Rule | 顧客マスター |
| 支払日Rule・予定日 | 顧客マスター |
| 取引金額 | 確定請求税込額 |
| 根拠ID/Version | 請求history・顧客締めVersion |

## 6. 日付とClock

- 締日到来判定：注入`Clock`の現在日
- 締め日時：注入`Clock`の`Instant`
- 期間計算：DayRule共通部品

これによりTestcontainers/単体テストで締日前・締日当日・再締めを固定時刻で検証できる。
