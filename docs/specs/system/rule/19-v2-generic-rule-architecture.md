# system/rule 詳細設計 19 — V2汎用Rule Architecture / 修正候補総まとめ

## 0. この文書の目的

この文書は、`system/rule` の現行V1コードを基準に、これまでの調査で判明した実装事実・未決事項・既知問題を統合し、V2でどこを・なぜ・どう変更するかを具体化する。

今回は実装コードを変更しない。

基準コード:

```text
branch: main
commit: 12c91a72b409df16b9d4be0b416247a07a8f170a
```

設計書は `agent/v1-common-architecture-spec` に追加する。

分類:

- **確定仕様**: 現行コードおよびテストから仕様として確定できるもの
- **実装事実**: 現在コードがそう動くこと
- **推測**: 現時点で断定しないもの
- **未決事項**: 仕様判断が必要なもの
- **修正候補**: V1改善/V2移行候補
- **V2候補**: 将来の汎用化設計

---

# 1. V1の現在地

V1はすでに次の基盤を持つ。

```text
RuleMaster
├─ Parameters
├─ DataSources
├─ Columns
├─ dslType
│   ├─ JEXL
│   ├─ MVEL
│   └─ JAVA_BEAN
├─ dslText / ruleBeanName
├─ resultFactKey
└─ activeFlag

RuleExecutionService
├─ RuleLoader
├─ RuleParameterResolver
├─ RuleFactBuilder
├─ GeneralDataFetcher
├─ DslExecutorDispatcher
└─ RuleExecutionResult
```

さらに業務側では:

```text
AllowanceMaster.ruleName
DeductionMaster.ruleName
       ↓
PayrollItemValueService
       ↓ AUTO
RuleExecutionService
```

まで接続済み。

つまりV1は「Rule UIだけ存在する未使用機能」ではなく、給与項目AUTO計算から呼べる実行基盤まで実装されている。

一方、Rule結果から最終給与確定/Payslip永続化までの接続は現mainでは確認できない。

---

# 2. V2で目指す姿

V2ではRule Engineを特定給与ドメイン専用にせず、次の責務に分離する。

```text
Rule Definition
      ↓
Rule Runtime
      ↓
Execution Context
      ↓
Fact Resolution
      ↓
Calculation Engine
      ↓
Typed Result
      ↓
Business Caller
```

Rule基盤自身は:

- 手当
- 控除
- 給与
- 日報
- 請求
- 原価
- 契約

等を知らない。

業務側が「どのRuleをどこで適用するか」を決める。

これは現V1の `AllowanceMaster.ruleName` / `DeductionMaster.ruleName` の責務分離を維持・強化する方向。

---

# 3. 修正優先度サマリ

## P0 — セキュリティ / 正確性 / 再現性

1. Tenant source of truth統一
2. Catalogなしraw table DataSourceの制限/廃止
3. Rule ParameterからtenantId完全排除
4. JPA/JDBC共通 `TenantExecutionScope`
5. Money calculation contract導入
6. Rule Revision / Version導入
7. Execution Snapshot導入
8. Fact/SELECT *情報露出改善
9. MVEL sandbox方針確定
10. Rule result型契約

## P1 — 拡張性 / 保守性

11. `RuleCalculator` interface導入
12. `@RuleComponent` / Registry導入
13. `RuleFunctions`導入
14. RuleReferenceCheckerのdomain依存排除
15. Catalog Version導入
16. Function Library Version導入
17. Error taxonomy細分化
18. Preview/Test API
19. params/context/facts/functions namespace分離
20. Tax master effective/version管理

## P2 — 運用性 / 高度化

21. Rule Definition Audit
22. Rule Test Execution History
23. performance metric
24. Calculator catalog UI
25. typed test form
26. simulation/version compare

---

# 4. 変更案1 — RuleCalculator契約

## 現状

JAVA_BEAN方式は `org.jeasy.rules.api.Rule` BeanをSpringから取得しEasy Rulesでfireする。

長所:

- Javaで厳密な計算が書ける
- BigDecimal/RoundingModeを明示できる
- Unit Testしやすい

短所:

- Easy Rules `Rule` は汎用計算契約としては粒度が曖昧
- Factsへ副作用でresultをputする必要がある
- Repository/ServiceをDIでき、副作用を持てる
- result typeがObject

## V2候補

新しい中心契約:

```java
public interface RuleCalculator<R> {
    R calculate(RuleCalculationContext context);
}
```

候補context:

```java
public record RuleCalculationContext(
    RuleParameters parameters,
    RuleFacts facts,
    RuleFunctionRegistry functions,
    Clock clock
) {}
```

### 原則

Calculatorは原則pure calculation。

禁止候補:

```text
Repository write
external HTTP
filesystem write
email
queue publish
別Transaction開始
```

Data取得はCalculator前のFact Resolutionで完了させる。

## 対象変更箇所

現行:

