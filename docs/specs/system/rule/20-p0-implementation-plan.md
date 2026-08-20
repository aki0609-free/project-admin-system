# system/rule 詳細設計 20 — P0修正 実装計画書

## 0. 目的

`19-v2-generic-rule-architecture.md` で整理したV2候補のうち、P0だけを実装可能な粒度へ分解する。

この文書ではコード修正は行わない。

基準コード:

```text
main
12c91a72b409df16b9d4be0b416247a07a8f170a
```

P0の目的は新機能追加ではなく、次の基盤契約を先に固定すること。

```text
Tenant isolation
DB DataSource safety
Money precision
Rule result contract
Version / reproducibility
Fact exposure
DSL security
```

---

# 1. P0-01 現V1挙動をIntegration Testで固定

## 理由

構造変更前に現在の正常系をGolden Masterとして固定しないと、V2化による意図しない計算差分を検出できない。

## 追加テスト候補

```text
RuleExecutionServiceIntegrationTest
RuleTenantIsolationIntegrationTest
RuleMoneyCompatibilityTest
RuleCatalogDataFetcherIntegrationTest
PayrollRuleIntegrationTest
```

## 固定する挙動

```text
JEXL execution
Parameter conversion
DataSource fact merge
resultFactKey
AUTO allowance/deduction rule execution
manual override
min/max clamp
最終HALF_UP
inactive rule failure
missing parameter failure
```

## 完了条件

V2構造変更前後で、仕様変更対象以外の既存結果が同一。

---

# 2. P0-02 Tenant source of truth統一

## 現状対象

```text
app/tenant/context/TenantContext.java
app/tenant/filter/TenantFilter.java
app/config/SecurityConfig.java
app/security/jwt/filter/JwtAuthenticationFilter.java
```

## 問題

JWT tenantIdと`X-Tenant-ID`の2系統がTenantContextを書き換え得る。

TenantFilterはHeader tenantをPrincipal tenantと照合しない。

Headerなしauthenticated requestではHibernate tenantFilterが有効になる保証が弱い。

## 新規候補

```text
app/tenant/TrustedTenantResolver.java
app/tenant/TenantExecutionScope.java
app/tenant/TenantResolution.java
```

## TrustedTenantResolver

責務:

```text
Authentication/SecurityUser取得
principalTenant取得
requestedTenant取得（必要な場合のみ）
一致/membership確認
resolvedTenant返却
```

### 単一Tenant Userの場合

```text
resolvedTenant = SecurityUser.tenantId
```

`X-Tenant-ID`はlogin以外では不要にする。

### Multi Tenantの場合

```text
requestedTenant
+ membership
→ resolvedTenant
```

## TenantFilter変更候補

現行:

```text
X-Tenant-ID
→ TenantContext
→ Hibernate Filter
```

変更後:

```text
TrustedTenantResolver
→ TenantExecutionScope
→ TenantContext + Hibernate Filter
```

## JwtAuthenticationFilter変更候補

JWT filter自身が最終Tenant scopeを所有しない。

JWT filterはPrincipal確立までを責務とし、resolved tenantのscope開始は共通Infrastructureへ寄せる。

## SecurityConfig

Filter順序を明示的に設計する。

```text
JWT authentication
→ tenant resolution/scope
→ controller
```

Filter annotation/orderの偶然に依存しない。

## 必須テスト

```text
JWT A + header B → reject
JWT A + no header → A
JWT A + header A → A
invalid JWT → 401
unknown JWT tenant/user → authentication不可
request終了後TenantContext clear
```

---

# 3. P0-03 TenantExecutionScope

## 目的

HTTPだけでなくService/Batch/SchedulerからRuleを実行しても同じTenant isolationを保証する。

## API候補

```java
public interface TenantExecutionScope {
    <T> T execute(String resolvedTenantId, Supplier<T> action);
}
```

またはcurrent authenticated scopeを使うAPIを別に持つ。

## 内部責務

```text
previous TenantContext保存
TenantContext set
Hibernate tenant filter enable
execute
finally disable
previous context restore/clear
```

## nested execution

同tenantのnested scope:

