# system/rule 詳細設計 21 — P0テスト方針

## 0. 位置づけ

本書はP0実装前に追加すべきテストの「方針」を示す。

ProjectAdminSystemには今後も多数の機能・業務ドメインが追加/調査されるため、system/ruleだけで過剰に詳細なテスト仕様を固定しない。

目的は、V2への構造変更で現在の正常動作を壊さないための最低限のSafety Netを決めること。

コード修正は行わない。

---

# 1. 基本方針

Rule Engineのテストは次の4層に分ける。

```text
1. Unit
   小さい変換・判定・計算

2. Component
   Rule Engine内部component間

3. Integration
   DB/Tenant/Transactionを含む

4. Business Contract
   給与等の呼出側から見た結果
```

E2Eで全ケースを保証しない。

重要な計算仕様はUnit/Integrationへ寄せ、E2Eは主要経路だけにする。

---

# 2. P0で絶対に固定する領域

```text
Tenant isolation
Rule load
Parameter conversion
Fact fetch
JEXL execution
Money result conversion
min/max
rounding
manual override
AUTO item integration
error/fail closed
```

これらは今後内部実装を変えても外部挙動を維持するためのContractとする。

---

# 3. Tenant Test方針

最優先。

最低限:

```text
Tenant AのRuleをTenant Aが取得可能
Tenant AからTenant BのRuleを取得不可
Tenant AのJDBC FactからTenant B rowを取得不可
JWT A + requested Bはreject
JWT A + headerなしでもAとしてscope
request終了後Contextが残らない
```

特に重要なのは、JPAとRule JDBCを別々のUnit Testだけで終わらせず、同一Integration Testで両者が同じTenantを参照することを保証すること。

---

# 4. Rule Execution Test方針

代表的なRuleを少数用意する。

```text
固定値
Parameter加算
Fact参照
条件分岐
BigDecimal計算
```

DSLの全構文を自前テストしない。

JEXLライブラリ自体のテストではなく、ProjectAdminSystemがJEXLへ渡すContext・Security・Result契約をテストする。

---

# 5. Parameter Test方針

代表型:

```text
STRING
INTEGER/LONG
DECIMAL
BOOLEAN
DATE
```

確認:

```text
required
missing
invalid format
default
unknown parameter
reserved security parameter
```

V2でtenantIdをreserved化したら、その登録/実行拒否を追加する。

---

# 6. Fact / Catalog Test方針

最低限:

```text
Catalog allowlist columnだけ取得
parameter bind
0件
1件
複数件
maxRows
Tenant auto scope
```

P0変更後:

```text
CatalogなしDataSource reject
Column mappingなしreject
SELECT *を生成しない
```

をContract化する。

---

# 7. Money Test方針

P0では「すべての給与計算」をRule Engineのテストに入れない。

Rule共通層では以下を保証する。

```text
BigDecimal維持
DECIMAL result contract
scale適用
HALF_UP
DOWN
UP
負数
境界値
```

法定税額等の具体的な業務計算は各Calculator/Domain側のGolden Testへ置く。

---

# 8. V1互換テスト

現在の給与連携で重要な順序を固定する。

```text
Rule result
→ BigDecimal conversion
→ min/max
→ final rounding
→ manual override / applied amount
```

P0でRoundingPolicyを導入しても、default policyでは既存期待値が変わらないことを確認する。

---

# 9. Manual Override

最低限:

```text
AUTO calculated amount
manual amountなし → calculated amount採用
manual amountあり → manual amount採用
calculatedAmountは保持
manualOverride=true
```

将来Snapshotを導入したら:

```text
calculatedAmount
appliedAmount
manualOverride
```

が監査情報へ残ることを追加する。

---

# 10. Rule Revision Test方針

Revision導入時の中心Contract:

```text
Revisionはimmutable
編集で新Revision生成
current revision切替
過去Revision指定実行可能
```

代表例:

