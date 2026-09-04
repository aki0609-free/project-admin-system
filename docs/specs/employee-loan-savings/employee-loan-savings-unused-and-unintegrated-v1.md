# 従業員貸付・貯蓄 未使用・未連携機能の調査 V1

## 1. 承認機能

`employee_loan`と`employee_saving`には次が存在する。

- `approval_status`
- `approval_comment`

しかし保存Requestと現行Dialogに承認入力はなく、Serviceは作成・更新のたびに`APPROVED`と`NULL`を設定する。V1で承認フローを実施しない方針と整合するが、現在は将来用カラムである。

承認機能を再開するまでは、Responseへ含める必要性を検討できる。少なくとも画面上で承認済みと誤認させる独立フローは存在しない。

## 2. 個別詳細APIの画面未接続

Controllerには次がある。

- `GET /api/employee/loans/{id}`
- `GET /api/employee/savings/{id}`

現行画面は一覧Responseの行データをそのまま編集Dialogへ渡すため、個別詳細GETを呼ぶQueryはない。外部利用がなければAPI削減候補である。

## 3. 日付項目のV1運用

### 3.1 借入日

`loanDate`は保存・一覧表示されるが、日報日付より後の借入を返済対象外にする判定には使われない。

### 3.2 返済開始日

`repaymentStartDate`も保存・一覧表示だけで、日報の実際返済額入力や残高更新を開始日前として拒否しない。

V1では開始日前返済も運用上許可する。`repaymentStartDate`は返済予定の確認用であり、日報保存や残高更新を拒否する条件には使わない。

## 4. 「積立計算基礎額」への名称統一（解決済み）

計算の意味に合わせ、API・Java・TypeScript・画面表示を「積立計算基礎額」へ改名した。

```text
積立計算基礎額 × 貯蓄率
```

DB物理カラム`min_salary_threshold`は既存環境との互換性のためV1では改名しない。

## 5. 予定額と実績額は自動連携しない

- `monthlyRepayment`は日報へ参考表示されるだけである。
- `monthlySavingAmount`も日報へ参考表示されるだけである。
- 新規日報の`loanRepaymentAmount`と`savingAmount`は0へ初期化される。

これは「その日に実際に受領した金額を入力する」という現行仕様であり、不具合ではない。ただし、利用者が予定額が自動徴収されると誤認しやすいため、画面説明が必要である。

## 6. 残高取引履歴（解決済み）

現在残高の更新に加えて、`employee_finance_transaction`へ不変の取引履歴を保存する。

```text
daily_report.loan_repayment_amount / saving_amount
  -> 差分更新
  -> employee_loan.current_balance / employee_saving.current_balance
  -> employee_finance_transaction（増減額・変更前後残高）
```

貸付登録、元本訂正、日報返済・積立、日報訂正・削除による取消を記録する。既存残高はDDL適用時に`OPENING_BALANCE`として1回だけ登録する。

## 7. 命名上の保守課題（解決済み）

`useEmployeeLoadDialog.ts`を`useEmployeeLoanDialog.ts`へ改名し、export名とファイル名を一致させた。

## 8. V1判断一覧

| 優先度 | 課題 | 推奨 |
|---|---|---|
| 完了 | 最低給与額の名称と計算意味が不一致 | 「積立計算基礎額」へ統一 |
| 完了 | 返済開始日が返済制御に未使用 | V1は開始日前返済を運用上許可 |
| 完了 | 残高の専用取引履歴がない | 共通残高取引履歴を実装 |
| 低 | 承認項目が固定値 | V2承認フローまで内部予約として維持 |
| 低 | 個別詳細GETが画面未使用 | 外部利用確認後に削減候補 |
| 完了 | `Load` / `Loan`ファイル名誤り | `Loan`へ改名 |

## 9. 調査した主な実装

```text
frontend/src/features/employees/pages/EmployeeLoanSavingsPage.vue
frontend/src/features/employees/composables/useEmployeeLoanSavingsPage.ts
frontend/src/features/employees/composables/useEmployeeLoanEditDialog.ts
frontend/src/features/employees/composables/useEmployeeSavingEditDialog.ts
frontend/src/features/dailyreport/composables/useDailyReportEditDialog.ts
backend/src/main/java/com/project/backend/features/employee/
backend/src/main/java/com/project/backend/features/dailyreport/
backend/src/main/resources/sql/system/report/pay_slip/
```
