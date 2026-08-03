# 労務費一覧表 Excel帳票基盤 V1

## 1. 目的

月次給与計算対象の全従業員を、既存の「労務費一覧」Excel書式へ出力する。

- 出力形式：Excel（`.xlsx`）
- 生成単位：対象月につき1ファイル
- データ単位：従業員につき1行
- 確定タイミング：月次締め
- 履歴：保持する
- 年次バックアップ：7年間
- 再締め：締めVersionを1つ上げ、最新Viewから新しい履歴を作成する
- 再出力：同じ締めVersionの履歴からExcelを再生成する

この帳票は日々編集する台帳ではないため、Spreadsheet台帳基盤ではなく帳票基盤の `EXCEL` を使用する。

## 2. 確定仕様

### 2.1 対象者

`vw_monthly_pay_slip_employee_month` が返す、月次給与計算対象の全従業員を対象とする。

- 在籍期間が対象締め期間と重なる
- 従業員が有効
- 月給契約が有効
- 個別従業員の選択は行わない

### 2.2 支払日

`closing_setting` の有効な `setting_code = 'PAYROLL'` を使用する。

- `payment_day_type`
- `payment_day_value`
- `payment_month_offset`

月末指定および月の日数を超える日付は、既存の `DayRuleUtils` と同じルールで月末へ丸める。

例：

- 対象月：2026年4月
- 支払設定：翌月15日
- Excel表示：`5/15支払`

### 2.3 テンプレート行数

原本の明細行は4～36行目の33名分である。

- 33名以下：既存の33行を使用し、未使用行を空欄にする
- 34名以上：合計行の直前へ必要な行数を追加する
- 追加行：原本明細行の高さ、罫線、表示形式を引き継ぐ
- 合計行：追加後の最終明細行までを `SUM` する

### 2.4 休業欄

原本の「休業」欄には、日報から月次集計した有給日数
`paid_leave_days` を出力する。

## 3. 処理フロー

```text
日報・従業員・給与マスター・給与締め設定
  ↓
vw_monthly_pay_slip_latest
  ↓
vw_monthly_labor_cost_list_latest
  ↓ INITIAL / RECLOSE
monthly_labor_cost_list_history
  ↓ INITIAL / RECLOSE / RETRY
monthly_labor_cost_list_output
  ↓
monthly_labor_cost_list.xlsx + 帳票専用Renderer
  ↓
S3保存
  ↓
report_history
  ↓ 会計年度終了後
7年バックアップ
```

最新Viewは常に最新の日報・マスターを参照する。確定後の閲覧・再出力では、最新Viewではなく締めVersion付きの履歴を使用する。

## 4. Excel列マッピング

| Excel列 | 表示名 | 出力カラム |
|---|---|---|
| A | 氏名 | `employee_name` |
| B | 出勤 | `work_day_count` |
| C | 休業 | `paid_leave_days` |
| D | 残業 | `overtime_hours` |
| E | 深夜 | `night_work_hours` |
| F | 基本給 | `basic_salary` |
| G | 残業 | `overtime_pay_amount` |
| H | 深夜 | `night_pay_amount` |
| I | 運転 | `driver_allowance_amount` |
| J | 他手当 | `other_allowance_amount` |
| K | 出張手当 | `business_trip_allowance_amount` |
| L | 支給合計 | `gross_amount` |
| M | 健保 | `health_insurance` |
| N | 子ども子育て支援金 | `child_care_contribution` |
| O | 厚年 | `pension_insurance` |
| P | 雇用 | `employment_insurance` |
| Q | 社保計 | `social_insurance_total` |
| R | 課税対象 | `taxable_amount` |
| S | 所得税 | `income_tax` |
| T | 年調分 | `year_end_adjustment_amount` |
| U | 住民税 | `resident_tax` |
| V | 寮費 | `dormitory_fee_amount` |
| W | 携帯貸出料 | `mobile_rental_amount` |
| X | WiFi使用料 | `wifi_fee_amount` |
| Y | その他 | `other_deduction_amount` |
| Z | 控除合計 | `deduction_total` |
| AA | 総支給額 | `net_before_advance_amount` |
| AB | 前払 | `advance_payment_amount` |
| AC | 貯金 | `saving_amount` |
| AD | 差引支給 | `net_payment_amount` |

休日金額は原本に専用列がないため、`他手当` に含める。今後原本へ休日列を追加する場合は、帳票専用RendererとViewの列マッピングだけを変更する。

## 5. 可変手当・控除の集約ルール

### 手当

- `OVERTIME_PAY`：残業
- `NIGHT_PAY`：深夜
- `DRIVER_ALLOWANCE`：運転
- `BUSINESS_TRIP_ALLOWANCE` / `TRIP_ALLOWANCE` / `BUSINESS_TRIP`：出張手当
- 上記以外および休日金額：他手当

### 控除

