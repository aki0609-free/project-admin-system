# system/rule 詳細設計 12 — 給与保存モデルとTenant自動Scope候補

## 1. 方針

今回もコード修正は行わない。

加えてV2候補として、Rule/Catalog利用者が以下を明示的に書かない設計を優先候補として記録する。

```text
tenantId = :tenantId
```

Tenantはセキュリティ境界であり、Rule作者/画面設定者の責務にしない。

---

## 2. Tenant自動判定の推奨方向

望ましいDSL/Catalog設定:

```text
employee_id = :employeeId
fiscal_year = :fiscalYear
month = :month
```

実行基盤内部で対象DataSourceがtenant scopedであると判定し、実SQLには自動で:

```text
AND tenant_id = <TenantContextのtenant>
```

を付加する。

Rule parameters/factsへtenantIdを業務parameterとして露出しない。

---

## 3. なぜ自動化すべきか

手書きtenant predicate方式では:

- Catalog作成者が書き忘れる
- Ruleごとに同じ記述が増える
- tenantIdが通常parameterとしてDSLへ露出する
- 誤ったtenantIdを指定できる余地が生まれる
- セキュリティ要件を業務Rule作者へ委ねる

という問題がある。

Tenant scopeはRuleの業務条件ではなくInfrastructure/Security concernとして扱う方が適切。

---

## 4. V2候補構造

概念:

```text
Rule
 ↓
Catalog Query Request
 ↓
Catalog metadata / Table metadata
 ↓
TenantScopeResolver
 ↓
TenantContext
 ↓
QueryBuilder
 ↓
自動tenant predicate
 ↓
DB
```

Rule側からはtenantIdを見せない。

---

## 5. tenant scoped判定方法候補

### A. Catalog metadata明示

```text
tenantScope = TENANT
```

または:

```text
tenantScoped = true
```

利点: 明確。

欠点: Catalog登録時の設定漏れ余地。

### B. DB metadata / schema registry

Catalogが参照するtableについて中央Registryで:

```text
resident_tax_monthly → TENANT
insurance_rate_master → GLOBAL
income_tax_table → GLOBAL
```

を管理。

Catalog作成者は意識しない。

### C. 自動Column検出

physical tableに `tenant_id` columnが存在すれば自動scope。

利点: 設定不要。

欠点: VIEW/JOIN/alias/複雑Queryで判定が難しくなる。

### 推奨候補

Bを基本とし、起動時/登録時にDB metadataで検証する方式。

つまり中央DataSource Registryを正として:

```text
scope = GLOBAL / TENANT
```

を保持し、TENANTなら実行基盤が必ずTenantContextを注入する。

---

## 6. Fail Closed

TENANT scoped DataSourceなのにTenantContextがない場合:

```text
queryを実行しない
```

を推奨。

GLOBALへfallbackしない。

またRule parametersにtenantIdが入っていてもTenant境界には使用しない。

**V2候補**: Tenantはtrusted server contextのみをsource of truthとする。

---

## 7. ResidentTaxMonthlyへの適用例

Rule作者:

```text
employee_id = :employeeId
fiscal_year = :fiscalYear
month = :month
```

基盤内部:

```text
scope=TENANTを検出
TenantContext.requireTenantId()
tenant_id predicateを自動付加
```

最終SQL概念:

```text
WHERE tenant_id = ?
  AND employee_id = ?
  AND fiscal_year = ?
  AND month = ?
```

ただし `tenantId` はRule parameter一覧には存在しない。

---

## 8. Global master

以下のような共通法定マスタはGLOBAL scope候補。

```text
insurance_rate_master
income_tax_table
standard_salary_table
```

GLOBALの場合tenant predicateを付加しない。

したがってRule作者はGLOBAL/TENANTの違いをDSLへ書く必要がない。

---

## 9. 給与保存モデルの現状

今回確認できた給与Entity:

- `Payslip`
- `PayslipItem`
- `SalaryRecord`

いずれも `BaseEntity` を継承しTenant scoped。

---

## 10. Payslip

Path:
`backend/src/main/java/com/project/backend/features/payroll/entity/Payslip.java`

