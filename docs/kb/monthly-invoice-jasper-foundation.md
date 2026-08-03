# 月次請求書 3パターン Jasper基盤 V1

## 1. 目的

顧客マスターの請求書パターンに従い、月次締めで確定した同一データから
3種類の請求書PDFを生成する。

対象帳票コードは次のとおり。

| 顧客の`invoice_type` | 帳票コード | 明細単位 |
| --- | --- | --- |
| `PATTERN_1` | `MONTHLY_INVOICE_PATTERN_1` | 日付・職種 |
| `PATTERN_2` | `MONTHLY_INVOICE_PATTERN_2` | 職種・役職（全現場合算） |
| `PATTERN_3` | `MONTHLY_INVOICE_PATTERN_3` | 現場・職種・役職 |

`MONTHLY_INVOICE`バッチは実行時に顧客マスターを参照し、上記の実帳票コードへ解決する。

## 2. 原本との対応

| パターン | 原本 | V1テンプレート |
| --- | --- | --- |
| 1 | 職種のみ・現場名なし | 表紙＋日付・職種別明細 |
| 2 | 職種・役職別・現場名なし | 表紙＋職種・役職別の日別マトリクス |
| 3 | 職種・役職・現場別 | 表紙＋現場・職種・役職別の日別マトリクス |

共通仕様：

- A4横
- 1ページ目は請求書表紙
- 2ページ目以降は明細
- 会社ロゴはV1初期版では表示せず、会社名を文字で表示
- パターン1の明細は調整しやすい1段構成を基準とする。原本の左右2面化はJasper Studioで追加調整可能

## 3. 確定データの流れ

```text
承認済み日報
  -> vw_monthly_invoice_latest_detail
  -> sp_monthly_invoice_snapshot
  -> monthly_invoice_history
  -> monthly_invoice_history_detail
  -> monthly_invoice_render_execution
  -> パターン別Render View
  -> JasperReports
  -> S3 / report_history / monthly_closing_item
```

重要事項：

- `INITIAL`と`RECLOSE`だけが最新Viewを読み、履歴を新規確定する
- `RETRY`は同じVersionのhistoryを再利用し、最新Viewを読み直さない
- 一般バッチの「再実行」操作でも、月次請求書だけは`executionMode=RETRY`へ自動変換する
- 同一の対象月・顧客・Versionは一意とし、上書きしない
- 保存済みPDFの確認・印刷はS3上の完成ファイルを使い、再集計しない

## 4. 請求計算

### 4.1 通常分

| `billing_unit` | 数量 | 金額 |
| --- | --- | --- |
| `DAILY` | `work_hours / 8` | `ROUND(quantity * billing_base_unit_price, 0)` |
| `HOURLY` | `work_hours` | `ROUND(quantity * billing_base_unit_price, 0)` |

### 4.2 割増・通勤

```text
時間外金額 = overtime_hours × billing_overtime_unit_price
深夜金額   = night_work_hours × billing_night_unit_price
休日金額   = holiday_work_hours × billing_holiday_unit_price
通勤金額   = mileage × billing_commute_unit_price
```

各金額は1円単位で四捨五入する。

### 4.3 消費税

- V1初期値：10%
- `tax_amount = ROUND(subtotal_amount * tax_rate, 0)`
- `total_amount = subtotal_amount + tax_amount`

税率はinputTableへ保存するため、将来の税率変更時も過去Versionを再現できる。

### 4.4 未対応単位

`MONTHLY`と`FIXED`は日報行ごとに計算すると重複請求になるため、V1では生成を停止する。
請求期間に1回だけ計上する契約ルールをマスター化してから対応する。

## 5. スナップショット項目

締め時に次の情報をhistoryへ固定する。

- 顧客名、請求書パターン
- 対象月、請求期間、締めVersion、発行日
- 請求書番号
- 自社名、住所、電話、FAX
- 適格請求書発行事業者番号
- 振込先表示、請求書備考
- 税率、小計、消費税、合計
- 日付、現場、職種、役職、単価、数量、各割増・通勤金額

請求書番号：

```text
YYYYMM-{customerId 6桁}-V{closingVersion}
```

例：`202606-000001-V2`

## 6. DB資産

### 6.1 適用順

```text
1. backend/src/main/resources/sql/system/report/monthly_snapshot_foundation_v1.sql
2. backend/src/main/resources/sql/system/report/invoice/monthly_invoice_foundation_v1.sql
3. backend/src/main/resources/sql/system/report/invoice/monthly_invoice_render_views_v1.sql
4. backend/src/main/resources/sql/system/report/invoice/monthly_invoice_report_master_v1.sql
```

### 6.2 作成される主な資産

| 種別 | 名前 |
| --- | --- |
| input | `monthly_invoice_input` |
| history header | `monthly_invoice_history` |
| history detail | `monthly_invoice_history_detail` |
| output mapping | `monthly_invoice_render_execution` |
| source view | `vw_monthly_invoice_latest_detail` |
| pattern 1 | `vw_monthly_invoice_pattern_1_render` |
| pattern 2 | `vw_monthly_invoice_pattern_2_render` |
| pattern 3 | `vw_monthly_invoice_pattern_3_render` |
| snapshot procedure | `sp_monthly_invoice_snapshot` |
| cleanup procedure | `sp_monthly_invoice_cleanup` |

ストアドは次の場合に明示的に失敗させる。

- inputがない
- 対象期間に請求元データがない
- `MONTHLY`または`FIXED`の未設定ルールを含む
- 顧客または有効な会社情報がない
- `RETRY`対象のhistoryがない

## 7. Jasper資産

```text
backend/src/main/resources/reports/monthly_invoice_pattern_1.jrxml
backend/src/main/resources/reports/monthly_invoice_pattern_2.jrxml
backend/src/main/resources/reports/monthly_invoice_pattern_3.jrxml
```

アプリ起動時、S3に同名テンプレートが存在しない場合だけ配布資産から初期登録する。
既にS3で調整済みのテンプレートは上書きしない。

## 8. 検証結果

ローカルMySQL 8.4で以下を確認した。

- DDL、View、ストアドの作成成功
- 初回確定と再締めで新しいVersionを作成
- `RETRY`でhistoryを増やさずrenderデータだけ作成
- 帳票マスター3件、各パラメータ7件を登録
- サンプル計算：小計61,028円、税6,103円、合計67,131円
- パターン1/2/3のJRXMLコンパイルとPDF出力成功
- 全テンプレートがA4横、2ページ以上で出力されることを確認

## 9. 実環境適用前チェック

- 顧客ごとの`invoice_type`を確認する
- `company_profile`の会社情報・適格番号・振込先を確認する
- 日報へ請求単価スナップショットが保存されていることを確認する
- `MONTHLY`または`FIXED`契約の有無を確認する
- Jasper Studioで顧客指定の罫線、文字サイズ、改ページを最終調整する
- DEVで再締め・失敗再実行・S3プレビューを通し、原本と金額を照合する
- 合格後に本番DBへSQLを順番に適用する

## 10. 将来拡張

- 会社ロゴのS3テンプレート資産化
- 顧客別税率・端数処理マスター
- `MONTHLY`・`FIXED`の期間課金ルール
- 請求先担当者Resolverとメール送信
- JasperテンプレートVersion・ハッシュの履歴固定
- 適格請求書の税率別内訳