```text
features/system/rule/service/executor/JavaBeanDslExecutor.java
features/system/rule/service/RuleBeanCatalogService.java
features/system/rule/config/RuleEngineConfig.java
```

新規候補:

```text
features/system/rule/calculator/RuleCalculator.java
features/system/rule/calculator/AbstractRuleCalculator.java
features/system/rule/calculator/RuleCalculationContext.java
features/system/rule/calculator/RuleCalculatorRegistry.java
```

## 互換性

V1 `JAVA_BEAN` は即削除しない。

移行期間:

```text
JAVA_BEAN_LEGACY
CALCULATOR
```

の2方式をDispatcherで共存させる。

---

# 5. 変更案2 — AbstractRuleCalculator

Base classは必須にしない。

推奨構造:

```text
RuleCalculator interface
        ↑
AbstractRuleCalculator（必要なCalculatorだけ利用）
        ↑
IncomeTaxCalculator 等
```

Base class候補責務:

- typed Fact取得
- required Fact validation
- BigDecimal conversion
- MoneyMath access
- safe null handling
- result validation

避ける責務:

- DB access
- Domain routing
- Tenant resolution
- HTTP
- Audit persistence

### 理由

継承を強制するとCalculatorがBase classへ過剰依存する。

中心契約はinterfaceに置き、共通便利機能だけoptional abstract classとする。

---

# 6. 変更案3 — Annotation + Registry

ユーザー提案の「Annotationを付ければEnum項目も動的にできないか」に対する推奨形。

Java Enum自体をruntime追加するのではなく、AnnotationをscanしてRegistry/Catalog APIを動的生成する。

候補:

```java
@RuleComponent(
    code = "INCOME_TAX",
    category = "DEDUCTION",
    displayName = "所得税計算",
    version = "2026"
)
@Component
public class IncomeTaxCalculator
        implements RuleCalculator<BigDecimal> {
}
```

Registry:

```text
Spring startup
 ↓
@RuleComponent scan
 ↓
RuleCalculatorRegistry
 ↓
Catalog API
 ↓
Vue Select
```

## 新規候補

```text
RuleComponent.java
RuleComponentDescriptor.java
RuleCalculatorRegistry.java
RuleCalculatorCatalogController.java
```

## Frontend

現在Enumに固定されているCalculator/RuleType選択肢をCatalog APIから取得可能にする。

## メリット

新Calculator追加時:

```text
system/rule Enum修正
frontend Enum修正
switch追加
```

を不要にできる。

---

# 7. 変更案4 — RuleFunctions / RuleUtils

## 方針

ユーザー案は採用候補。

ただし単一巨大`RuleUtils`は避け、責務別Facadeへ分割する。

DSLに公開するnamespace例:

```text
money
math
dates
checks
collections
```

例:

```text
money.roundYen(amount * rate)
math.between(value, min, max)
dates.isBetween(targetDate, from, to)
checks.coalesce(value, 0)
```

## Java候補

```text
RuleMoneyFunctions
RuleMathFunctions
RuleDateFunctions
RuleCheckFunctions
RuleCollectionFunctions
RuleFunctionRegistry
```

## 設計原則

Functionはpure / deterministic。

禁止:

- Repository
- DB
- HTTP
- file
- current system time直接取得
- Spring bean lookup

時刻が必要なら`Clock`または`targetDate`を明示入力する。

## Domain依存関数

完全汎用Functionsへ次のようなものは入れない。

```text
isHousingAllowanceEligible
calculateResidentTaxFiscalYear
incomeTax2026
```

これらはPayroll/Tax Calculatorへ置く。

## Security

JEXL/MVELへJava objectを公開するため、method allowlist test必須。

特にMVELはJEXLのRESTRICTED permissions相当の追加安全設計が必要。

---

# 8. 変更案5 — Money計算基盤

この項目は機能拡張候補として独立実装する。

## 現状

Rule engine共通層:

```text
BigDecimal inputは可能
共通scaleなし
共通MathContextなし
共通RoundingModeなし
```

PayrollItemCalculationServiceでは最終的に:

```java
setScale(0, RoundingMode.HALF_UP)
```

がハードコードされている。

つまり現在:

```text
Rule内部計算
→ min/max
→ 最終円単位HALF_UP
```

## 問題

法定計算では:

- 中間計算で丸める
- 最終だけ丸める
- 切捨て
- 切上げ
- 四捨五入
- 率のscale

が項目ごとに異なる可能性がある。

## 新規候補

```text
MoneyMath
MoneyRoundingPolicy
CalculationPrecisionPolicy
MoneyScalePolicy
```

### MoneyMath候補API

```java
BigDecimal add(...)
BigDecimal subtract(...)
BigDecimal multiply(...)
BigDecimal divide(...)
BigDecimal percentage(...)
BigDecimal round(...)
BigDecimal roundYen(...)
BigDecimal floorYen(...)
BigDecimal ceilYen(...)
```

