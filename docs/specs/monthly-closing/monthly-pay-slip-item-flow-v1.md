# 月次給与明細の計算・表示・履歴確定フロー V1

## 1. 確定方針

```text
手当・控除マスター
  └─ 日報で入力・計算・表示する項目を管理

日報
  └─ 日次の計算結果、項目コード、名称、数量、計算根拠を保存

月次View
  ├─ SUM     : 日報・明細取引の期間合計
  ├─ LAST    : 締日時点の最新有効値
  ├─ BALANCE : 締日時点の貸付・貯蓄・汎用給与項目残高
  ├─ TAX     : 税・保険マスターによる法定控除
  └─ FIXED   : 月次帳票固有項目

月次締め
  └─ Viewの結果をhistoryTableへバージョン付きで確定

月次給与明細
  └─ historyTableの確定値だけを表示・印刷・メール送信
```

日次Ruleは日報保存時に実行する。月次ViewからJEXL、MVEL、Java Beanを呼び出さず、V1では汎用月次Rule実行基盤を設けない。

## 2. 計算と帳票表示の分離

月次帳票に表示しない項目であっても、総支給、控除合計、課税対象額、手取り計算へ含む場合がある。このため、計算対象と帳票表示対象を別Viewで管理する。

| View | 責務 |
|---|---|
| `vw_monthly_pay_slip_calculation_item_source` | 月次の金額計算に使う項目行の正本 |
| `vw_monthly_pay_slip_statement_item_source` | 月次給与明細へ表示する項目行の正本 |
| `vw_monthly_pay_slip_variable_item_source` | 既存資産互換用。表示項目Viewを参照する |
| `vw_monthly_pay_slip_variable_item` | 表示区分ごとに`display_order`、`item_code`で連番を割り当てる |
| `vw_monthly_pay_slip_gross_basis` | 計算用Viewから手当合計・総支給を計算する |
| `vw_monthly_pay_slip_deduction_basis` | 計算用Viewから法定外を含む可変控除合計を計算する |
| `vw_monthly_pay_slip_tax_calculation` | 税・保険マスターから法定控除を計算する |
| `vw_monthly_pay_slip_latest` | 最新の月次計算結果と帳票表示項目を結合する |

V1初期状態では計算対象と表示対象を同一にする。会社固有の表示変更は`vw_monthly_pay_slip_statement_item_source`だけを変更し、給与合計へ影響させない。

## 3. 項目の集計方式

| 方式 | 主な入力 | V1の例 |
|---|---|---|
| `SUM` | 承認済み日報、確定済み給与項目取引 | 早出残業、深夜、休日、勤務態度手当、寮費、携帯電話料、Wi-Fi料、前払い、貯金、借入返済 |
| `LAST` | 対象期間末日に有効な期間付きマスター | 従業員給与設定、標準報酬月額、住民税年度データ |
| `BALANCE` | 残高台帳、貸付、貯蓄 | 法定預り残高、借入残高、貯蓄残高、寮費残日数 |
| `TAX` | 所得税額表、住民税、保険料率、標準報酬 | 健康保険、子ども・子育て支援金、厚生年金、雇用保険、所得税、住民税 |
| `FIXED` | 月次帳票固有のView式 | 基本給、通勤手当、会社情報、対象期間 |

同じ項目コードであっても、日次は当日値、月次は期間合計または締日時点残高になる。月次の項目名・計算式・表示順はView側で明示する。

## 4. 表示項目の形式

月次View内部では次の行形式を維持する。

```text
tenant_id
target_month
employee_id
item_category
item_code
item_name
display_order
item_value
```

`item_category`は`ALLOWANCE`、`LEGAL_DEDUCTION`、`OTHER_DEDUCTION`を使用する。各区分を`ROW_NUMBER()`で並べ、JRXML用の01～12番へ展開する。12件を超えた場合は締め処理を失敗させ、黙って項目を欠落させない。

## 5. 履歴確定

`sp_monthly_pay_slip_snapshot`は次を同一締めバージョンで保存する。

- ヘッダー・合計：`monthly_pay_slip_history`
- 可変表示項目：`monthly_pay_slip_history_item`
- 描画用ヘッダー：`monthly_pay_slip_render_output`
- 描画用可変項目：`monthly_pay_slip_render_output_item`

`INITIAL`と`RECLOSE`は最新Viewから新しい履歴を作る。`RETRY`はViewを再参照せず、同じ締めバージョンの履歴から描画データを再作成する。

## 6. 手当・控除マスターとの境界

- `showOnDailyStatement`は日報の入力・計算候補を制御する。
- 日報へ保存された明細は日次給与明細Viewが取得する。
- 旧`showOnMonthlyStatement`は月次帳票の正本にしない。
- 旧`carryToMonthlySettlement`は月次計算条件の正本にしない。
- 旧DB列・バックエンド項目は既存資産移行のため一時的に保持するが、フロントエンドからは編集しない。

## 7. 検証

`RuntimeSchemaAssetsIntegrationTest`で次を確認する。

- 新規MySQLへ全View・ストアドを適用できる。
- `show_on_monthly_statement=false`でも、月次Viewで明示した項目は計算・表示対象になる。
- DRAFT取引は除外し、`CONFIRMED`かつ`transaction_purpose=PAYROLL_ITEM`の取引だけを合計する。`BALANCE_ACCRUAL`は未徴収・未支給残高の発生専用であり、給与額へ直接含めない。
- 締め時点のView値を履歴へ保存する。
- `RETRY`は履歴値を維持する。
- `RECLOSE`は最新Viewから新しいバージョンを作る。
