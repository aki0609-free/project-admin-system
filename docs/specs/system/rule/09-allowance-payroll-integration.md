# system/rule 詳細設計 09 — 手当からRule実行・給与項目金額への反映

## 1. 対象範囲

今回は `system/rule` を手当側の実利用へ接続し、以下を追う。

```text
AllowanceMaster.ruleName
→ AllowancePayrollItemValueProvider
→ PayrollItemValueService
→ RuleExecutionService
→ Rule result
→ BigDecimal化
→ min/max
→ 円単位丸め
→ PayrollItemCalculationResult
```

基準コード:

- branch: `main`
- commit: `12c91a72b409df16b9d4be0b416247a07a8f170a`

**この調査ではコード修正を行わない。**

---

## 2. 手当マスタはRule名を給与項目Snapshotへ渡す

`AllowancePayrollItemValueProvider#toSnapshot()` は `AllowanceMaster` から以下を `PayrollItemMasterSnapshot` へ渡す。

- targetType = ALLOWANCE
- id
- allowanceCode
- allowanceName
- calculationType
- ruleName
- defaultAmount
- minAmount
- maxAmount
- allowManualInput
- displayOrder

**実装事実**: 手当マスタの `ruleName` は給与項目計算共通層まで運ばれる。

---

## 3. 手当の取得Tenant境界

`AllowancePayrollItemValueProvider#findMaster()` は `TenantContext.getTenantId()` を取得し、IDまたはcode検索のどちらでもtenantIdをRepository条件へ明示する。

例:

```text
findByIdAndTenantIdAndDeletedAtIsNull
findByTenantIdAndAllowanceCodeAndDeletedAtIsNull
```

**確定仕様**: 手当マスタの給与項目参照はtenant明示Query。

---

## 4. DAILY / MONTHLY対象

`AllowancePayrollItemValueProvider#findItems()`:

### DAILY

対象AllowanceUnit:

- DAILY
- BOTH

かつ:

- showOnDailyStatement=true
- enabled=true
- deletedAt is null

### MONTHLY

- showOnMonthlyStatement=true
- enabled=true
- deletedAt is null

### PAYROLL / BONUS

現行Providerでは:

```text
List.of()
```

を返す。

**実装事実**: このProviderのqueryType一覧取得上、手当はDAILY/MONTHLYで列挙され、PAYROLL/BONUSでは列挙されない。

このqueryTypeの業務上の意味はpayrollitem側詳細設計で別途確認する。

---

## 5. 計算方式

`PayrollItemValueService#calculate()` は `master.calculationType()` で3分岐する。

```text
MANUAL
FIXED
AUTO
```

### MANUAL

`manualAmount()`。

`allowManualInput=true` 必須。

Request manualAmountがnullなら0。

### FIXED

`defaultAmount` を金額にする。

nullなら0。

### AUTO

`executeRule()` でsystem/ruleを実行。

**確定仕様**: system/ruleが実際に利用されるのは給与項目 calculationType=AUTO の経路。

---

## 6. AUTO時のRule実行

`PayrollItemValueService#executeRule()`:

```text
master.ruleName blank
→ IllegalStateException

ruleNameあり
→ RuleExecutionService.execute(ruleName, RuleContextRequest)
```

Rule実行失敗はcatchして:

```text
IllegalStateException(
  "給与項目のRule計算に失敗しました。code=..., ruleName=...",
  cause
)
```

へwrapする。

**実装事実**: 業務側ではRule内部例外をそのまま公開せず、給与項目code/ruleNameの文脈を付けて再throwする。

---

## 7. Ruleへ渡すParameter

`buildRuleParameters()` はまずRequestの `parameters` を全件copyする。

その後、給与項目共通側が以下を強制追加/上書きする。

```text
targetType
 targetMasterId
 targetCode
```

manualAmountがあれば:

```text
manualAmount
```

も追加。

### 例

呼出元が:

```text
employeeId=123
targetDate=2026-08-01
hours=8
```

