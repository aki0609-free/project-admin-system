# system/rule 詳細設計 13 — PayrollItemCalculationService呼出元と永続化接続の確認

## 1. 調査方針

今回もコード修正は行わない。

基準コード:

- main commit: `12c91a72b409df16b9d4be0b416247a07a8f170a`

前回の未確認点:

```text
PayrollItemCalculationService
→ Payslip/PayslipItem
→ SalaryRecord
→ gross / deduction / net
```

が実際に接続されているかをrepository tree全体から確認した。

---

## 2. 結論

現mainコードでは、`PayrollItemCalculationService` から `Payslip` / `PayslipItem` / `SalaryRecord` へ接続する実装を確認できなかった。

GitHub code searchでも以下の利用箇所は検出されなかった。

```text
PayrollItemCalculationService 呼出
new PayslipItem
PayslipRepository
SalaryRecordRepository
PayrollItemCalculationResult利用
```

また `features/payroll` は現状Entity/Enum中心で、給与生成Service/Controller/Repositoryを確認できない。

したがって、現在のコードを正とすると:

```text
Rule計算基盤
→ PayrollItemValueService
→ PayrollItemCalculationService
```

までは存在するが、その結果を月次給与確定・Payslip保存へ接続する部分は未実装または現在のmainには存在しない可能性が高い。

---

## 3. 確定できる実装範囲

現在追跡できている処理:

```text
Allowance/Deduction Master
 ↓
PayrollItemValueProvider
 ↓
PayrollItemValueService
 ↓
MANUAL / FIXED / AUTO
 ↓ AUTO
RuleExecutionService
 ↓
Rule result
 ↓
BigDecimal化
 ↓
min/max
 ↓
PayrollItemCalculationService
 ↓
setScale(0, HALF_UP)
 ↓
manual override判定
 ↓
PayrollItemCalculationResult
```

ここまでは実装事実。

---

## 4. 現時点で確定できない処理

以下はEntityは存在するが、上記計算結果との接続を確認できない。

```text
PayrollItemCalculationResult
 ↓ ?
PayslipItem.amount
 ↓ ?
Payslip
 ↓ ?
SalaryRecord.grossSalary
 ↓ ?
総支給
 ↓ ?
控除合計
 ↓ ?
差引支給額
```

したがって、この部分を「現行仕様」として設計書に断定してはいけない。

分類: **未決事項 / 未実装候補**。

---

## 5. Payslip/PayslipItemの位置付け

Entityは存在する。

### Payslip

```text
employeeId
closingDate
paymentDate
items
```

### PayslipItem

```text
payrollItemTypeId
amount BigDecimal(12,0)
description
```

しかし生成・保存Service/Repositoryの利用経路を確認できない。

**実装事実**: データモデルは存在。

**未決事項**: 現在どの機能がこれを生成する予定/責務なのか。

---

## 6. SalaryRecordの位置付け

Entity:

```text
employeeId
yearMonth
grossSalary BigDecimal(12,0)
```

のみ確認。

呼出/保存経路は確認できない。

**未決事項**: SalaryRecordが給与確定結果なのか、税計算用給与履歴なのか、別集計用なのかはEntityだけでは断定しない。

---

## 7. Rule基盤としての意味

これはRule設計上かなり重要。

現時点のRule機能は:

```text
「給与項目の値を計算できる基盤」
```

までは実装されている。

一方:

```text
「そのRule結果が給与確定処理で必ず利用される」
```

ところまでは現在コードから保証できない。

したがってRule機能を本番給与計算基盤として評価する際は、このIntegration gapを考慮する必要がある。

---

## 8. テスト上の意味

RuleExecutionService単体テストやPayrollItemValueService/CalculationServiceテストが存在しても、それだけでは:

```text
Rule result
→ 実給与
→ DB保存
```

を保証しない。

将来必要なIntegration Test候補:

```text
Allowance AUTO Rule
→ Payroll calculation
→ PayslipItem amount
→ Payslip save
```

```text
Deduction AUTO Rule
→ deduction aggregation
→ net salary
→ persistence
```

```text
manual override
→ calculatedAmountとappliedAmountの差
→ persistence snapshot
```

---

## 9. Transaction候補

給与確定実装時には少なくとも:

```text
給与項目計算
→ Rule実行
→ 手当/控除集計
→ Payslip
→ PayslipItem群
→ SalaryRecord等
```

のTransaction境界を明確化する必要がある。

RuleのDataSource読取自体と給与確定保存の整合性も検討対象。

**V2候補**: 給与確定単位のapplication transaction + calculation snapshot。

---

## 10. Rule version snapshotの保存タイミング

永続化接続が未確認だからこそ、給与確定実装時にversion snapshotを設計へ入れる余地がある。

候補:

```text
PayrollItemExecutionSnapshot
- tenant (BaseEntity)
- payslipItemId
- calculatedAmount
- appliedAmount
- manualOverride
- ruleName
- ruleVersion
- ruleHash
- catalogVersions
- calculatorCode
- calculatorVersion
- functionLibraryVersion
- taxDataVersion
- roundingPolicy
- calculatedAt
```

Facts全文は保存せず、再現に必要なallowlistだけをsnapshot化する候補。

---

## 11. Tenant auto-scope方針の再確認

前回方針を維持する。

Rule/Catalog authorは:

```text
tenantId = :tenantId
```

を書かない。

TENANT scoped DataSourceは基盤が自動判定し、trusted `TenantContext` からtenantを強制注入する。

これはRule Parametersとは別レイヤーのSecurity concern。

---

## 12. 現時点の分類

### 確定仕様/実装事実

- Allowance/Deduction AUTOはRuleExecutionServiceへ接続
- Rule結果はPayrollItemCalculationServiceまで届く
- min/max後、最終HALF_UP
- manual overrideあり
- Payslip/PayslipItem/SalaryRecord Entityは存在

### 未決事項

- PayrollItemCalculationServiceの実際の業務呼出元
- Payslip/PayslipItem保存処理
- SalaryRecord保存処理
- allowance/deduction集計
- gross/net計算
- 給与確定Transaction

### 修正/実装候補

- Rule→給与確定→DBのIntegration layer
- Execution Snapshot
- Version Snapshot
- Tax master version
- Tenant auto scope
- end-to-end integration tests

---

## 13. 次の調査

Ruleシステム側へ戻り、次は**テスト保証範囲**を整理する。

対象:

```text
RuleExecutionService tests
RuleLoader tests
DSL executor tests
Catalog/data source tests
PayrollItemValueService tests
PayrollItemCalculationService tests
Allowance/Deduction Provider tests
```

各テストについて:

```text
何を保証しているか
境界値
異常系
Tenant
Version
丸め
BigDecimal
employeeId where bind
不足テスト
```

を一覧化する。

これによりsystem/ruleの「実装されている」だけでなく「どこまでテストで保証されているか」を確定する。