- `YEAR_END_ADJUSTMENT`：年調分
- `DORMITORY_FEE`：寮費
- `MOBILE_RENTAL`：携帯貸出料
- `WIFI_FEE`：WiFi使用料
- `ADVANCE_PAYMENT`：前払
- `SAVING` / `SAVINGS`：貯金
- 上記以外：その他

業務項目の追加はマスターおよびView側で吸収し、Excel共通出力基盤は変更しない。

## 6. 月次給与Viewへの整合性修正

日報へ確定保存している以下の金額を、月次給与の可変手当へ追加した。

- `OVERTIME_PAY`
- `NIGHT_PAY`
- `HOLIDAY_PAY`

修正前はこれらが月次総支給へ含まれず、新帳票の列合計と課税・差引支給が一致しない状態だった。

影響範囲：

- 月次給与明細の手当、総支給、雇用保険、課税対象、所得税、差引支給
- 労務費一覧表の同項目

日報の確定金額カラムから1度だけ集計し、`daily_report_allowances` との二重計上は行わない。

## 7. 実装資産

### Excelテンプレート

`backend/src/main/resources/reports/monthly_labor_cost_list.xlsx`

- 実在の氏名、会社名、金額を除去済み
- 原本の罫線、列幅、行高、印刷書式を維持
- テンプレート確認用の計算式を修復済み
- 実行時には締め履歴の確定値を差し込む

### Java

- `ExcelTemplateReportRenderer`
- `ExcelTemplateReportRendererRegistry`
- `MonthlyLaborCostListExcelRenderer`
- `ExcelReportExporter`
- `ReportTemplateLoader`
- `ReportTemplateValidator`
- `BundledReportTemplateInitializer`

共通基盤はテンプレートのS3取得、Renderer選択、保存、帳票履歴を担当する。帳票固有のセル位置は `MonthlyLaborCostListExcelRenderer` だけに閉じ込める。

### SQL

`backend/src/main/resources/sql/system/report/labor_cost/monthly_labor_cost_list_foundation_v1.sql`

作成される主な資産：

- `vw_monthly_labor_cost_list_item_total`
- `vw_monthly_labor_cost_list_latest`
- `monthly_labor_cost_list_input`
- `monthly_labor_cost_list_history`
- `monthly_labor_cost_list_output`
- `sp_monthly_labor_cost_list_snapshot`
- `sp_monthly_labor_cost_list_cleanup`
- 帳票マスター `MONTHLY_LABOR_COST_LIST`
- バッチ `PRINT_MONTHLY_LABOR_COST_LIST`
- 月次帳票定義 `MONTHLY / EXCEL`
- 月次締め出力定義（実行順15、7年バックアップ）

## 8. DB適用順

```text
1. sql/daily_report/pay_component_rule_foundation_v1.sql
2. sql/system/report/monthly_snapshot_foundation_v1.sql
3. sql/operation/monthly/closing_output_foundation_v1.sql
4. sql/system/report/pay_slip/monthly_pay_slip_view_foundation_v1.sql
5. sql/system/report/labor_cost/monthly_labor_cost_list_foundation_v1.sql
```

各SQLは再実行可能な構成とする。ローカルMySQLでは、新帳票SQLの連続2回適用に成功している。

## 9. テスト結果

### Excel Renderer

- 35名を出力
- 原本33行を超える2行を自動追加
- 合計行が追加行の後へ移動
- 会社名、対象年月、支払日を出力
- 先頭・末尾従業員を出力
- 基本給、差引支給の合計を検証

### 回帰テスト

帳票関連30テストが成功した。

- 月次給与明細 Jasper
- 日次給与明細 Jasper
- 請求書3パターン Jasper
- 帳票S3保存・履歴
- メール個人別出力
- HTMLテンプレート
- Excelテンプレート初期登録

## 10. AWS反映後の確認

1. DB SQLを上記順で適用する。
2. Backendをbuild・ECR push・deployする。
3. 起動時に `templates/reports/monthly_labor_cost_list.xlsx` がS3へ初期登録されたことを確認する。
4. 月次締め対象月を準備する。
5. `MONTHLY_LABOR_COST_LIST` を含む月次締めを実行する。
6. S3へ `.xlsx` が保存されたことを確認する。
7. 帳票履歴から同じファイルを取得できることを確認する。
8. 再出力では同じ締めVersionの値が変わらないことを確認する。
9. 再締めでは締めVersionが1増え、最新Viewの値が新履歴になることを確認する。
10. 34名以上のデータで明細行が自動追加されることを確認する。

## 11. V1での制約

- Excelファイル内の手入力値は正データにしない。
- 計算結果の正は月次履歴テーブルとする。
- Renderer変更はBackendの再build・再deployが必要。
- セル配置変更はこの帳票のRendererだけで対応でき、帳票共通基盤の変更は不要。
- 原本の列追加・削除時は、View・履歴・出力・Renderer・テストの5点を同時に更新する。