を渡していた場合、Allowance AUTO Ruleへ最終的に:

```text
employeeId
targetDate
hours
targetType=ALLOWANCE
targetMasterId={AllowanceMaster.id}
targetCode={allowanceCode}
[manualAmount]
```

が渡る。

**確定仕様**: employeeId等の業務Parameterは上位から渡せる。手当共通層はそれを保持したままRuleへ引き渡す。

---

## 8. Parameter上書き優先順位

`buildRuleParameters()` の順序:

```text
1. request.parameters putAll
2. targetType put
3. targetMasterId put
4. targetCode put
5. manualAmount put
```

したがって呼出元が同名keyをparametersに入れていても、給与項目共通層の値で上書きされる。

**確定仕様**: targetType / targetMasterId / targetCode / manualAmountは給与項目層が正。

---

## 9. Rule戻り値→BigDecimal

AUTO Ruleの `RuleExecutionResult.result()` はObject。

`PayrollItemValueService#toBigDecimal()` で金額へ変換する。

### null

```text
BigDecimal.ZERO
```

### BigDecimal

そのまま。

### Number

```text
number.doubleValue()
→ finite check
→ BigDecimal.valueOf(doubleValue)
```

### その他

```text
new BigDecimal(String.valueOf(value))
```

---

## 10. Number→double経由の精度注意

Rule resultがBigDecimal以外のNumberの場合、一度double化してからBigDecimalへ変換する。

例:

```text
Long / Integerは通常整数範囲で問題が小さい
Float / Doubleはbinary floating point精度の影響を受ける
```

**既知事項 — 金額精度**: 金額RuleはBigDecimalを直接返す方が安全。

V2 Calculator contractでは金額Rule result型をBigDecimalへ固定する候補を記録する。

---

## 11. 負数禁止

Rule/Manual/FIXEDの計算後 `applyLimit()` で:

```text
result.signum() < 0
```

なら:

```text
IllegalArgumentException("給与項目の計算結果は0以上である必要があります。")
```

**確定仕様**: PayrollItemValueServiceを通る手当/控除等の共通給与項目金額は負数を許可しない。

控除側で正の「控除額」として保持する設計と推測されるが、控除解析で確定する。

---

## 12. minAmount / maxAmount

`PayrollItemValueService#applyLimit()`:

```text
amount < minAmount → minAmount
amount > maxAmount → maxAmount
```

つまりErrorではなくclamp。

順番:

```text
Rule result
→ BigDecimal化
→ 非負数確認
→ min clamp
→ max clamp
```

**確定仕様**: Rule式の生結果が上下限外でも、給与項目層で最終範囲へ補正する。

---

## 13. 最終円単位丸め

`PayrollItemCalculationService#calculateOne()` で `PayrollItemValueService` の結果をさらに:

```java
valueResult.amount().setScale(0, RoundingMode.HALF_UP)
```

する。

nullなら0。

**確定仕様（現行給与項目計算）**: 最終 `calculatedAmount` は小数0桁、HALF_UP。

つまり円未満四捨五入。

### 重要

system/rule engine自体には丸めcontractがないが、PayrollItemCalculationServiceでは最終金額を円単位HALF_UPへ固定している。

---

## 14. 丸めのタイミング

順序は:

```text
Rule DSL/Calculator内部計算
→ Object result
→ BigDecimal化
→ min/max clamp
→ PayrollItemValueResult.amount
→ setScale(0, HALF_UP)
→ calculatedAmount
```

つまりmin/max比較は**丸め前の値**に対して行う。

### 例

```text
Rule result = 99.6
minAmount = 100
```

PayrollItemValueServiceで先に100へclamp。
その後HALF_UPして100。

別例:

```text
Rule result = 100.4
maxAmount = 100
```

先に100へclamp。その後100。

**計算仕様上重要**: `round → clamp` ではなく `clamp → round`。

---

## 15. 手入力Override

`PayrollItemCalculationService` は最終段でmanual overrideを持つ。

条件:

