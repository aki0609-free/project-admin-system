# system/rule 詳細設計 10 — 控除からRule実行・税/保険詳細Providerとの関係

## 1. 対象範囲

今回は `DeductionMaster.ruleName` から共通給与項目計算へ接続する経路と、控除固有の税・保険Detail Providerを確認する。

**この調査ではコード修正を行わない。**

---

## 2. 控除のRule接続

`DeductionPayrollItemValueProvider` は `AllowancePayrollItemValueProvider` と同型で、`DeductionMaster` を `PayrollItemMasterSnapshot` へ変換する。

Snapshotへ渡す主項目:

- targetType = DEDUCTION
- id
- deductionCode
- deductionName
- calculationType
- ruleName
- defaultAmount
- minAmount
- maxAmount
- allowManualInput
- displayOrder

したがって calculationType=AUTO の控除は共通 `PayrollItemValueService` を通じて `RuleExecutionService` を実行する。

---

## 3. Tenant境界

控除マスタ取得は:

- `findByIdAndTenantIdAndDeletedAtIsNull`
- `findByTenantIdAndDeductionCodeAndDeletedAtIsNull`

を使用する。

**確定仕様**: 控除マスタの給与項目参照はtenantId明示Query。

---

## 4. DAILY / MONTHLY / PAYROLL / BONUS

`DeductionPayrollItemValueProvider#findItems()`:

### DAILY

- DeductionUnit.DAILY / BOTH
- showOnDailyStatement=true
- enabled=true
- deletedAt is null

### MONTHLY

- showOnMonthlyStatement=true
- enabled=true
- deletedAt is null

### PAYROLL / BONUS

現行Providerでは空List。

---

## 5. 控除AUTO計算

共通 `PayrollItemValueService` により:

```text
MANUAL → manualAmount
FIXED  → defaultAmount
AUTO   → RuleExecutionService.execute(ruleName, parameters)
```

となる。

控除だけの特別なRule Executorは存在しない。

---

## 6. Ruleへ渡す共通Parameter

控除AUTO Ruleにも手当と同じ給与項目共通Parameterが付与される。

```text
上位parameters
+ targetType=DEDUCTION
+ targetMasterId
+ targetCode
+ manualAmount（存在時）
```

employeeId / targetDate等を上位から渡せばそのままRuleへ届く。

---

## 7. 控除金額の符号

共通 `PayrollItemValueService#applyLimit()` は負数を拒否する。

したがって控除額も「-10000円」ではなく「10000円の控除額」という正数として扱う設計。

**確定仕様（現行共通計算）**: 控除金額自体は0以上。

給与総額から減算する処理は別の集計層の責務と考えられる。

---

## 8. 上下限制御

控除も手当と同じく:

```text
Rule result
→ BigDecimal化
→ 負数拒否
→ minAmount clamp
→ maxAmount clamp
```

を通る。

上下限超過はErrorではなくclamp。

---

## 9. 最終円単位丸め

`PayrollItemCalculationService` の共通処理により最終 `calculatedAmount` は:

```java
setScale(0, RoundingMode.HALF_UP)
```

となる。

**確定仕様**: 控除も現行給与項目計算では最終的に円未満四捨五入。

ただし税・保険の法定端数処理が途中計算で必要な場合、Rule/Calculator内部で別途処理しない限りこの最終HALF_UPだけでは足りない。

---

## 10. 控除固有Detail Provider

控除master機能にはRule計算とは別に以下のDetail Providerがある。

- `EmploymentInsuranceDeductionDetailProvider`
- `IncomeTaxDeductionDetailProvider`
- `InsuranceRateDeductionDetailProvider`
- `ResidentTaxDeductionDetailProvider`
- `StandardSalaryDeductionDetailProvider`

これらは主に控除詳細画面へ税・保険関連マスタ情報を返す役割。

---

## 11. 雇用保険Detail

`EmploymentInsuranceDeductionDetailProvider`:

```text
Clockから現在年を取得
→ InsuranceRateRepository
→ InsuranceType.EMPLOYMENT_INSURANCE + year
```

を検索する。

**実装事実**: 雇用保険率はJava定数ではなくDBマスタから年度指定で取得する構造。

---

## 12. 健康保険Detail

`InsuranceRateDeductionDetailProvider`:

```text
Clockから現在年
→ InsuranceRateRepository
→ InsuranceType.HEALTH_INSURANCE + year
```

**実装事実**: 健康保険率も年度別DBマスタ。

---

## 13. 所得税Detail