```text
許可
```

異tenant nested scope:

```text
原則reject
```

system-level明示scopeだけ例外候補。

## Transaction注意

Hibernate Session取得タイミングとの整合が必要。

Tenant scopeをTransaction開始後にenableするか、Transaction interceptorとの順序をIntegration Testで固定する。

---

# 4. P0-04 Rule Repository tenant依存整理

## 現状

RuleMasterRepositoryには:

```text
findByTenantIdAnd...
findByRuleName...
findById...
findAll...
```

が混在し、一部はHibernate Filter依存。

## 方針

Repository methodにtenantIdを毎回渡す設計へ戻すのではなく、TenantExecutionScopeでRepository scopeを保証する。

ただしSecurity-sensitive lookupは明示tenant methodを残して二重防御してよい。

## 変更対象

```text
RuleMasterRepository
RuleDataSourceCatalogRepository
RuleLoader
RuleMasterQueryService
RuleMasterCommandService
```

## 完了条件

Rule ServiceをHTTP外から呼んでもTenant scopeなしではfail closed。

---

# 5. P0-05 Rule ParameterからtenantId排除

## 現状

Catalog tenantScoped時、whereClauseに`:tenantId`を書く必要がある。

GeneralDataFetcherが値をTenantContextで上書きするためspoofing自体は防いでいる。

## 変更

Rule Parameter definitionとしてtenantIdを使わせない。

reserved name候補:

```text
tenantId
tenant_id
currentTenant
```

## Validator

Rule Parameter登録時にreserved security parameterをreject。

## DSL

tenantIdをfactsへ公開しない。

## Catalog

tenant predicateはQuery Builderが自動生成。

---

# 6. P0-06 raw table DataSource制限

## 対象

```text
GeneralDataFetcher
RuleDefinitionValidator
RuleDataSource関連DTO/Entity
```

## 現状問題

Catalogなし:

```text
tableName + whereClause
```

を登録可能で、Catalog tenantScopedFlagによる強制がない。

## 移行

Phase A:

```text
既存raw source検出
警告
新規登録禁止
```

Phase B:

```text
既存raw sourceをCatalogへmigration
```

Phase C:

```text
runtime raw path削除
```

## 互換性

DB内に既存raw Ruleがあるか事前SQL調査が必要。

0件なら即禁止可能。

---

# 7. P0-07 TenantScopedQueryBuilder

## 目的

Rule作者からtenant SQLを消す。

## 新規候補

```text
TenantScopedQueryBuilder.java
RuleQueryPlan.java
```

入力:

```text
catalog physical table
allowed columns
where template/structured filters
resolved tenant
parameters
```

出力:

```text
SQL
Named parameters
```

## tenant predicate

Catalog metadata:

```text
tenantScope = TENANT
 tenantColumn = tenant_id
```

ならBackendが自動追加。

## 注意

`tenantColumn`を自由文字列にする場合もidentifier validation必須。

---

# 8. P0-08 SELECT * fallback廃止

## 現状問題

Column mappingなしで全columnがFactへ流入する可能性がある。

## 修正

TENANT/business Catalogでは:

```text
active column mapping >= 1
```

必須。

SQL生成:

```text
SELECT allowlisted_column...
```

のみ。

## 既存データ

Column mapping 0件のCatalogをmigration前に一覧化する。

---

# 9. P0-09 Fact exposure制限

## 現状

Rule test resultでfactsを返す。

## 修正候補

Production execution result:

```text
result
metadata
```

のみ。

Preview/Admin test:

```text
safeFacts
```

を返す。

## safeFacts

Catalog column側へ:

```text
debugVisibleFlag
```

を持たせる案、またはdefault非表示 + explicit allowlist。

## Log

Parameter map/Facts全文をERROR logへ出さない。

---

# 10. P0-10 Money Result Contract

## 対象

```text
RuleExecutionResult
PayrollItemValueService
PayrollItemCalculationService
```

## 現状

Rule result Objectを`toBigDecimal()`で変換。

Numberはdouble経由になり得る。

## 修正

Money用途ではRule metadataのresultTypeを`DECIMAL`にする。