## Policy候補

```text
scale
roundingMode
mathContext
intermediateRounding
finalRounding
zeroDivisionPolicy
```

### RoundingMode

最低限test対象:

```text
HALF_UP
DOWN
UP
FLOOR
CEILING
HALF_EVEN
```

## 重要

Globalに「全金額はHALF_UP」と固定しない。

Rule/Calculator metadataまたはCalculator自身がPolicyを宣言する。

## 対象変更候補

```text
PayrollItemCalculationService#calculateOne
PayrollItemValueService#toBigDecimal
```

現状の`Number -> double -> BigDecimal.valueOf`も見直し候補。

Money RuleはBigDecimal resultを型契約として要求する方が安全。

---

# 9. 変更案6 — Typed Result

## 現状

`RuleExecutionResult.result` はObject。

## 問題

Callerが期待する型をRule定義だけでは保証できない。

## V2候補

Rule metadata:

```text
resultType = DECIMAL
```

またはCalculator generic typeからCatalogへ型情報を公開する。

DSL Ruleでも:

```text
STRING
INTEGER
LONG
DECIMAL
BOOLEAN
DATE
DATETIME
OBJECT（制限的）
```

を宣言。

実行後:

```text
raw result
→ RuleResultConverter
→ declared result type validation
→ typed result
```

金額RuleはDECIMAL必須。

---

# 10. 変更案7 — Rule Identity / Revision

## 現状

RuleNameをstable identityとして:

- rename禁止
- 参照中delete禁止

が実装されている。

ただし編集は同一Rule recordを更新するため、過去ロジック再現が弱い。

## V2候補

```text
RuleDefinition
- id
- tenant
- ruleName
- currentRevision

RuleRevision
- id
- ruleDefinitionId
- revisionNo
- dslType
- dslText
- calculatorCode
- calculatorVersion
- resultType
- resultFactKey
- createdAt
- createdBy
- contentHash
- status
```

Revisionはimmutable。

編集:

```text
v1をUPDATE
```

ではなく:

```text
v1 immutable
→ v2 INSERT
```

## 実行

通常:

```text
RuleDefinition.currentRevision
```

過去再現:

```text
explicit revisionId
```

## DB Migration

V1 RuleMasterを最初のRevisionとして移行可能。

---

# 11. 変更案8 — Catalog Version

Ruleが同じでもCatalogが変わればFactが変わる。

例:

```text
whereClause変更
column mapping変更
physicalName変更
maxRows変更
```

したがってRule Versionだけでは再現できない。

## V2候補

```text
RuleDataSourceCatalog
→ identity

RuleDataSourceCatalogRevision
→ immutable version
```

Execution Snapshotには利用Catalog Revisionを保存する。

---

# 12. 変更案9 — Function Library Version

`money.roundYen()`等のJava実装変更でも同じDSL結果が変化する。

したがって:

```text
functionLibraryVersion
```

をExecution Snapshotへ記録する候補。

Library全体versionでもよいが、将来細粒度が必要ならfunction set hashを保存する。

---

# 13. 変更案10 — Calculator Version

Java Calculatorはソースコード変更で結果が変わる。

最低限metadata:

```text
calculatorCode
calculatorVersion
```

例:

```text
INCOME_TAX / 2026
INCOME_TAX / 2027
```

version文字列は単純連番ではなく業務effective versionを許可する。

コードcommit SHAを補助snapshotとして記録する案もある。

---

# 14. 変更案11 — Execution Snapshot

給与・請求等の確定処理では必須候補。

## 保存候補

```text
ruleName
ruleRevisionId
ruleHash
dslType
calculatorCode
calculatorVersion
catalogRevisionIds
functionLibraryVersion
taxDataYear
taxDataVersion
roundingPolicy
calculatedAmount
appliedAmount
manualOverride
calculatedAt
```

## Facts

Facts全文保存は推奨しない。

理由:

- 個人情報
- 容量
- secret
- 不要DB column

代わりに:

```text
execution input snapshot allowlist
```

を定義する。

例:

```text
employeeId
targetDate
hours
baseAmount
rate source id/version
```

## 保存場所

候補A:

```text
PayslipItemへ直接追加
```

候補B（推奨）:

```text
RuleExecutionSnapshot別Table
```

Rule以外の計算にも流用可能な`CalculationSnapshot`にする案もある。

---

# 15. 変更案12 — Rule Definition Audit

Execution SnapshotとDefinition Auditは分ける。

Definition Audit:

```text
誰が
いつ
どのRuleを
どのRevisionからどのRevisionへ
何を変更したか
```

Execution Snapshot:

```text
どの計算で
どのRevisionを使い
何を算出したか
```

同一Tableに混ぜない。

---

# 16. 変更案13 — Tenant完全自動化

ユーザー要望:

```text
tenantId = :tenantId
```

をRule/Catalog作者に持たせない。