`IncomeTaxDeductionDetailProvider`:

```text
LocalDate.now(clock).getYear()
→ IncomeTaxBracketRepository
→ year
→ minSalary ASC, dependents ASC
```

**実装事実**: 所得税表も年度別DBマスタ。

---

## 14. 住民税Detail

`ResidentTaxDeductionDetailProvider` は年度を:

```text
6月〜12月 → 当年
1月〜5月 → 前年
```

として計算する。

実装式:

```text
fiscalYear = currentYear - (month < 6 ? 1 : 0)
```

その年度のResidentTaxMonthlyを:

```text
employeeId ASC, month ASC
```

で取得する。

**確定仕様（詳細表示）**: 住民税年度切替境界は6月。

---

## 15. 標準報酬Detail

`StandardSalaryDeductionDetailProvider`:

```text
StandardSalaryRepository.findAllByOrderByMinSalaryAsc()
```

年度条件は付けていない。

**未決事項**: StandardSalary Entity側に有効期間等があるか、別途確認が必要。

---

## 16. Detail ProviderとRule計算は現時点で別系統

今回確認したDetail Providerは:

```text
Deduction Master詳細表示
→ 税/保険DBマスタを表示
```

する役割であり、`PayrollItemValueService` から直接呼ばれてはいない。

したがって現時点では:

```text
控除AUTO計算 → RuleExecutionService
税率/税表表示 → Detail Provider
```

が分かれている。

**重要**: 実際のAUTO RuleがDataSource Catalog経由でこれら税テーブルを参照しているかは、実Rule/Catalog DB定義確認が必要。

---

## 17. ハードコード評価

### 業務値そのもの

今回確認範囲では:

- 雇用保険率
- 健康保険率
- 所得税額表

をJava数値定数で固定していない。

DBマスタ参照なので汎用性は高い。

### Javaに固定されている業務ルール

- ResidentTax fiscalYearの6月境界
- Detail View TypeとProvider対応
- InsuranceType.EMPLOYMENT_INSURANCE / HEALTH_INSURANCE
- 現在年をClockから使う

特に住民税6月境界は業務ルールのJavaハードコード。

---

## 18. targetDateとの不一致可能性

Detail Providerは多くが `Clock` の「現在年」を使う。

一方Rule実行Contextでは `targetDate` をParameterとして渡せる。

過去給与再計算時に:

```text
targetDate = 過去年月
現在Clock = 2026
```

でもDetail Providerは現在年度を表示する。

**既知事項 — 再現性**: Detail表示側の年度基準がtargetDateではなく現在Clock。

Rule計算側がCatalogでtargetDate bindを使えば別年度を選べるが、両者が一致する保証はない。

---

## 19. Versioning候補との関係

税・保険は年度依存が明確なため、最終V2案では:

- ruleVersion
- catalogVersion
- calculatorVersion
- functionLibraryVersion
- taxMasterVersion/effectiveYear

までexecution snapshot候補として扱う。

---

## 20. 金額計算拡張候補との関係

控除では法定端数規則が項目ごとに異なる可能性が高い。

したがって現行の最終:

```text
setScale(0, HALF_UP)
```

だけを全控除の唯一の丸め仕様とするのは危険。

V2候補:

```text
MoneyRoundingPolicy
CalculationPrecisionPolicy
```

をRule/Calculator単位で明示する。

---

## 21. 現時点の重要既知事項

### A. AUTO控除もRuleExecutionServiceへ接続済み

確定。

### B. 控除額は正数として扱う

確定。

### C. 最終円単位HALF_UP

共通給与項目計算の現行仕様。

### D. 税率・所得税表はDBマスタ

良い点。

### E. 住民税年度6月境界はJavaハードコード

修正候補/仕様化候補。

### F. Detail Providerは現在Clock基準

過去再計算表示とのズレ候補。

### G. 税/保険Detail ProviderとAUTO Rule計算は現時点で別系統

実Rule DB定義確認が必要。

---

## 22. 次に掘る範囲

次は `tax` 配下そのものを深入りしすぎず、**Rule計算から利用されうる税・保険データモデルだけ**を整理する。

対象:

- InsuranceRate
- IncomeTaxBracket
- StandardSalary
- ResidentTaxMonthly

確認内容:

- 年度/有効期間
- employeeId依存
- rate型/scale
- amount型
- unique/index
- Tenant有無
- 過去年度保持
- Rule Catalogで安全に参照できる形か

その後、給与集計側で控除額を実際にどう差し引くかへ進む。
