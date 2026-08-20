# system/rule 詳細設計 15 — Validation / Catalog / Parameter テスト保証

## 1. 方針

今回もコード修正は行わない。

基準コード: main `12c91a72b409df16b9d4be0b416247a07a8f170a`

対象:

- `RuleValueConverterTest`
- `RuleMasterValidatorTest`
- `RuleParameterResolverTest`
- `RuleDataSourceCatalogServiceTest`
- `RuleMasterCommandServiceTest`

---

## 2. RuleValueConverterTest

Path:
`backend/src/test/java/com/project/backend/features/system/rule/service/converter/RuleValueConverterTest.java`

### テスト済み型

```text
INTEGER  "10" -> Integer 10
LONG     "20" -> Long 20
DECIMAL  "12.50" -> BigDecimal("12.50")
BOOLEAN  "true" -> true
DATE     "2026-07-25" -> LocalDate
DATETIME "2026-07-25T09:30:00" -> LocalDateTime
```

### 実装保証として重要な点

DECIMALは`double`ではなくBigDecimalへ変換される基本ケースがtestされている。

さらにBOOLEANについて:

```text
"yes"
```

を曖昧なbooleanとして拒否するtestがある。

したがってRule parameter/data source値の型変換は、少なくとも主要型について明示的にtestされている。

### 不足候補

- STRING
- null
- blank
- INTEGER/LONG overflow
- DECIMAL exponent表記
- NaN/Infinity相当入力
- DATE不正日付
- DATETIME timezone/offset
- boolean大文字小文字方針

---

## 3. RuleParameterResolverTest

Path:
`backend/src/test/java/com/project/backend/features/system/rule/service/validation/RuleParameterResolverTest.java`

### default値

Rule parameter:

```text
rate DECIMAL required default="1.25"
count INTEGER required default=null
```

caller:

```text
count="3"
systemValue="preserved"
```

結果:

```text
rate -> BigDecimal("1.25")
count -> Integer 3
systemValue -> preserved
```

### 保証

- 宣言parameterの型変換
- default value適用
- callerが渡した未宣言parameterも保持

最後の点は汎用Rule基盤として重要。

現在のresolverは「RuleParameterに宣言された値だけに絞るallowlist」ではなく、systemValue等の追加parameterを保持できる設計。

これはemployeeId/targetDate等を上位contextから透過させるには便利だが、security boundaryとしては別途管理が必要。

### required parameter

`employeeId LONG required` が欠落するとIllegalArgumentException。

つまりemployeeIdのようなRule入力をrequired parameterとして宣言でき、欠落を実行前に拒否できることがtestされている。

---

## 4. RuleParameterの設計上の注意

現行:

```text
Declared Parameters
+
Undeclared caller/system parameters
```

が最終parameter mapに共存可能。

### 利点

- 汎用contextを追加しやすい
- employeeId / targetDate / systemValue等を透過可能

### リスク

DSLへ露出するnamespaceを厳密に設計しないと、将来security/internal値までRule作者から参照可能になる可能性。

V2候補:

```text
params.*      user/rule declared parameters
context.*     trusted execution context
facts.*       fetched business facts
functions.*   allowlisted pure functions
```

のようにnamespaceを責務分離する。

Tenantは`params`にも`context`にもDSL公開せず、Infrastructure security contextとして扱う案を維持。

---

## 5. RuleMasterValidatorTest — Rule名変更禁止

Path:
`backend/src/test/java/com/project/backend/features/system/rule/service/validation/RuleMasterValidatorTest.java`

既存Rule:

```text
OVERTIME_ALLOWANCE
```

update request:

```text
RENAMED_RULE
```

を拒否するtestがある。

Exception:

```text
RuleConflictException
```

### 評価

RuleNameが他マスタから参照されるstable identifierとして扱われているため、rename禁止は参照切れ防止として合理的。

ただし将来Version/alias/migrationを実装する場合は再検討候補。

---

## 6. Unsafe SQL whereClause拒否

Validator testでは以下を登録しようとする。

```text
tenant_id = :tenantId; DROP TABLE users
```

結果:

```text
IllegalArgumentException
message contains whereClause
```

### 保証

少なくともsemicolon + destructive SQLを含むwhereClauseを登録時validationで拒否する。

これはRule作者が任意SQL全文を実行する設計ではなく、制限されたwhereClause DSLとして扱おうとしている証拠。

### ただし重要

この1testだけではSQL safety全体を保証しない。

不足候補:

```text
UNION
subquery
comment -- /* */
OR 1=1
function call
information_schema
pg_catalog
sleep系
CTE
quote escape
nested expression
```

どこまで許可/禁止するかを仕様化する必要がある。

最終的には文字列blacklistだけでなく、Catalog + structured filter builder方式が安全。

---

## 7. Unsafe DSL拒否

Validator test:

```text
Runtime.getRuntime().exec('command')
```

をRule DSLとして登録しようとするとIllegalArgumentException。

### 保証

危険なRuntimeアクセスを登録段階で拒否するvalidationが存在する。