V2では完全排除を推奨。

## 現状

Catalog tenantScoped時:

```text
whereClauseに :tenantId 必須
GeneralDataFetcherがTenantContext値で上書き
```

良い点:

caller spoofingは防止。

不足:

Catalog作者のpredicate書き忘れ余地。

## V2

```text
Catalog
tenantScope = TENANT

filter:
employee_id = :employeeId
```

Query Builderが自動で:

```text
tenant_id = resolvedTenant
```

を付ける。

Rule ParameterにtenantIdは存在しない。

---

# 17. 変更案14 — TenantExecutionScope

現状TenantContextはThreadLocal、JPA filterはHTTP TenantFilter、JDBCはGeneralDataFetcher独自処理。

これを統合する。

## 新規候補

```text
TrustedTenantResolver
TenantExecutionScope
TenantScopeType
TenantScopedQueryBuilder
```

## TenantExecutionScope責務

```text
resolvedTenant検証
TenantContext set
Hibernate tenant filter enable
JDBC tenant provider
finally clear
nested scope制御
```

## HTTP

```text
JWT/SecurityUser
→ TrustedTenantResolver
→ TenantExecutionScope
```

## Batch/Scheduler

```text
trusted job tenant metadata
→ TenantExecutionScope
```

## Fail Closed

Tenant requiredなのにresolved tenantなしなら処理しない。

---

# 18. 変更案15 — X-Tenant-IDの扱い

現状はJWT tenantとHeader tenantの2系統がある。

P0で整理する。

候補1（単一Tenant Userなら推奨）:

```text
Login後はJWT tenantのみ
X-Tenant-ID廃止
```

候補2（Multi-tenant membership対応）:

```text
X-Tenant-ID = requestedTenant
 ↓
Membership check
 ↓
resolvedTenant
```

Headerをそのままtrusted tenantにはしない。

---

# 19. 変更案16 — raw table DataSource廃止

## 現状

Catalogなしで:

```text
tableName
whereClause
```

をRule DataSourceへ直接設定できる。

この経路ではCatalog tenantScopedFlagによるtenant強制がない。

## P0候補

V2では:

```text
DB DataSourceはCatalog必須
```

とする。

Legacy移行期間だけraw tableを許可する場合:

```text
SYSTEM_INTERNAL
feature flag
SYS_ADMIN only
migration-only
```

等へ強く限定。

最終的には削除。

---

# 20. 変更案17 — Structured Filter Builder

現在whereClauseTemplateはSQL文字列。

Validatorは危険patternを防ぐが、文字列DSLの安全性には限界がある。

V2候補:

```json
{
  "filters": [
    {"column":"employee_id","op":"EQ","parameter":"employeeId"},
    {"column":"target_date","op":"LTE","parameter":"targetDate"}
  ]
}
```

BackendがSQLを生成する。

## 利点

- SQL injection surface縮小
- tenant predicate自動化
- column allowlist
- operator allowlist
- type validation
- frontend builder可能

## 段階移行

V2初期は既存whereClauseTemplateを残し、structured filterを新Ruleから推奨。

---

# 21. 変更案18 — Fact Namespace

現在factsはflat Map中心。

V2候補:

```text
params.*
facts.*
context.*
functions.*
```

例:

```text
params.employeeId
facts.employee.hourlyWage
facts.attendance.hours
context.targetDate
functions.money.roundYen(...)
```

### tenant

tenantはDSL namespaceへ公開しない。

### メリット

- key collision防止
- trusted/untrusted区分
- Documentation生成
- DSL autocomplete

---

# 22. 変更案19 — Fact Allowlist / SELECT *禁止

## 現状問題

Column Mapping 0件時にSELECT *へ広がる経路があり、Rule Test Responseはfacts全体を返す。

## V2

DB Catalogは必ずColumn allowlistを持つ。

```text
active catalog column >= 1
```

を登録条件候補とする。

SELECT *は原則禁止。

System internal sourceだけ例外を許可するなら明示flagが必要。

## Test Response

`RuleExecutionResult.facts`を本番APIから削除する案も検討。

管理Previewだけ:

```text
debugFacts
```

としてsafe allowlistを返す。

---

# 23. 変更案20 — Preview API

## 現状

TestタブはruleName + parametersのみ送信。

DB保存済みActive Ruleを再読込するため、未保存DSLを試せない。

## V2候補

```http
POST /api/system/rules/execution/preview
```

Request:

```text
Draft Rule Definition
Parameters
```

処理:

```text
validate
→ resolve facts
→ execute
→ return safe debug result
```

DBへ保存しない。

## Security

SYS_ADMIN限定。

本番execution APIとはError detailレベルを分ける。

---

# 24. 変更案21 — Error Taxonomy

現状:

```text
RULE_INVALID_REQUEST
RULE_CONFLICT
COMMON_RESOURCE_NOT_FOUND_ERROR
COMMON_INTERNAL_ERROR
```

