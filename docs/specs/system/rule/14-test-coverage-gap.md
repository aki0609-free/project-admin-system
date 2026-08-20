# system/rule 詳細設計 14 — テスト保証範囲と不足テスト

## 1. 方針

コード修正は行わない。

基準コード: main `12c91a72b409df16b9d4be0b416247a07a8f170a`

Rule機能について「実装がある」ことと「テストで仕様が保証されている」ことを分離して整理する。

---

## 2. Rule test tree

現行Rule test package:

`backend/src/test/java/com/project/backend/features/system/rule/service/`

直下:

- `RuleDataSourceCatalogServiceTest`
- `RuleMasterCommandServiceTest`

subpackage:

- converter
- executor
- fetcher
- validation

Rule専用test package自体は存在する。

---

## 3. JexlDslExecutorTest

Path:
`backend/src/test/java/com/project/backend/features/system/rule/service/executor/JexlDslExecutorTest.java`

確認できるtest:

### execute_shouldCalculateUsingFacts

DSL:

```text
hours * rate
```

Facts:

```text
hours = 2
rate = BigDecimal("1500")
```

Expected:

```text
BigDecimal("3000")
```

### 保証していること

- FactsをJEXL DSLから参照可能
- Integer × BigDecimalの基本演算
- 結果がBigDecimalとして得られる基本ケース

### execute_shouldRejectUndefinedVariableInStrictMode

未定義 `missingValue` を使用するとRuntimeException。

### 保証していること

- JEXL strict modeで未定義変数を黙ってnull扱いしない

---

## 4. JEXLで不足しているテスト

現時点で専用testとして確認できない重要項目:

- 小数同士の加減乗除
- 除算scale/precision
- 0除算
- null
- boolean/条件分岐
- Map/Listアクセス
- parametersとfactsの同名key競合
- 非数値result
- very large BigDecimal
- negative result
- RuleFunctions導入時のfunction呼出
- sandbox/securityで禁止すべきmethod/classアクセス
- timeout/巨大expression

特に給与用途では除算・scale・roundingのテストが重要。

---

## 5. GeneralDataFetcherTest

Path:
`backend/src/test/java/com/project/backend/features/system/rule/service/fetcher/GeneralDataFetcherTest.java`

現行test:

`fetch_shouldUseCatalogAndForceTenantParameter`

Catalog:

```text
physicalName = vw_rule_employee_basic
where = tenant_id = :tenantId AND employee_id = :employeeId
tenantScopedFlag = true
```

呼出parameterには意図的に:

```text
tenantId = spoofed
employeeId = 10
```

を渡す。

TenantContext:

```text
tenant-a
```

Expected SQL parameter:

```text
tenantId = tenant-a
employeeId = 10
__ruleLimit = 2
```

### 非常に重要な保証

現行実装は既に、呼出元parameterのtenantIdを信用せず `TenantContext` の値で上書きすることをtestしている。

したがってTenant source of truthは現在でもTenantContext側にある。

---

## 6. Tenant設計についての新しい評価

前回「tenantIdをRule parameterとして持ちたくない」という方針をV2候補にしたが、現行コードは安全性の一部を既に実装している。

現在:

```text
whereClauseTemplateには :tenantId が必要
↓
parametersにspoofed tenantIdがあっても
↓
GeneralDataFetcherがTenantContextで強制上書き
```

つまり:

### 現行の良い点

**tenant値そのものをRule callerが選択することはできない。**

### 改善したい点

Rule/Catalog作者はまだ:

```text
tenant_id = :tenantId
```

をwhereClauseTemplateへ書く必要がある。

V2候補はこの記述自体も消し、tenantScopedFlag/RegistryからSQL predicateを自動注入すること。

これは「セキュリティ修正」というより「安全性をさらに基盤側へ寄せ、設定漏れを防ぐ改善」。

---

## 7. Tenantで不足しているtest

重要不足候補:

```text
tenantScopedFlag=true
+ TenantContextなし
→ Fail Closed
```

```text
tenantScopedFlag=true
+ whereClauseTemplateにtenant predicateなし
→ 登録時/実行時拒否
```

またはV2では:

```text
tenant predicate自動注入
```

を保証するtest。

さらに:

- GLOBAL sourceではtenant不要
- spoofed tenantがSQLへ漏れない
- JOIN/VIEW時のtenant scope
- list/single-row双方
- 複数DataSourceを1Ruleで読む場合

が必要。

---

## 8. RuleDataSourceCatalogServiceTest

Rule test tree上に存在。

Catalog Service単体testがあるためCatalog管理層は無テストではない。

ただし今回の段階では全assertionを逐語確認していないため、詳細保証範囲は次の追加確認対象。

---

## 9. RuleMasterCommandServiceTest

Rule Master command service testも存在。

Rule CRUD/validation側に一定の単体testがあることは確認。

ただしversion増分、DSL変更、参照整合性等の詳細保証は追加確認対象。

---

## 10. PayrollItem系test

`features/system/rule` test packageには:

- PayrollItemValueServiceTest
- PayrollItemCalculationServiceTest
- AllowancePayrollItemValueProviderTest
- DeductionPayrollItemValueProviderTest

は確認できない。

別feature test packageに存在する可能性はあるため、完全な不存在断定は次回tree追加探索後とする。

---

## 11. 現時点でテスト保証されている重要仕様

### 保証あり

1. JEXLでFactsを使った基本計算
2. Integer × BigDecimalの基本計算
3. strict modeで未定義変数を例外化
4. GeneralDataFetcherがCatalogを使ってSQL生成
5. TenantContextが呼出parameterの偽tenantIdを上書き
6. singleRow時に内部limitを設定するケース

### 実装事実だが今回確認したtest保証なし

- Rule result→BigDecimal変換
- min/max clamp
- 負数拒否
- final setScale(0, HALF_UP)
- manual override
- AUTO→RuleExecutionService
- employeeId等上位parameter透過
- tax master年度選択
- ResidentTax fiscalYear 6月境界
- Rule→Payslip persistence

---

## 12. 給与計算で優先度が高い不足テスト

### P0

- AUTO Rule結果がPayrollItemCalculationResultまで届く
- BigDecimal小数result
- min境界 / max境界 / 超過clamp
- negative result reject
- HALF_UPの0.49 / 0.50 / -値方針
- manual override allowed/not allowed
- Rule例外時の給与項目計算挙動

### P0 Tenant

- TENANT sourceでTenantContextなし→拒否
- Tenant predicate設定漏れ検出、または自動注入

### P1

- employeeId bind
- targetDate bind
- 過去年月tax master選択
- ResidentTax 5月/6月境界
- StandardSalary境界
- IncomeTaxBracket境界

### P1 Version

- rule更新後に過去計算再現可能か
- catalog変更後の再現
- function library変更後の再現

---

## 13. RuleFunctions導入時に必須となるtest

提案済みの責務別Functions:

```text
money
math
dates
checks
```

について各functionはpure function testを持つ。

Money例:

```text
round(value, scale, mode)
roundYen
floorYen
ceilYen
```

必須境界:

```text
1.4 / 1.5 / 1.6
-1.4 / -1.5 / -1.6
0
非常に大きい値
非常に小さい値
```

RoundingModeごとの差を固定test化する。

---

## 14. Version test候補

Version機能を導入する場合:

```text
同じruleName
version=1 DSL=A
version=2 DSL=B
```

で、過去execution snapshotがversion=1を指した場合にAを再現できることをIntegration Testにする。

Catalog/Calculator/Functionsも同様。

---

## 15. 現時点の評価

Rule基盤にはテストが全くないわけではない。

特に:

```text
JEXL strict mode
Tenant spoofing防止
```

は明示的にtestされており良い。

一方、給与計算エンジンとして見ると:

```text
精度
丸め
境界
Rule失敗
version
過去再現
給与永続化
```

の保証がまだ薄い。

したがって現在のtest suiteは:

```text
Rule infrastructureの基本動作保証
```

としては存在するが、

```text
給与計算の法的・金額的正確性を保証するsuite
```

にはまだ達していない。

---

## 16. 次

次回は残りのRule test package:

- converter
- validation
- RuleDataSourceCatalogServiceTest
- RuleMasterCommandServiceTest

を細かく読み、Catalog登録時に何を防げているか、DSL/型/物理名/whereClauseのvalidationがどこまでtestされているかを確認する。