Fields:

```text
id
employeeId
closingDate
paymentDate
items
```

`BaseEntity`継承。

### 実装事実

Payslip本体には現時点で:

```text
grossAmount
totalAllowance
totalDeduction
netAmount
ruleVersion
```

等の計算snapshot fieldはない。

---

## 11. PayslipItem

Path:
`backend/src/main/java/com/project/backend/features/payroll/entity/PayslipItem.java`

Fields:

```text
id
payrollItemTypeId
amount BigDecimal(12,0)
description
payslip
```

`BaseEntity`継承。

### 金額

DB scale=0。

最終給与項目は整数円保存を前提としている。

これは共通 `PayrollItemCalculationService` の最終 `setScale(0, HALF_UP)` と整合する。

### 重要

PayslipItemには現時点で:

```text
calculatedAmount
manualOverride
ruleName
ruleVersion
ruleHash
facts
calculatorVersion
catalogVersion
```

等はない。

保存されるのは基本的に採用後の `amount`。

**既知事項 — 監査/再現性**: Entity構造上、なぜそのamountになったかを完全再現するsnapshotは保持していない。

---

## 12. SalaryRecord

Path:
`backend/src/main/java/com/project/backend/features/payroll/entity/SalaryRecord.java`

Fields:

```text
id
employeeId
yearMonth
grossSalary BigDecimal(12,0)
```

`BaseEntity`継承。

### 実装事実

SalaryRecordは非常に薄い。

現時点Entityには:

```text
deductionTotal
netSalary
taxableSalary
insuranceBase
```

等はない。

---

## 13. 現段階で分かる保存粒度

```text
Payslip
  employee + closing/payment date

PayslipItem
  item type + final amount + description

SalaryRecord
  employee + YearMonth + grossSalary
```

少なくともEntity定義上、RuleExecutionResult.factsやversion情報は保存されない。

---

## 14. Rule結果の監査Snapshot候補

V2ではPayslipItemまたは別ExecutionSnapshot tableへ以下を保存する候補。

```text
calculatedAmount
appliedAmount
manualOverride
ruleName
ruleVersion
ruleHash
catalogVersions
calculatorCode
calculatorVersion
functionLibraryVersion
taxDataVersion
roundingPolicy
```

Facts全文は個人情報/容量/秘密情報を含む可能性があるため、無条件保存ではなくallowlist/snapshot DTO化を推奨。

---

## 15. TenantとExecution Snapshot

Execution Snapshot自体も給与情報なのでTENANT scope必須。

ただしRule DSLへtenantIdを保存・公開する必要はない。

TenantはBaseEntity/TenantContext等の共通基盤から付与する。

---

## 16. 今回まだ確定していない点

給与Entityは確認したが、現main treeでは `features/payroll` 直下にservice/controller/repositoryが見当たらず、Entity/enumsのみ確認できた。

したがって:

```text
PayrollItemCalculationResult
→ PayslipItem save
→ total deduction
→ net salary
```

の実稼働Service接続はまだ確定していない。

別feature/packageに給与計算実装が存在する可能性を次回追加探索する。

---

## 17. 修正候補更新

### 高: Tenant predicateの自動注入

Rule/Catalog authorに `tenantId=:tenantId` を書かせない。

### 高: Tenant source of truthをTenantContextへ固定

Request/DSL parameterからtenantを選ばせない。

### 高: Tenant scoped DataSourceはFail Closed

TenantContextなしでQuery禁止。

### 高: 給与Rule execution snapshot

最終amountだけでは過去計算根拠を再現できない。

### 中: GLOBAL/TENANT DataSource Registry

Catalog単位の手設定より中央Registryを優先候補。

---

## 18. 次

次回はtree全体から給与計算・Payslip生成・SalaryRecord生成の利用箇所を追加探索する。

特に:

```text
PayrollItemCalculationService呼出元
Payslip生成箇所
PayslipItem生成箇所
SalaryRecord生成箇所
gross/net/deduction集計
```

を探し、Rule resultから永続化までの実際の接続有無を確定する。