V2候補:

```text
RULE_NOT_FOUND
RULE_INACTIVE
RULE_PARAMETER_MISSING
RULE_PARAMETER_INVALID
RULE_RESULT_TYPE_INVALID
RULE_DATASOURCE_INVALID
RULE_DATASOURCE_FETCH_FAILED
RULE_DSL_SYNTAX_ERROR
RULE_DSL_EXECUTION_FAILED
RULE_CALCULATION_FAILED
RULE_REVISION_NOT_FOUND
RULE_TENANT_SCOPE_MISSING
```

### API message

安全なmessageのみ。

Parameterの生valueは返さない。

### Log

structured redactionを導入。

---

# 25. 変更案22 — MVELの扱い

現状JEXL:

```text
strict
safe
silent=false
RESTRICTED permissions
RuleDslSafety
```

MVEL:

```text
RuleDslSafety
MVEL.eval
```

Security modelが同等ではない。

## 選択肢

A. MVELを廃止しJEXL + Calculatorへ集約

B. MVEL sandbox/allowlistを実装して維持

### 推奨

利用実績が少なければAを優先。

Rule Engineの選択肢が多いこと自体は価値ではなく、意味論・security・test matrixを増やす。

未決事項として利用Rule数をDBで確認して判断する。

---

# 26. 変更案23 — DSLの役割を限定

推奨:

### DSL向き

```text
条件
単純四則演算
閾値
軽量集計
設定変更頻度が高いロジック
```

### Calculator向き

```text
法令計算
税
社会保険
複雑な端数
年度Version
多段計算
高い再現性要求
```

すべてをDSLへ押し込まない。

---

# 27. 変更案24 — RuleReferenceProvider

## 現状

RuleReferenceCheckerが直接:

```text
AllowanceMasterRepository
DeductionMasterRepository
```

を知る。

これはsystem/ruleからdomainへの依存。

## V2候補

```java
public interface RuleReferenceProvider {
    List<RuleReference> findReferences(String ruleName);
}
```

各Domain:

```text
AllowanceRuleReferenceProvider
DeductionRuleReferenceProvider
PayrollRuleReferenceProvider
...
```

SpringがListで収集。

Rule側は具体Domainを知らない。

### 効果

新Domain追加時にsystem/rule変更不要。

---

# 28. 変更案25 — RuleUsageProvider

Reference（削除可否）とUsage（どこで使うか）を分ける。

```text
ReferenceProvider
  削除/無効化可否

UsageProvider
  UIで「このRuleはどこで使用中か」を表示
```

将来Rule影響分析にも利用できる。

---

# 29. 変更案26 — Tax Master Version

確認済み:

```text
InsuranceRate       yearあり
IncomeTaxBracket    yearあり
ResidentTaxMonthly  fiscalYearあり
StandardSalary      yearなし
```

StandardSalaryは過去再現性上の弱点。

## 修正候補

```text
StandardSalary
+ effectiveFrom/effectiveTo
```

または:

```text
masterVersion
```

税/保険マスタをExecution Snapshotで識別可能にする。

---

# 30. 変更案27 — Clock / targetDate

Detail Providerの一部は `Clock` の現在年を使う。

過去給与再計算ではtargetDate基準の方が適切。

## 方針候補

```text
実行基準日 = context.targetDate
現在時刻   = Clock
```

を明確に分ける。

税/保険年度選択に現在Clockを使うのは避ける。

Clockは「現在日時が本当に必要な処理」だけに使用。

---

# 31. 変更案28 — Payroll最終丸めの外出し

現状:

```text
PayrollItemCalculationService
setScale(0, HALF_UP)
```

V2:

```text
CalculationResult
- rawAmount
- roundedAmount
- roundingPolicy
```

またはMoneyCalculator内で最終化。

PayrollItemCalculationServiceが全計算に同じ丸めを強制しない。

ただしV1互換期間はHALF_UPをdefault policyにする。

---

# 32. 変更案29 — min/maxと丸め順序

現状:

```text
raw result
→ BigDecimal
→ min/max clamp
→ HALF_UP
```

この順序は仕様として固定するか判断が必要。

V2ではPolicyに:

```text
LIMIT_BEFORE_ROUND
LIMIT_AFTER_ROUND
```

まで持たせる必要は通常ない。

業務仕様が明確ならCalculator側で明示し、共通層の過度な可変化を避ける。

現V1互換では`clamp -> round`を維持。

---

# 33. 変更案30 — manual override監査

現状:

```text
calculatedAmount
amount
manualOverride
```

まではruntime resultで区別できる。

永続化Modelに根拠が残らない可能性がある。

V2 Snapshotでは:

```text
calculatedAmount
appliedAmount
manualOverride
manualInputBy
manualInputAt
manualReason(optional)
```

まで検討。

給与監査では重要。

---

# 34. 変更案31 — Rule Test UI