```text
Object
→ declared result converter
→ BigDecimal
```

Money callerはBigDecimal以外を受けない。

## 禁止候補

```text
Doubleをmoney resultとして許容
NaN
Infinity
```

## Compatibility

既存Number resultは移行期間だけconverterで受け、warningを出す。

---

# 11. P0-11 MoneyMath / RoundingPolicy

## 新規候補

```text
common/calculation/MoneyMath.java
common/calculation/RoundingPolicy.java
common/calculation/PrecisionPolicy.java
```

Rule専用にせず、給与/請求/原価でも利用できる共通層候補。

## RoundingPolicy

最低限:

```text
scale
RoundingMode
```

MathContextは必要なCalculatorだけ指定できる構造。

## 代表policy

```text
YEN_HALF_UP = scale 0 / HALF_UP
YEN_DOWN    = scale 0 / DOWN
YEN_UP      = scale 0 / UP
```

ただしEnumだけで全法定計算を表現しようとしない。

---

# 12. P0-12 PayrollItemCalculationServiceの丸め

## 現状

最終:

```text
setScale(0, HALF_UP)
```

## 移行

最初は結果を変えない。

```text
hardcoded HALF_UP
↓
default RoundingPolicy.YEN_HALF_UP
```

へ置換する。

次の段階で項目/Calculatorがpolicyを宣言可能にする。

## Test

移行前後で既存給与テスト結果完全一致。

---

# 13. P0-13 BigDecimal変換

## 問題候補

```text
Number.doubleValue()
→ BigDecimal.valueOf(...)
```

はmoney contractとして曖昧。

## 修正

優先順位:

```text
BigDecimal →そのまま
BigInteger → new BigDecimal
Integer/Long → BigDecimal.valueOf(long)
String → new BigDecimal(validated)
Float/Double → legacy only / reject候補
```

## DSL

JEXL numeric literalの実型をtestして、Money expressionでBigDecimalを維持できる書き方をDocument化する。

---

# 14. P0-14 Rule Revision

## DB候補

```text
rule_definition
rule_revision
```

## RuleDefinition

```text
id
tenant_id
rule_name
current_revision_id
active_flag
created_at
updated_at
```

## RuleRevision

```text
id
rule_definition_id
revision_no
dsl_type
dsl_text
rule_bean_name/calculator_code
result_fact_key
result_type
content_hash
created_at
created_by
```

## 更新

Rule編集時:

```text
existing revision UPDATE禁止
new revision INSERT
current_revision_id切替
```

## rename

ruleName stable identity維持。

---

# 15. P0-15 Revision Migration

## Step 1

既存RuleMaster各行からRuleDefinition生成。

## Step 2

現内容をrevisionNo=1としてRuleRevisionへcopy。

## Step 3

currentRevisionId設定。

## Step 4

Read pathをRevision対応へ切替。

## Step 5

Write pathをimmutable revision方式へ切替。

## Rollback

Migration期間は旧RuleMaster columnを即削除しない。

Dual writeは複雑なので避け、read switchのfeature flag/段階migrationを検討。

---

# 16. P0-16 Execution Snapshot

## 新規候補

```text
RuleExecutionSnapshot.java
RuleExecutionSnapshotRepository.java
RuleExecutionMetadata.java
```

## 保存タイミング

Test/Previewでは保存不要。

Business確定処理で保存。

## Transaction

給与確定と同一Transactionが推奨。

給与保存成功・snapshot失敗でcommitしてしまわない。

## 最低保存

```text
ruleDefinitionId
ruleRevisionId
ruleName
ruleHash
resultType
calculatedResult
calculatedAt
```

後続でCatalog/Function/Calculator version追加。

---

# 17. P0-17 Result Type

## Rule metadata追加

```text
resultType
```

候補:

```text
DECIMAL
INTEGER
LONG
BOOLEAN
STRING
DATE
DATETIME
```

OBJECTは原則避ける。

## Validator

給与AUTO Ruleへ割り当てる場合:

```text
resultType == DECIMAL
```

をDomain側で検証。

Rule Engine自体は給与を知らない。

---

