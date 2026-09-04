# 従業員貸付・貯蓄 入力項目の利用先・システム連携 V1

## 1. 貸付

保存先：`employee_loan`

| 画面項目 | API項目 | DBカラム | 区分 | 主な利用先 |
|---|---|---|---|---|
| 従業員 | `employeeId` | `employee_id` | 利用中 | 残高、日報、給与明細との結合。登録後変更不可 |
| 借入元本 | `principal` | `principal` | 利用中 | 初期残高、返済取消上限、給与明細 |
| 月返済額 | `monthlyRepayment` | `monthly_repayment` | 利用中 | 日報の月返済参考額。実返済額へ自動入力はしない |
| 借入日 | `loanDate` | `loan_date` | 画面内利用 | 一覧・管理記録。返済可否の自動開始判定には未使用 |
| 返済開始日 | `repaymentStartDate` | `repayment_start_date` | 運用上の予定日 | 一覧・管理記録。V1では開始日前の返済も運用上許可し、自動制御しない |
| 借入残高 | Responseのみ | `current_balance` | 派生値 | 日報返済差分、給与明細、台帳。画面直接編集不可 |
| 有効 | `activeFlag` | `active_flag` | 利用中 | 日報の返済対象・残高参照。完済時は自動false |
| 承認状態 | Responseのみ | `approval_status` | 固定値 | V1では保存時`APPROVED`固定 |
| 承認コメント | Responseのみ | `approval_comment` | 未使用 | V1では常にNULL |

## 2. 貯蓄

保存先：`employee_saving`

| 画面項目 | API項目 | DBカラム | 区分 | 主な利用先 |
|---|---|---|---|---|
| 従業員 | `employeeId` | `employee_id` | 利用中 | 残高、日報、給与明細との結合。登録後変更不可 |
| 貯蓄率 | `percentage` | `percentage` | 利用中 | 月貯蓄参考額の計算 |
| 積立計算基礎額 | `savingCalculationBaseAmount` | `min_salary_threshold` | 利用中 | 貯蓄率を掛ける計算元。DBカラム名はV1互換性のため維持 |
| 貯蓄残高 | Responseのみ | `current_balance` | 派生値 | 日報貯蓄差分、給与明細、台帳。画面直接編集不可 |
| 有効 | `activeFlag` | `active_flag` | 利用中 | 日報の貯蓄対象・残高参照 |
| 承認状態 | Responseのみ | `approval_status` | 固定値 | V1では保存時`APPROVED`固定 |
| 承認コメント | Responseのみ | `approval_comment` | 未使用 | V1では常にNULL |

## 3. 月貯蓄参考額の現行計算

```text
monthlySavingAmount
  = savingCalculationBaseAmount × percentage ÷ 100
```

実際の給与との閾値比較は行わない。名称も計算の意味に合わせて「積立計算基礎額」へ統一した。

日報ではこの値を参考表示するだけであり、実際貯蓄額は利用者が入力する。

## 4. 日報・月次への連携

| 値 | 日報 | 月次・帳票 |
|---|---|---|
| 借入残高 | 参照表示 | 日次・月次給与明細の残高表示 |
| 実際返済額 | 日報で入力 | 日報集計・月次返済額、残高減算 |
| 貯蓄残高 | 参照表示 | 日次・月次給与明細の残高表示 |
| 実際貯蓄額 | 日報で入力 | 日報集計・月次貯蓄額、残高加算 |
| 月返済額 | 参考表示 | 自動徴収額ではない |
| 月貯蓄参考額 | 参考表示 | 自動徴収額ではない |

## 5. 値の所有関係

- 借入元本・予定月返済額：`employee_loan`。
- 借入残高：`employee_loan.current_balance`をServiceだけが更新する。
- 貯蓄率・計算基礎額：`employee_saving`。
- 貯蓄残高：`employee_saving.current_balance`をServiceだけが更新する。
- その日に実際に返済・貯蓄した金額：`daily_report`。
- 月次の返済・貯蓄額：対象期間の日報を集計する。
- 残高の増減根拠：`employee_finance_transaction`に変更前・変更後残高と符号付き増減額を保存する。