V2 UI候補:

```text
Parameter definition
 ↓
型付きForm自動生成

LONG     number
DECIMAL  decimal
DATE     date picker
BOOLEAN  checkbox
```

Advanced modeだけRaw JSONを残す。

さらにPreview時に:

```text
resolved params
safe facts
result
result type
rule revision
catalog revisions
rounding
```

を表示。

---

# 35. 変更案32 — Calculator Catalog UI

Annotation Registryから:

```json
{
  "code":"INCOME_TAX",
  "displayName":"所得税計算",
  "category":"DEDUCTION",
  "version":"2026",
  "resultType":"DECIMAL"
}
```

を返す。

FrontendはEnumハードコードを減らす。

---

# 36. Package構成候補

```text
features/system/rule/
├─ api/
├─ definition/
│  ├─ entity/
│  ├─ repository/
│  └─ service/
├─ revision/
├─ execution/
│  ├─ RuleExecutionService
│  ├─ RuleExecutionContext
│  └─ RuleExecutionResult
├─ parameter/
├─ fact/
│  ├─ resolver/
│  └─ datasource/
├─ catalog/
├─ calculator/
│  ├─ RuleCalculator
│  ├─ AbstractRuleCalculator
│  ├─ RuleComponent
│  └─ RuleCalculatorRegistry
├─ functions/
│  ├─ RuleMoneyFunctions
│  ├─ RuleMathFunctions
│  ├─ RuleDateFunctions
│  └─ RuleFunctionRegistry
├─ dsl/
│  ├─ JexlDslExecutor
│  └─ LegacyMvelDslExecutor
├─ reference/
├─ audit/
└─ snapshot/
```

Tenantはこのfeature配下へ置かず、`app/tenant`等の共通Infrastructureへ置く。

---

# 37. 実行シーケンス V2

```text
Business Caller
 ↓
RuleExecutionFacade.execute(ruleName, context)
 ↓
Trusted Tenant already resolved
 ↓
RuleDefinition + Revision load
 ↓
Parameter validation
 ↓
Catalog Revision load
 ↓
Fact resolve
 ↓
Execution engine select
 ├─ JEXL
 └─ Calculator Registry
 ↓
Typed result validation
 ↓
Money/Result policy
 ↓
Execution metadata生成
 ↓
Business Caller
 ↓
確定処理ならExecution Snapshot保存
```

---

# 38. RuleExecutionServiceの責務整理

現V1のServiceは比較的薄く良い。

V2でもorchestratorに留める。

入れないもの:

- tax domain logic
- allowance logic
- tenant selection
- rounding details
- DB raw SQL

それぞれ専用componentへ分離。

---

# 39. Transaction設計

Rule Preview/Test:

```text
readOnly
snapshot保存なし
```

給与確定:

```text
business transaction
 ↓
Rule calculation read
 ↓
Payroll persistence
 ↓
Execution Snapshot
```

Rule Calculator自身はTransactionを開始しない。

外部API依存Ruleを将来許可する場合は別カテゴリとして扱い、通常Calculatorと混在させない。

---

# 40. Migration Phase 0 — テスト追加

コード構造を変える前にV1 behaviorを固定する。

最低限:

```text
Tenant isolation
JEXL BigDecimal
min/max
HALF_UP
manual override
AUTO Rule execution
Rule failure
Catalog fetch
```

をIntegration Test化。

---

# 41. Migration Phase 1 — Tenant P0

最優先。

1. JWT/header tenant信頼境界整理
2. TrustedTenantResolver
3. TenantExecutionScope
4. headerなしJPA filter保証
5. GeneralDataFetcherをresolved tenantへ接続
6. raw DataSource制限
7. Integration Test

この段階ではRule DSL仕様は変えない。

---

# 42. Migration Phase 2 — Money Contract

1. MoneyMath
2. RoundingPolicy
3. BigDecimal result contract
4. PayrollItemCalculationServiceのHALF_UP互換Policy化
5. division/rounding test

V1結果が変わらないdefault設定から始める。

---

# 43. Migration Phase 3 — Revision / Snapshot

1. RuleDefinition / RuleRevision
2. V1 RuleMaster migration
3. Revision-aware Loader
4. Execution metadata
5. Snapshot Entity
6. Audit

既存RuleName APIはcompatibility facadeで維持。

---

# 44. Migration Phase 4 — Calculator Registry

1. `RuleCalculator`
2. `@RuleComponent`
3. Registry
4. Calculator Catalog API
5. 既存JAVA_BEAN adapter
6. 新規Calculatorから新方式利用

既存Easy Rules Beanを一括書換しない。

---

# 45. Migration Phase 5 — RuleFunctions

1. pure function contract
2. money/math/date/check functions
3. JEXL exposure security test
4. MVEL方針決定
5. function library version
6. frontend documentation/autocomplete

---

# 46. Migration Phase 6 — Structured Catalog