# 18. P0-18 MVEL security判断

## 実装前調査

DBで:

```text
dsl_type = MVEL
```

の件数・active件数を確認。

## 0件

新規MVEL作成を停止し、deprecated化を推奨。

## 利用あり

既存Rule migration計画を作る。

Sandboxを新規構築するよりJEXL/Calculatorへ移す方が総保守コストが低い可能性が高い。

---

# 19. P0-19 DSL result / side effect制限

## JEXL

Ruleは最終resultを返す、またはresultFactKeyへ値を置く現互換を維持。

V2では可能ならreturn-styleへ寄せる。

## Calculator

副作用禁止。

Rule execution中のDB writeをArchitecture Test/Code Review ruleで禁止候補。

---

# 20. P0-20 Transaction Boundary

## Rule Definition CRUD

Revision insert + current revision switchを1Transaction。

## Rule execution

Fact readはcaller transactionへ参加。

## Business確定

```text
calculate
→ business entity save
→ execution snapshot save
```

同一Transaction。

## Preview

readOnly。

---

# 21. P0変更の依存関係

```text
V1 tests
  ↓
Tenant source
  ↓
TenantExecutionScope
  ↓
Catalog-only DB source
  ↓
Tenant query builder / SELECT allowlist

V1 money tests
  ↓
Typed result
  ↓
MoneyMath / RoundingPolicy
  ↓
Payroll compatibility

V1 Rule CRUD tests
  ↓
Rule Revision
  ↓
Execution Metadata
  ↓
Snapshot
```

Tenant・Money・Revisionの3系列はある程度並行可能だが、各系列内の順序は守る。

---

# 22. P0で変更しないもの

この段階では以下はまだ触らない。

```text
Vue大幅改修
Annotation Calculator Registry
RuleFunctions
ReferenceProvider
Structured Filter UI
Approval workflow
MVEL全面削除
Easy Rules全面削除
```

P0を安定させてからP1へ進む。

---

# 23. P0 Definition of Done

P0完了条件:

```text
1. authenticated requestのTenantが1つに確定
2. JPA/JDBCが同一Tenant scope
3. Rule作者がtenantIdを書かない
4. CatalogなしDB access不可
5. SELECT allowlist必須
6. Money resultはBigDecimal contract
7. 丸めpolicyが明示化
8. V1既存計算結果が互換testで一致
9. Rule編集がimmutable revision
10. 確定計算にrevision根拠を保存可能
11. Test APIが不要Factを露出しない
12. MVEL security方針が確定
```

---

# 24. 実装PRの分割推奨

1つの巨大PRにしない。

```text
PR-1  V1 behavior tests
PR-2  Tenant resolver/scope
PR-3  Rule DB DataSource hardening
PR-4  Typed money result + rounding policy
PR-5  Rule revision schema/model
PR-6  Revision-aware execution
PR-7  Execution snapshot
PR-8  Fact exposure / security cleanup
```

各PRで既存CIをgreenにしてから次へ進む。

---

# 25. リスク

最大リスクは「設計改善によって現在動いている給与計算値を変えてしまうこと」。

したがって:

```text
安全性を上げる変更
構造を変える変更
計算仕様を変える変更
```

を同一PRに混ぜない。

特にMoney周りでは、`HALF_UP`をPolicy化するPRで丸め方式そのものを変更しない。

---

# 26. 実装開始時に最初に確認するデータ

コード修正前に本番/検証DBで件数確認が必要。

```text
MVEL Rule数
JAVA_BEAN Rule数
CatalogなしDataSource数
Column mapping 0件Catalog数
whereClauseに:tenantIdを含むCatalog数
RuleName参照中Allowance数
RuleName参照中Deduction数
```

これでmigration riskを判断する。

---

# 27. 総合判断

P0はRule Engineの「機能追加」ではない。

現在の良い設計を残しながら:

```text
安全性
正確性
再現性
```

をArchitecture Contractとして固定する段階。

このP0が完了してからCalculator Registry、Annotation、RuleFunctions等の汎用化を進めれば、V2機能追加によって基盤が不安定になるリスクを大幅に下げられる。
