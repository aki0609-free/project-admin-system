# 従業員貸付・貯蓄 画面からDBまでの処理フロー V1

## 1. 画面構成

`EmployeeLoanSavingsPage.vue`は次の3タブを持つ。

1. 貸付
2. 貯蓄
3. 残高履歴

一覧行クリックで編集Dialog、新規作成Toolbarで新規Dialogを開く。

## 2. 貸付フロー

### 2.1 一覧

```text
useEmployeeLoansQuery
  -> GET /api/employee/loans
  -> EmployeeLoanController.findAll
  -> EmployeeLoanService.findAll
  -> EmployeeLoanRepository
  -> employee_loan
```

### 2.2 新規登録

```text
貸付 新規作成
  -> EmployeeLoanEditDialog
  -> toEmployeeLoanSaveRequest
  -> POST /api/employee/loans
  -> EmployeeLoanService.create
  -> 従業員存在・在籍・有効貸付重複を検証
  -> current_balance = principal
  -> approval_status = APPROVED
  -> employee_loan INSERT
  -> employee_finance_transactionへ貸付登録履歴
```

有効な貸付は1従業員につき1件だけ登録できる。

### 2.3 更新

従業員は変更できない。返済開始後、すなわち現在残高が元本と異なる状態では元本を変更できない。未返済状態で元本を変更すると、現在残高も新元本へ更新する。

### 2.4 削除

未返済で残高が元本と同じ、または完済で残高0の場合だけ論理削除できる。一部返済済みで残高が残る貸付は削除せず、無効化する。

## 3. 貯蓄フロー

### 3.1 一覧・登録

```text
useEmployeeSavingsQuery
  -> GET /api/employee/savings
  -> EmployeeSavingController
  -> EmployeeSavingService
  -> employee_saving
```

新規登録時、現在残高は0、承認状態は`APPROVED`となる。有効な貯蓄設定は1従業員につき1件である。

### 3.2 更新・削除

- 従業員は登録後変更できない。
- 設定更新時も現在残高は維持する。
- 現在残高が0でない貯蓄は削除できず、無効化する。

## 4. 日報との残高連携

### 4.1 参照

日報で従業員を選択すると、`GET /api/employees/{employeeId}/finance-summary`を実行する。

```text
EmployeeFinanceQueryService
  -> 有効な最新貸付
  -> 有効な最新貯蓄
  -> 借入残高・貯蓄残高
  -> 月返済予定額・月貯蓄参考額
  -> 日報Dialogへ表示
```

日報の新規作成時、月返済額・月貯蓄額は参考値として表示するだけで、実際返済額・実際貯蓄額へ自動入力しない。

### 4.2 更新

日報保存時、日報の旧値との差額を`EmployeeFinanceBalanceCommandService.applyDailyReportAmountDiff()`へ渡す。

```text
実際貯蓄額の差分
  -> employee_saving.current_balanceへ加算

実際返済額の差分
  -> employee_loan.current_balanceから減算
  -> 残高0ならactive_flag = false
```

日報訂正・取消では負の差分で残高を戻す。貯蓄残高を下回る取消、または貸付元本を超える返済取消は拒否する。

すべての残高増減は`employee_finance_transaction`へ符号付き増減額、変更前残高、変更後残高、日報ID、取引日を保存する。履歴は画面の「残高履歴」タブから確認できる。

## 5. 画面で使用するAPI

| HTTP | API | 用途 |
|---|---|---|
| GET | `/api/employee/loans` | 貸付一覧 |
| POST | `/api/employee/loans` | 貸付登録 |
| PUT | `/api/employee/loans/{id}` | 貸付更新 |
| DELETE | `/api/employee/loans/{id}` | 貸付論理削除 |
| GET | `/api/employee/savings` | 貯蓄一覧 |
| POST | `/api/employee/savings` | 貯蓄登録 |
| PUT | `/api/employee/savings/{id}` | 貯蓄更新 |
| DELETE | `/api/employee/savings/{id}` | 貯蓄論理削除 |
| GET | `/api/employees/{employeeId}/finance-summary` | 日報用残高・予定額 |
| GET | `/api/employee/finance-transactions` | 貸付・貯蓄の残高取引履歴 |

## 6. 主な関連クラス

### フロントエンド

```text
EmployeeLoanSavingsPage.vue
useEmployeeLoanSavingsPage.ts
EmployeeLoanTable.vue / EmployeeSavingTable.vue / EmployeeFinanceTransactionTable.vue
EmployeeLoanEditDialog.vue / EmployeeSavingEditDialog.vue
useEmployeeLoanEditDialog.ts / useEmployeeSavingEditDialog.ts
employeeLoanSavingConverters.ts
employeeLoanSavingFormFactory.ts
```

### バックエンド

```text
EmployeeLoanController / EmployeeSavingController
EmployeeLoanService / EmployeeSavingService
EmployeeLoanMapper / EmployeeSavingMapper
EmployeeLoanRepository / EmployeeSavingRepository
EmployeeFinanceController
EmployeeFinanceQueryService
EmployeeFinanceBalanceCommandService
EmployeeFinanceTransactionController / EmployeeFinanceTransactionService
EmployeeFinanceTransactionRepository / EmployeeFinanceTransaction
```
