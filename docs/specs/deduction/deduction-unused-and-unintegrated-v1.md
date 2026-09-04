# 控除マスター 未使用・未連携機能の調査 V1

## 1. 結論

控除マスターの基本計算、Rule、従業員別適用、日報入力元切替、取引、残高の主要経路は実装されている。旧月次表示・月次精算フラグは管理画面から除外し、月次の計算・表示を別Viewへ分離した。残高Policyの一部は引き続き整理対象である。

## 2. 優先度：高

### 2.1 旧`carryToMonthlySettlement`（管理画面から除外済み）

月次計算対象は`vw_monthly_pay_slip_calculation_item_source`を正とする。旧項目はDB互換のため残すが、管理画面から除外した。

### 2.2 旧`showOnMonthlyStatement`（管理画面から除外済み）

月次表示対象は`vw_monthly_pay_slip_statement_item_source`を正とする。汎用MONTHLY Rule候補は返さず、旧項目はDB互換項目としてのみ残す。

## 3. 優先度：中

### 3.1 残高繰越が未参照

`carryForwardFlag`は保存・返却されるが、`PayrollItemBalanceQueryService`は参照しない。現行はEnrollmentの開始から対象日までを通算するため、実質的には月をまたいで残高が続く。

### 3.2 残高超過許可が未参照

`advanceConsumptionFlag`は保存されるが、超過の許可・拒否判定には使われていない。残高表示は`0`未満を切り捨てるため、超過量も確認できない。

### 3.3 加算頻度を画面から編集できない

`accrualFrequency`と`accrualRuleName`はAPI・DBにあるが、現在のPolicy Editorには入力欄がない。新規作成時は`MANUAL`、`MANUAL_TRANSACTION`が既定となる。

暦日加算を使う場合はSQL初期資産等で設定する必要があり、管理画面だけで完結しない。

### 3.4 汎用月次候補と給与計算が未接続

`deductionUnit=MONTHLY/PAYROLL`を指定できるが、Providerの`PAYROLL`は空リストを返す。法定控除は別の月次給与Viewで計算され、汎用控除Ruleの月次実行基盤にはつながっていない。

### 3.5 動的parameterの「日報に表示」が未接続

`dailyDisplayFlag`はDBへ保存され、従業員設定responseにも含まれる。しかし、日報画面または日報入力Serviceがこの値を参照してparameter入力欄を表示する処理は確認できない。

Ruleへ渡すparameter値は従業員設定から解決できるが、「日報上でparameter自体を見せる・編集する」という意味では未完成である。

## 4. 保存・表示中心の項目

| 項目 | 状態 |
|---|---|
| `note` | 管理メモのみ |
| `deductionType`のLEGAL以外 | 一覧分類中心。計算分岐は確認できない |
| `detailViewType=NONE` | 詳細providerなし。一般控除として正常 |

## 5. 旧仕様・重複に注意する項目

- 従業員の旧固定寮カラムと動的控除Policyが併存する。
- 月次法定控除は控除マスターのAUTO Ruleではなく、税・保険マスターと月次Viewで計算する部分がある。
- `defaultAmount`は控除金額、parameterの`defaultValue`は従業員設定値であり意味が異なる。
- `allowManualInput`は金額変更可否であり、parameterの編集可否ではない。

## 6. 未使用・整理候補コード

- 旧`DeductionValueRequest`は2026-09-02に削除し、共通PayrollItem request経路へ統一した。
- `DeductionMaster`と同義だった旧`DeductionSavePayload` aliasは2026-09-02に削除した。
- 詳細表示composableの一部は空行を返し、実データは共通mapper経由で渡されるため、責務を整理できる余地がある。

削除は参照確認とOpenAPI影響確認後に行うこと。

## 7. V1対応判断

| 優先度 | 課題 | 推奨 |
|---|---|---|
| 対応済み | 旧月次精算対象 | 管理画面から除外し、計算用Viewへ責務を統一 |
| 対応済み | 旧月次表示設定 | 管理画面から除外し、表示用Viewへ責務を統一 |
| 中 | 繰越・超過許可が未実装 | V1で必要な控除だけテストし、UI説明または実装 |
| 中 | 月次汎用Rule未接続 | V1はView計算と明記し、将来拡張へ分離 |
| 中 | parameterの日報表示が未接続 | 表示が必要な項目について日報UI・APIへ接続 |
| 低 | alias・旧Request | V1安定化後の削除候補 |

## 8. 必須テスト観点

- MANUAL/FIXED/AUTOの金額決定と上下限
- AUTO/FIXED手動変更と変更理由
- ALL_EMPLOYEES/EMPLOYEE_ENROLLMENTの候補差
- DAILY_REPORT/TRANSACTIONの従業員別切替
- 動的parameterの型・必須・SELECT・resolver
- 日報控除から月次給与への集計
- 取引型控除から月次給与への集計
- 残高の日数・金額と月跨ぎ
- 論理削除後に新規入力候補へ出ないこと

## 9. 調査した主な実装

```text
frontend/src/features/master/deduction/
frontend/src/features/master/payrollitem/
backend/src/main/java/com/project/backend/features/master/deduction/
backend/src/main/java/com/project/backend/features/master/payrollitem/
backend/src/main/resources/sql/system/report/pay_slip/
backend/src/main/resources/sql/daily_report/
```