1. raw table path deprecated
2. Catalog required
3. structured filter
4. tenant auto predicate
5. SELECT *禁止
6. catalog revision

---

# 47. Migration Phase 7 — Preview/UI

1. Draft preview endpoint
2. typed parameter form
3. safe facts
4. revision compare
5. calculator catalog

---

# 48. 互換性方針

V2移行で「既存Ruleを全部一度に書き直す」は避ける。

Compatibility Adapter:

```text
V1 JEXL             → existing executor
V1 MVEL             → legacy executor
V1 JAVA_BEAN        → legacy Easy Rules adapter
V2 CALCULATOR       → RuleCalculatorRegistry
```

新規RuleからV2方式を推奨。

既存Ruleはテストを作った上で段階移行。

---

# 49. テスト戦略

## Unit

```text
MoneyMath
RuleFunctions
Parameter converter
Result converter
Registry
Revision hash
Tenant resolver
```

## Integration

```text
Tenant A/B isolation
Catalog/JDBC
Rule loader
JEXL
Calculator
Snapshot
```

## Contract

```text
同Revision + 同Facts + 同Function Version
→ 同result
```

## Golden Test

税・保険等:

```text
年度別既知入力
→ 法定期待金額
```

を固定。

---

# 50. Money Test詳細

必須例:

```text
0
0.49
0.50
0.51
1.49
1.50
1.51
-0.49
-0.50
-0.51
```

各RoundingMode。

BigDecimal:

```text
1 / 3
100 / 6
999999999999
0.00001
```

MathContext/scale違い。

min/max:

```text
min-0.01
min
min+0.01
max-0.01
max
max+0.01
```

---

# 51. Version Test詳細

```text
Rule v1 → 1000
Rule v2 → 1200
```

Execution Snapshotがv1なら、currentがv2でもv1再現可能。

Catalog v1/v2、Calculator v1/v2、Function v1/v2も同様。

---

# 52. Tenant Test詳細

```text
JWT A + header B → reject
JWT A + no header → A
JWT A + header A → A
```

さらにJPA/JDBC双方が同tenantを見ることを1つのIntegration Testで保証する。

Rule作者のparameter mapにtenantIdを入れてもSecurity scopeへ影響しないことも残す。

最終的にはtenantIdをreserved/ignored parameterにし、DSLへ公開しない。

---

# 53. 修正対象ファイル一覧 — P0

## Tenant

現行主要対象:

```text
backend/src/main/java/com/project/backend/app/tenant/context/TenantContext.java
backend/src/main/java/com/project/backend/app/tenant/filter/TenantFilter.java
backend/src/main/java/com/project/backend/app/config/SecurityConfig.java
backend/src/main/java/com/project/backend/app/security/jwt/filter/JwtAuthenticationFilter.java
backend/src/main/java/com/project/backend/features/system/rule/service/fetcher/GeneralDataFetcher.java
backend/src/main/java/com/project/backend/features/system/rule/service/loader/RuleLoader.java
backend/src/main/java/com/project/backend/features/system/rule/repository/RuleMasterRepository.java
backend/src/main/java/com/project/backend/features/system/rule/repository/RuleDataSourceCatalogRepository.java
```

新規候補:

```text
TrustedTenantResolver.java
TenantExecutionScope.java
TenantScopeType.java
```

## Money

```text
PayrollItemValueService
PayrollItemCalculationService
RuleExecutionResult / result conversion
```

新規候補:

```text
MoneyMath.java
MoneyRoundingPolicy.java
CalculationPrecisionPolicy.java
```

## Version

```text
RuleMaster
RuleLoader
RuleMasterCommandService
RuleMasterQueryService
RuleExecutionService
```

新規:

```text
RuleDefinition
RuleRevision
RuleExecutionSnapshot
```

---

# 54. 修正対象ファイル一覧 — P1

## Calculator

```text
JavaBeanDslExecutor
RuleBeanCatalogService
DslExecutorDispatcher
```

新規:

```text
RuleCalculator
AbstractRuleCalculator
RuleComponent
RuleCalculatorRegistry
RuleCalculatorCatalogController
```

## Functions

新規:

```text
RuleFunctionRegistry
RuleMoneyFunctions
RuleMathFunctions
RuleDateFunctions
RuleCheckFunctions
```

## Reference

現行:

```text
RuleReferenceChecker
```

新規Domain側:

```text
AllowanceRuleReferenceProvider
DeductionRuleReferenceProvider
```

---

# 55. 修正対象ファイル一覧 — UI

現行Rule Page / Tabs / Composable / APIに対して将来:

```text
Calculator Catalog取得
Rule Revision表示
Preview API
Typed Parameter Form
safe Fact viewer
resultType
roundingPolicy
```

を追加。

ただしV2 backend contract確定前にUI先行実装しない。

---

# 56. 削除候補

最終段階で削除候補:

```text
raw tableName DataSource path
legacy MVEL（利用実績次第）
Easy Rules Rule direct execution（adapter移行完了後）
RuleBeanCatalogService（Registryへ統合後）
flat tenantId parameter convention
SELECT * fallback
```

削除はMigrationとデータ移行完了後。

---

# 57. 現V1で残すべき良い設計

V2で全部作り直さない。

残す/活かすもの:

```text
DslExecutor interface / DispatcherのStrategy構造
RuleParameterResolverの型変換思想
Catalog allowlist
JEXL strict mode
JEXL RESTRICTED permissions
GeneralDataFetcherのtenant spoofing上書き思想
RuleName stable identity思想
参照中削除禁止
SYS_ADMINによる管理API保護
TraceId
Clock DI
```

---

# 58. 過度に汎用化しない事項

V2でも以下は避ける。

```text
全業務をRule Engineに移す
全計算をDSL化
丸めを100種類metadata化
任意Spring BeanをDSLから呼ぶ
任意SQLをRuleから書く
任意HTTPをRuleから呼ぶ
```

Rule基盤の役割は「安全に設定可能な計算・判定を実行する」ことであり、汎用プログラミング環境を提供することではない。

---

# 59. 推奨V2アーキテクチャ最終形

```text
Business Domain
  Allowance / Deduction / Payroll / Other
             ↓
      RuleExecutionFacade
             ↓
      Rule Definition / Revision
             ↓
   Parameter Validation
             ↓
       Fact Resolver
             ↓
     Catalog Revision
             ↓
   Tenant-safe Query Layer
             ↓
       Execution Engine
       ├─ JEXL
       └─ RuleCalculator Registry
             ↓
         RuleFunctions
             ↓
       Typed Result
             ↓
       Money Policy
             ↓
      Execution Metadata
             ↓
        Business Domain
             ↓
   Calculation/Execution Snapshot
```

横断:

```text
TenantExecutionScope
Clock
Audit
TraceId
Security
Versioning
```

---

# 60. 実装順序の推奨

実際に修正を開始する場合、次の順番を推奨する。

```text
1. 現V1挙動のIntegration Test追加
2. Tenant trust boundary修正
3. raw DataSource制限
4. Money / BigDecimal contract
5. Rule Revision
6. Execution Snapshot
7. Calculator interface + Registry
8. RuleFunctions
9. ReferenceProvider
10. Catalog Revision / structured filter
11. Preview API
12. Frontend動的Catalog/UI
13. Legacy MVEL/Easy Rules整理
```

理由:

安全性・計算正確性・再現性を先に固定し、その後拡張性を上げる方が移行リスクが低い。

---

# 61. 実装開始前の確認質問

1. Tenantは現在「1ユーザー=1Tenant」で固定か。将来1ユーザーが複数Tenantを切り替える予定があるか。
2. MVELで既に本番Rule定義を作っているか。なければ廃止候補にできるか。
3. JAVA_BEAN/Easy Rulesの既存Rule Beanは何個あるか。
4. Rule変更後も過去給与を完全再計算可能にする必要があるか。それとも確定結果Snapshot保存だけでよいか。
5. 給与項目の最終丸め `HALF_UP` は現在の確定業務仕様か、仮実装か。
6. 手当/控除ごとに丸めPolicyを選択可能にしたいか、それとも複雑なものはCalculator側だけで管理するか。
7. StandardSalary等の法定マスタは年度履歴を全て保持する方針でよいか。
8. Rule TestではFactのどこまで管理者に見せる必要があるか。
9. Rule Definition変更に承認フロー（draft/approved/published）が必要か。
10. Rule Calculatorを今後、給与以外の請求・原価・契約にも利用する予定か。

---

# 62. 読む順番

`system/rule`を理解するには次の順を推奨する。

```text
01〜05  基本データ構造・実行フロー
06      Runtime利用・拡張性
07      DSL計算意味論
08      Test/Result/Error
09      手当連携
10      控除連携
11      税/保険Data Model
12〜13  給与永続化gap
14〜15  Test/Validation
16      Exception/Log
17      Tenant境界
18      Security/Tenant信頼境界
19      本書 V2総まとめ
```

---

# 63. 最終評価

V1は「作り直し必須の設計」ではない。

既に:

- Strategy型Executor
- Catalog
- Parameter typing
- Tenant spoofing防止の一部
- JEXL sandbox
- Domain側ruleName適用

など、V2へ発展させやすい土台がある。

最大の課題は構造そのものより、次の横断契約がまだ弱いこと。

```text
Tenantの唯一のsource of truth
Moneyの精度・丸め
Version/Revision
Execution再現性
Factの公開範囲
Calculatorの副作用制限
```

この6点を先に固定すれば、その上にAnnotation Registry / RuleFunctions / 動的Catalog等を安全に追加できる。

したがってV2は全面Rewriteではなく、**V1の良い構造を残した段階的強化**を推奨する。
