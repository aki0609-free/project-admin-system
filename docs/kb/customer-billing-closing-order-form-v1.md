# 顧客請求締め・月次注文書 V1

## 1. 目的

給与・社内帳票で使用する会社側締日と、請求書・注文書で使用する顧客別締日を分離する。

顧客請求締めでは、顧客マスターの締日を基準に日報を集計し、次の処理を同一Versionで確定する。

1. 月次請求書を生成する
2. 月次注文書を生成する
3. 顧客取引へ確定請求額を反映する

## 2. 締め処理の区分

| 区分 | メニュー | 対象 |
|---|---|---|
| 社内月次締め | 締め処理 → 月次管理 | 給与、控除、従業員系帳票、社内台帳 |
| 顧客請求締め | 締め処理 → 顧客請求締め | 請求書、注文書、顧客取引、入金予定 |

社内月次締めでは請求書・注文書を生成しない。

## 3. 顧客別請求期間

対象請求月が2026年7月の場合の例を示す。

| 顧客締日 | 集計開始 | 集計終了 |
|---|---|---|
| 月末 | 2026-07-01 | 2026-07-31 |
| 20日 | 2026-06-21 | 2026-07-20 |
| 31日指定 | 2026-07-01 | 2026-07-31 |

2月など指定日が存在しない月は、その月の末日へ補正する。

締日当日にシステムを起動する必要はない。後日実行しても、対象請求月と顧客マスターから同じ期間を再計算する。

## 4. 明示締めと再締め

### 初回締め

1. 対象請求月を選択する
2. 顧客別の締日、集計期間、税抜額、税額、税込額を確認する
3. 顧客行の`締め`、または`全顧客締め`を押す
4. Version 1として請求書・注文書・顧客取引を確定する

締め状態とVersionは`対象請求月＋顧客`単位で管理する。顧客ごとにV1、V2、未締めが混在してよい。

`全顧客締め`ではアプリケーションClockの日本日付と顧客の集計終了日を比較し、次の顧客だけを処理する。

- 実行日が締日当日または締日後
- 請求対象データがある
- まだ締めていない

締日前の顧客と締め済み顧客は除外する。一社が失敗しても、別Transactionで成功した他社の締め結果は維持する。

個別の`締め`は例外運用のため締日前でも実行可能とするが、画面に確認警告を表示する。

### 再締め

日報や単価を修正した場合は`再締め`を実行する。

- 最新Viewから再集計する
- 対象顧客のVersionだけを1増やす
- 旧Versionの履歴とPDFは残す
- 顧客取引は最新Versionの確定請求額へ更新する

帳票一覧は月全体のVersionでは絞らず、顧客ごとの最新確定Versionを表示する。

## 5. 注文書仕様

### 生成単位

`対象請求月＋顧客`ごとに1枚生成する。複数現場の金額は1枚へ合算する。

### 金額

注文書では税額を再計算しない。確定済みの`monthly_invoice_history`から次の値を取得する。

| 注文書項目 | 取得元 |
|---|---|
| うち工事価格 | subtotal_amount |
| 消費税及び地方消費税 | tax_amount |
| 請負代金額 | total_amount |
| 税率 | tax_rate |

これにより請求書と注文書の1円差を防止する。

### 日付

| 項目 | 値 |
|---|---|
| 請求書提出日 | 顧客請求期間の終了日 |
| 注文書右上の日付 | 請求書提出日－45日 |
| 表示上の工期開始 | 請求書提出日－45日 |
| 表示上の工期終了 | 請求書提出日 |

金額集計期間と、注文書へ表示する45日間は別の値として履歴保存する。

### 会社・顧客情報

| 原本欄 | 取得元 |
|---|---|
| 下請負人 | 会社情報マスター |
| 元請負人 | 顧客マスター |

元請負人は`showPrimeContractor`で表示を切り替える。名称・郵便番号・住所は注文書生成時点の値を履歴へ固定する。

## 6. DB資産

適用対象：

- `sql/operation/monthly/customer_billing_closing_v1.sql`
- `sql/system/report/order_form/monthly_order_form_foundation_v1.sql`

主なテーブル：

- `customer_billing_closings`
- `monthly_order_form_input`
- `monthly_order_form_history`
- `monthly_order_form_render_execution`

主なView・ストアド：

- `vw_monthly_order_form_render`
- `sp_monthly_order_form_snapshot`
- `sp_monthly_order_form_cleanup`

## 7. 帳票資産

- 帳票コード：`MONTHLY_ORDER_FORM`
- バッチコード：`PRINT_MONTHLY_ORDER_FORM`
- JRXML：`reports/monthly_order_form.jrxml`
- 出力形式：PDF
- 履歴管理：あり
- S3保存：あり
- 年度バックアップ：7年

## 8. 確認項目

- 月末締めと20日締めで対象期間が正しい
- 2月の末日補正が正しい
- 請求書と注文書の3金額が一致する
- 複数現場が顧客単位で合算される
- 元請負人の表示ON/OFFが機能する
- 初回締めと再締めでVersionが増える
- 旧Versionの履歴とPDFが残る
- 顧客取引が最新の確定請求額を参照する

## 9. AWS DEV反映記録（2026-08-11）

### 9.1 DB資産

- `runtime-schema-manifest.txt`に登録された34資産をRDSへ適用した。
- 顧客請求締め、注文書のinput／history／render execution、View、ストアド、帳票マスターを含む。
- 適用結果：`RUNTIME_SCHEMA_UPGRADE_COMPLETE`
- 既存業務データの削除は実施していない。

### 9.2 デプロイイメージ

| 対象 | ECRイメージ | Digest |
|---|---|---|
| Backend | `project-admin-dev-backend:manual-20260811-132845Z` | `sha256:794a16589bc1746fe8153f28b9e0881a7ccf670b5f07865e2c6ea501a86b7729` |
| Frontend | `project-admin-dev-frontend:manual-20260811-133141Z` | `sha256:c555e03c63f15750778ae6d7f7305e41c6ca63d0bbb4a3c324d33008dffb61d3` |

### 9.3 反映後確認

| 確認項目 | 結果 |
|---|---|
| Backend actuator | `UP` |
| Backend container | `healthy`／再起動0回 |
| Frontend container | `healthy`／再起動0回 |
| Redis container | `healthy` |
| Cloudflare Tunnel | 稼働中 |
| 公開URL | Cloudflare AccessへのHTTP 302を確認 |

確認URL：`https://project-admin.fuyo-system.com/operation/customer-billing`

既に締め済みの対象月で請求書・注文書を生成する場合は、「顧客請求締め」から再締めを実行する。