```text
calculationTypeがAUTOまたはFIXED
AND allowManualInput=true
AND manualAmountsに対象idが存在
```

trueの場合:

```text
amount = manual inputへmin/max clamp
```

を使う。

### 重要

`calculatedAmount` 自体はRule/FIXEDの円単位HALF_UP結果を保持する。

`amount` はmanual override結果になる。

つまりResultは:

```text
calculatedAmount = 自動算出値
amount           = 実採用値
manualOverride   = true/false
```

を区別する。

---

## 16. Manual override側の丸め

manualAmountsの型は:

```text
Map<Long,Integer>
```

なので最初から整数円。

`BigDecimal.valueOf(integer)` → min/max clamp。

追加setScaleは不要な値域。

---

## 17. AUTO RuleへmanualAmountを渡す挙動

`resolveManualAmount()` は `allowManualInput=true` の場合:

- manualAmountsにidあり → その値
- なければ `snapshot.defaultAmount()`

を `PayrollItemValueRequest.manualAmount` へ渡す。

その後AUTO Ruleでも `buildRuleParameters()` がmanualAmountをfactsへ入れる。

**実装事実**: AUTO Ruleは `manualAmount` をParameterとして参照可能。

manual overrideが実際に指定されていない場合でも、allowManualInput=trueならdefaultAmountがmanualAmountとしてRuleへ渡る場合がある。

**未決事項**: AUTO RuleへdefaultAmountを `manualAmount` 名で渡す意味が業務仕様として意図されたものか。

---

## 18. Factsの返却

AUTOの場合:

```text
RuleExecutionResult.facts
```

をPayrollItemValueResultへ持ち越す。

MANUAL/FIXEDの場合:

```text
request.parameters
+ targetType
+ targetMasterId
+ targetCode
+ manualAmount
```

のbase factsを作る。

さらにPayrollItemCalculationServiceで:

```text
displayOrder
allowManualInput
```

をfactsへ追加する。

**実装事実**: 給与項目計算結果にもRule debug/context情報がかなり保持される。

---

## 19. 手当とRuleの適用場所

前回までの「適用場所はRule側で決めない」という理解が、手当実装で確認できた。

```text
AllowanceMaster
  ↓ ruleNameを持つ
PayrollItemValueService
  ↓ calculationType=AUTOならfire
system/rule
```

RuleMaster自身はAllowanceMaster IDを知らない。

**確定設計**: 適用場所は業務側が決める。

---

## 20. PayrollItemValueProviderもPlugin構造

`PayrollItemValueService` constructorは:

```text
List<PayrollItemValueProvider>
→ supports()をkeyにMap化
```

する。

Allowance側は:

```text
supports() = ALLOWANCE
```

を返す。

**実装事実**: 給与項目側にもRule Executorと同じPlugin/Strategyパターンが既に存在する。

これはV2汎用化で参考になる。

---

## 21. RuleUtils / RuleFunctions案

ユーザー提案:

> RuleのFactにRuleUtilsのようなobjectを入れ、汎用判定ロジックを追加してDSLから呼ぶ。

**方向性は有効。**

ただし巨大な万能Utils 1個に集約するより、責務別の限定Function APIを推奨する。

候補:

```text
facts["math"]   → RuleMathFunctions
facts["money"]  → RuleMoneyFunctions
facts["dates"]  → RuleDateFunctions
facts["checks"] → RuleCheckFunctions
```

例DSL概念:

```text
money.roundYen(amount * rate)
dates.isBetween(targetDate, from, to)
checks.isBlank(value)
```

---

## 22. RuleFunctionsの設計原則

DSLへ露出するFunction objectは以下を守る。

### 副作用なし

- DB更新しない
- Repository呼ばない
- HTTP呼ばない
- filesystem触らない

### Domain-neutral

避ける:

```text
isHousingAllowanceEligible(employeeId)
calculateIncomeTax(...)
```

推奨:

```text
between(value,min,max)
round(value,scale,mode)
isDateInRange(...)
coalesce(...)
```

