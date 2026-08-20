# system/rule 詳細設計 11 — 税・保険データモデルとRule参照上の論点

## 1. 対象

Rule/控除計算から利用されうる以下のデータモデルを確認する。

- InsuranceRate
- IncomeTaxBracket
- StandardSalary
- ResidentTaxMonthly

基準コード: main / `12c91a72b409df16b9d4be0b416247a07a8f170a`

**コード修正は行わない。**

---

## 2. InsuranceRate

Path:
`backend/src/main/java/com/project/backend/features/tax/entity/InsuranceRate.java`

Table:
`insurance_rate_master`

Fields:

```text
id             Long
insuranceType  InsuranceType
year           Integer
employeeRate   BigDecimal precision=6 scale=5
employerRate   BigDecimal precision=6 scale=5 nullable
```

### 実装事実

率は `BigDecimal` で保持される。

DB精度は `DECIMAL(6,5)` 相当なので、例として `0.12345` のような5桁小数を保持可能。

### 年度

`year` を明示保持する。

Detail Providerは `insuranceType + year` で取得する。

### Tenant

`InsuranceRate` は `BaseEntity` を継承しておらず、tenantId fieldもない。

**実装事実**: 保険率マスタは全Tenant共通のグローバル税制マスタとして設計されている。

### Version

version/revision/effectiveFrom/effectiveToはない。

年度が事実上のversion key。

### 未決事項

同一 `insuranceType + year` の複数行をDB unique constraintで禁止しているかはEntity上では確認できない。

---

## 3. IncomeTaxBracket

Path:
`backend/src/main/java/com/project/backend/features/tax/entity/IncomeTaxBracket.java`

Table:
`income_tax_table`

Fields:

```text
id          Long
year        Integer
minSalary   Integer
maxSalary   Integer
dependents  Integer
taxAmount   Integer
```

### 実装事実

所得税額は計算率ではなく、給与レンジ + 扶養人数に対応する整数金額として保持する。

### 金額型

すべてInteger。

したがって税額表自体には小数・丸め処理は存在しない。

### Tenant

BaseEntity非継承、tenantIdなし。

**実装事実**: 所得税額表も全Tenant共通。

### 年度

`year`あり。

過去年度行を削除しなければ複数年度を共存可能。

### Version

year以外のversion/effective periodなし。

### 境界仕様の未決事項

`minSalary` / `maxSalary` のinclusive/exclusive境界はEntityだけでは確定できない。

Rule/Catalogで参照する場合、この境界条件を明示仕様化する必要がある。

---

## 4. StandardSalary

Path:
`backend/src/main/java/com/project/backend/features/tax/entity/StandardSalary.java`

Table:
`standard_salary_table`

Fields:

```text
id              Long
minSalary       Integer
maxSalary       Integer
standardSalary  Integer
```

### 重要

**year / effectiveFrom / effectiveToが存在しない。**

Detail Providerも全件を `minSalary ASC` で読む。

### Tenant

tenantIdなし。

グローバルマスタ。

### 既知事項 — 過去再現性

標準報酬テーブルを将来更新した場合、旧テーブルを同一Table上で年度識別して保持できない。

過去給与再計算で当時の標準報酬表を再現するには現行モデルだけでは不足する可能性が高い。

**修正候補: 高**

候補:

```text
year
または
effectiveFrom / effectiveTo
または
masterVersion
```

を導入する。

---

## 5. ResidentTaxMonthly

Path:
`backend/src/main/java/com/project/backend/features/tax/entity/ResidentTaxMonthly.java`

Table:
`resident_tax_monthly`

`BaseEntity` 継承。

Fields:

```text
id          Long
employeeId  Long
fiscalYear  Integer
month       Integer  // 1〜12
taxAmount   Integer
```

Unique Constraint:

```text
tenant_id + employee_id + fiscal_year + month
```

### 実装事実

住民税は税率計算マスタではなく、従業員ごとの月額確定値を保存する。

### Tenant

BaseEntity由来tenant_idあり。

DB unique constraintにもtenant_idを含む。

4モデルのうち住民税だけTenant/Employee固有。

### 金額

Integerなので整数円。

### 過去保持

fiscalYear + monthを持つため、過去年度を保持可能。

---

## 6. ResidentTax RepositoryのTenant論点

`ResidentTaxMonthlyRepository` の主なmethod:

```text
findByFiscalYearOrderByEmployeeIdAscMonthAsc
findByEmployeeIdAndFiscalYearOrderByMonthAsc
findByEmployeeIdAndFiscalYearAndMonth
deleteByEmployeeIdAndFiscalYearAndMonth
deleteByFiscalYear
deleteByEmployeeIdAndFiscalYear
```

これらmethod名にはtenantId条件がない。

Entity/unique constraintにはtenant_idが存在するが、Repository query methodはtenant明示ではない。

### 未決事項 / 要追加確認

BaseEntity/Tenant Hibernate Filter等により自動tenant filteringされるかを共通Tenant設計で確認する必要がある。

**現時点ではRepository method名だけを根拠にtenant漏洩と断定しない。**

ただしRule Catalogからphysical tableを直接SELECTする場合はJPA/Hibernate filterを通らないため、`tenant_id` bindが必須になる。