```text
v1 => 1000
v2 => 1200
current => 1200
explicit v1 => 1000
```

これだけでVersion設計の根幹をかなり保証できる。

---

# 11. Execution Snapshot Test方針

確定処理で最低限:

```text
ruleName
revisionId
result
calculatedAt
```

が残る。

給与等のBusiness transactionがrollbackした場合、Snapshotだけ残らないことをIntegration Testで確認する。

---

# 12. Error Test方針

代表的なfail closedだけを固定する。

```text
Rule not found
inactive Rule
missing required parameter
invalid result type
Tenant scope missing
Catalog violation
DSL execution failure
```

内部Exception classすべてを1対1でテストする必要はない。

APIの安全なerror code/message contractを中心にする。

---

# 13. Security Test方針

JEXLではProjectAdminSystem側の制限を確認する。

```text
許可されたFact/Functionは利用可能
危険なclass/method access不可
Tenant/security object非公開
```

MVELはP0で利用状況を確認し、廃止なら新規security testを増やしすぎない。

---

# 14. Test Data方針

実在個人情報を使わない。

Tenant:

```text
tenant-a
tenant-b
```

Employee:

```text
employee-a1
employee-b1
```

Rule:

```text
TEST_FIXED_AMOUNT
TEST_PARAMETER_AMOUNT
TEST_FACT_AMOUNT
```

のようにテスト目的が分かる名称を使う。

---

# 15. Test Class構成候補

過度に細分化しない。

```text
RuleExecutionServiceTest
RuleExecutionIntegrationTest
RuleTenantIsolationIntegrationTest
RuleCatalogIntegrationTest
RuleMoneyContractTest
RuleRevisionIntegrationTest
PayrollRuleIntegrationTest
```

機能が増えたら必要に応じて分割する。

---

# 16. CI方針

P0 testは通常CIで実行する。

分類候補:

```text
fast unit/component
stable integration
```

Tenant/DB/Transaction testを「重いから通常CIから外す」ことは避ける。

Rule Engine変更PRでは少なくとも:

```text
backend package + stable tests
```

で主要Contractを検出できる状態を目標にする。

---

# 17. PRごとのテスト追加

テストを最初に全部作る必要はない。

```text
PR-1 V1 behavior
  → current contract tests

PR-2 Tenant
  → tenant integration tests

PR-3 DataSource hardening
  → catalog/query tests

PR-4 Money
  → precision/rounding tests

PR-5/6 Revision
  → revision tests

PR-7 Snapshot
  → transaction/snapshot tests
```

変更と保証を同じPRに入れる。

---

# 18. 今後の他機能への共通原則

system/rule以外を調査するときも、次の観点を再利用する。

```text
誰がscopeを決めるか
どこがsource of truthか
入力型は何か
副作用はどこか
Transactionはどこか
Version/履歴は必要か
監査は必要か
例外時にfail closedか
どのtestが仕様を保証するか
```

Ruleだけを特別扱いせず、ProjectAdminSystem全体の詳細設計方針として利用できる。

---

# 19. 完了条件

P0実装前に最低限、以下のテスト方針が合意できればよい。

```text
TenantはIntegration Testで保証
MoneyはBigDecimal/rounding contractで保証
Rule実行は代表ケースで保証
Revisionはimmutable contractで保証
Business連携は主要経路だけ保証
```

全ケースを先回りして設計しない。

機能調査を続け、各domain固有仕様が判明した段階で、そのdomainのGolden Testを追加する。

---

# 20. 推奨判断

ProjectAdminSystemはまだ調査対象機能が多いため、現時点ではsystem/ruleのテストを完成形まで固定しない。

今必要なのは、将来の修正で壊してはいけない境界を明確にすること。

その境界は現時点では:

```text
Tenant
Money
Rule Execution
Revision
Transaction
Security
```

の6つ。

この6つをSafety Netとして固めた上で、他機能の調査を進める方針を推奨する。