### 不足候補

JEXL sandboxとvalidationの両層で以下を確認したい。

- Class / ClassLoader
- reflection
- System
- Runtime
- ProcessBuilder
- file/network
- Spring Bean直接参照
- arbitrary constructor
- static method

RuleFunctionsを導入する場合、任意Java呼出を広げずallowlisted functionだけを公開する方向がよい。

---

## 8. RuleDataSourceCatalogServiceTest

Path:
`backend/src/test/java/com/project/backend/features/system/rule/service/RuleDataSourceCatalogServiceTest.java`

Catalogに:

```text
employee_id active=true
secret_value active=false
```

が存在する場合、`findActive()` responseにはemployee_idだけが出ることをtest。

### 保証

inactive catalog columnをRule作成UI/API向けのactive catalog responseへ露出しない。

これは「DBに存在する全columnをRule作者へ見せる」のではなく、Catalog allowlistを介して公開する設計として良い。

### 不足候補

- inactive Catalog本体
- deletedAt
- orderNo
- duplicate sourceCode
- duplicate columnName
- physicalName validation
- type metadata consistency
- tenantScopedFlag consistency

---

## 9. RuleMasterCommandServiceTest — 参照中Rule削除禁止

Path:
`backend/src/test/java/com/project/backend/features/system/rule/service/RuleMasterCommandServiceTest.java`

Rule:

```text
OVERTIME_ALLOWANCE
```

ReferenceChecker:

```text
手当マスタ
```

を返す場合、deleteはRuleConflictException。

さらに:

```text
verify(repository, never()).delete(rule)
```

までtestしている。

### 保証

参照中Ruleは削除されず、Repository delete副作用も発生しない。

これはRule参照整合性として重要。

---

## 10. Version観点

現在のrename禁止・参照中delete禁止は「RuleNameをstable identityとして扱う」設計。

しかしVersionを本格導入する場合は:

```text
RuleIdentity
  OVERTIME_ALLOWANCE

RuleRevision
  v1
  v2
  v3
```

へ分ける方が自然。

そうすれば:

- identityはrenameしない
- revisionはimmutable
- 新編集は新revision
- 過去給与は旧revisionを参照

という設計が可能。

これは以前記録したVersion修正候補と整合する。

---

## 11. Catalog safetyの現時点評価

現行には既に複数の防御がある。

```text
Catalog allowlist
inactive column非公開
unsafe whereClause validation
TenantContext強制上書き
maxRows / internal limit
Rule DSL validation
JEXL strict mode
参照中Rule削除禁止
RuleName rename禁止
```

したがって「完全に自由なSQL/スクリプト実行基盤」ではない。

一方で汎用Rule platformとして強化するなら:

```text
structured query builder
Tenant predicate自動注入
DSL sandbox allowlist
function registry
immutable RuleRevision
catalog version
execution snapshot
```

が主要V2候補。

---

## 12. テスト不足 — 優先順位

### P0 Security

- TenantContextなしでTENANT sourceを必ず拒否
- unsafe SQL bypass pattern
- JEXL sandbox escape
- inactive/non-catalog column直接指定拒否
- physical table/view allowlist bypass

### P0 Money

- BigDecimal division
- scale
- MathContext
- HALF_UP/DOWN/UP/FLOOR/CEILING
- min/max境界
- negative

### P1 Rule lifecycle

- create success
- update success
- delete success
- duplicate RuleName
- version/revision
- referenced Rule update影響

### P1 Parameters

- optional missing
- default conversion error
- duplicate parameter name
- reserved names
- undeclared/internal parameter exposure

---

## 13. 確定仕様 / 実装事実 / 未決事項

### 実装事実 + test保証

- DECIMAL→BigDecimal変換
- required parameter欠落拒否
- default parameter適用
- 未宣言caller parameter保持
- RuleName変更拒否
- unsafe SQL clauseの一例を拒否
- Runtime.exec型unsafe DSL拒否
- inactive Catalog column非公開
- 参照中Rule削除拒否

### 推測してはいけないこと

- SQL injection全般が完全防御済み
- JEXL sandbox escapeが完全防御済み
- Catalogから任意DBアクセスが完全に不可能
- 金額精度が給与要件を満たす

### 未決事項

- undeclared parameterをどこまで許可するか
- reserved namespace
- SQL filterの最終形
- Rule revision/version model
- rounding policy

### V2候補

- params/context/facts/functions namespace分離
- Tenant predicate完全自動化
- structured filter builder
- immutable RuleRevision
- RuleFunctions registry
- MoneyPolicy/CalculationContext

---

## 14. 次の調査

次はRule実行全体について、例外の流れを追う。

```text
Controller
→ Command/Query Service
→ RuleExecutionService
→ ParameterResolver
→ DataFetcher
→ Converter
→ DSL Executor
→ result conversion
→ caller
```

各層で:

- 何のExceptionになるか
- 握りつぶしがないか
- API responseへ何が出るか
- transaction rollback
- audit/log
- secret/SQL/DSLの漏洩

を確認する。