---

## 7. 4モデルの比較

```text
InsuranceRate
  年度: あり
  Tenant: なし
  Employee: なし
  値: BigDecimal rate

IncomeTaxBracket
  年度: あり
  Tenant: なし
  Employee: なし
  値: Integer taxAmount

StandardSalary
  年度: なし
  Tenant: なし
  Employee: なし
  値: Integer standardSalary

ResidentTaxMonthly
  年度: fiscalYearあり
  Tenant: あり
  Employee: あり
  値: Integer taxAmount
```

---

## 8. Rule Catalogから参照する場合

### InsuranceRate

必要条件例:

```text
insurance_type = :insuranceType
year = :targetYear
```

Tenant bind不要のグローバルマスタ。

### IncomeTaxBracket

必要条件例:

```text
year = :targetYear
:minSalary <= salary
:maxSalary >= salary
dependents = :dependents
```

ただし境界inclusive/exclusiveは正式仕様確認が必要。

### StandardSalary

```text
minSalary / maxSalary
```

のみで現在表は引けるが、過去年度指定ができない。

### ResidentTaxMonthly

最低限:

```text
tenant_id = :tenantId
employee_id = :employeeId
fiscal_year = :fiscalYear
month = :month
```

が必要。

---

## 9. Rule CatalogとTenant

Rule DataSourceがJdbcTemplate等でphysical tableを読む構造では、JPA EntityのTenant機構は期待できない。

したがってResidentTaxMonthlyをCatalog登録する場合:

```text
WHERE tenant_id = :tenantId
  AND employee_id = :employeeId
  ...
```

をCatalogのwhereClauseで保証する必要がある。

これは以前確認した `whereClause` / parameter bind設計と直結する。

**修正候補**: Tenant scoped tableにはCatalog metadataで `tenantScoped=true` 等を持たせ、tenant predicateを自動注入する方式もV2候補。

---

## 10. employeeId指定について

ResidentTaxMonthlyは明確にemployeeIdを持つため、以前の質問:

> employeeId的な指定もwhereClauseでできるか

に対する具体例になる。

Rule parameterとしてemployeeIdを渡し、Catalog WHEREでbindすれば従業員別住民税をFact化できる構造。

---

## 11. targetDate → 年度変換

現行Detail ProviderではClock/current dateからyear/fiscalYearを決める。

Ruleで過去給与を再計算するなら、現在Clockではなく:

```text
targetDate
→ targetYear / fiscalYear
```

を決定する方が再現性が高い。

この変換はユーザー提案の `RuleDateFunctions` に置く候補もある。

例概念:

```text
dates.year(targetDate)
dates.residentTaxFiscalYear(targetDate)
```

ただし `residentTaxFiscalYear` は日本給与ドメイン依存なので、完全汎用Function libraryではなくPayroll/Tax Calculator側へ置く選択肢も強い。

---

## 12. Money / Precision

### InsuranceRate

BigDecimal scale=5。

例えば:

```text
baseAmount * employeeRate
```

では中間値が小数円になる。

したがって最終 `HALF_UP` だけでなく、その保険制度が要求する端数規則をCalculator/Rule側で明示する必要がある。

### IncomeTax / ResidentTax / StandardSalary

DB値自体はIntegerなのでデータ取得後のscale問題は小さい。

ただしStandardSalary × insuranceRateでは再びBigDecimal精度問題が発生する。

---

## 13. Versioning修正候補

前回のversion候補にTax masterを追加する。

```text
ruleVersion
catalogVersion
calculatorVersion
functionLibraryVersion
taxMasterVersion/effectiveYear
```

特にStandardSalaryはyearを持たないため優先度が高い。

### Execution Snapshot候補

```text
ruleName
ruleVersion
ruleHash
catalogVersion
calculatorVersion
functionLibraryVersion
taxDataYear
taxDataVersion
```

---

## 14. 現在コードで良い点

- 保険率がBigDecimal
- 保険率・所得税表に年度がある
- 税率そのものをJava定数化していない
- 住民税がtenant + employee + fiscalYear + monthでDB unique
- 過去住民税を年度別保持できる

---

## 15. 修正候補・未決事項

### 高: StandardSalaryに年度/有効期間がない

過去再現性。

### 高: Rule CatalogでTenant table参照時のtenant predicate保証

JPA filterに依存できない。

### 高: Tax master versionを給与計算結果へ記録する仕組みがない

監査/再現性。

### 中: IncomeTaxBracketの給与レンジ境界仕様

inclusive/exclusiveを明文化する。

### 中: InsuranceRateのtype+year uniqueness

Entity上ではunique constraintなし。

### 中: targetDate基準とClock基準の統一

過去給与・未来試算。

### 中: 税/保険ごとの端数Policy

最終HALF_UPだけに依存しない。

---

## 16. 次の解析

次は給与集計側へ進み、**控除の正数amountがどこで総支給から差し引かれるか**を追う。

```text
PayrollItemCalculationResult
→ 月次給与計算
→ allowance合計
→ deduction合計
→ gross / taxable / net
→ 保存Entity
```

ここでRule計算結果が最終給与へどう反映され、どの時点のFact/金額が保存されるかを確認する。