Domain固有計算はRuleCalculator/JavaBeanへ置く。

### Deterministic

現在時刻を直接読む関数をなるべく避ける。
必要ならClock/targetDateをFactで明示する。

### Allowlist

DSLから呼んでよいmethodを限定・契約化する。

---

## 23. JEXL/MVEL安全性との関係

RuleFunctions objectをfactsへ入れると、DSLに「method call能力」を明示的に提供することになる。

JEXLはRESTRICTED permissionsがあるため、カスタムFunction objectのmethodが許可されるか実験/testが必要。

MVELはより自由度が高いため、Objectを入れるほど攻撃面が広がる。

**V2候補**: MVELへ任意Java objectを直接渡すより、許可Function専用Facade + sandbox testを用意する。

---

## 24. Function versioning

RuleUtils/Functionsを追加・変更すると、**同じDSL textでも計算結果が変わる可能性**がある。

例:

```text
money.roundYen(x)
```

のimplementation変更。

したがってRule versionだけでなくFunction API versionも再現性へ影響する。

V2では少なくとも:

- ruleVersion
- catalogVersion
- calculatorVersion
- functionLibraryVersion

を検討対象にする。

---

## 25. Versioning修正候補

ユーザー要望により明示記録。

### V-01 Rule version

RuleMaster変更ごとにversion/revisionを持つ候補。

### V-02 Catalog version

physicalName / where / columns / maxRows変更をversion管理。

### V-03 Calculator version

Java Calculator実装の業務version。

例:

```text
INCOME_TAX / 2026
INCOME_TAX / 2027
```

### V-04 Function Library version

RuleFunctionsの意味論変更を追跡。

### V-05 Execution snapshot

給与結果へ:

```text
ruleName
ruleVersion
dslType
ruleHash
catalogVersions
calculatorCode
calculatorVersion
functionLibraryVersion
```

を記録する候補。

**重要度: 高。**

過去給与再現・監査・法改正対応に必要。

---

## 26. 金額計算拡張候補との接続

`PayrollItemCalculationService` には現在:

```text
setScale(0, HALF_UP)
```

がハードコードされている。

これは現行の「円未満四捨五入」仕様。

V2汎用化では:

```text
MoneyRoundingPolicy
CalculationPrecisionPolicy
```

へ外出し候補。

例えば:

```text
FINAL_YEN_HALF_UP
FINAL_YEN_DOWN
FINAL_YEN_UP
SCALE_2_HALF_UP
NO_FINAL_ROUNDING
```

等をmetadataとして持てる。

ただし法定計算では中間丸めと最終丸めが異なるため、単純Enum 1個では不足する可能性がある。

---

## 27. 現時点の重要既知事項

### A. AUTO手当は実際にRuleExecutionServiceを使っている

確定。

### B. employeeId等を上位ParameterからRuleへ渡せる

確定。

### C. targetType/targetMasterId/targetCodeは給与項目層が強制付与

確定。

### D. 金額は最終的に円単位HALF_UP

現行PayrollItemCalculationServiceの確定実装。

### E. clampはround前

計算仕様上重要。

### F. Rule Number resultのBigDecimal化にdouble経由がある

精度注意。

### G. RuleUtils案は有効だがFunction Facade分割推奨

V2候補。

### H. versioningはRuleだけでなくCatalog/Calculator/Functionsまで検討

修正候補として正式記録。

---

## 28. 次に掘る範囲

次は **控除側** を同じ粒度で追う。

```text
DeductionMaster.ruleName
→ DeductionPayrollItemValueProvider
→ PayrollItemValueService
→ RuleExecutionService
→ min/max
→ round
→ deduction固有Provider
```

特に控除側には:

- 雇用保険
- 所得税
- 社会保険/標準報酬
- 住民税

等のDetail Providerが存在するため、Ruleとは別にJavaへ計算ロジックがハードコードされているかを確認する。

ここからは率・端数・上限下限・法定計算をかなり細かく記録する。
