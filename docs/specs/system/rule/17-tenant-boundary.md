# system/rule 詳細設計 17 — Tenant境界・自動スコープ

## 1. 調査方針

今回は実装コードを変更しない。Rule管理CRUDとRule実行時DB参照について、Tenant分離を横断確認する。

基準コード:

- main branch: `12c91a72b409df16b9d4be0b416247a07a8f170a`
- 設計書branchはmainから32 commit ahead / 0 behindで、差分はdocsのみ
- GitHub APIではローカルworktreeの未コミット変更は確認できないため、今回の「現在コード」はmain commitを基準とする

---

## 2. 結論

現状のTenant機構は一枚岩ではなく、次の3方式が混在する。

```text
A. HTTP X-Tenant-ID
   → TenantContext(ThreadLocal)

B. Hibernate tenantFilter
   → BaseEntity tenant_id = :tenantId

C. Repository methodへtenantIdを明示引数
   → findByTenantIdAnd...

D. RuleのNamedParameterJdbcTemplate
   → GeneralDataFetcherがTenantContextからtenantIdを強制上書き
   → Catalog whereClauseTemplate内の :tenantId を要求
```

Rule実行DataFetcherはユーザー入力のtenantIdを信用せずTenantContext値で上書きするため、この一点は良い。

しかし「tenantId = :tenantId のようなパラメータをRule定義に持ちたくない。自動判定したい」という目標に対しては、現実装は未達。CatalogのwhereClauseTemplateへtenant条件を明示記述する必要がある。

さらにHTTP TenantFilterは `X-Tenant-ID` headerをそのままTenantContextへ入れており、このクラス単体では認証Principalとのtenant所属照合を行わない。Tenant IDの信頼境界はSecurity層と合わせて別途保証が必要。

---

## 3. TenantContext

主要ファイル:

`backend/src/main/java/com/project/backend/app/tenant/context/TenantContext.java`

class:

`TenantContext`

functions:

- `setTenantId(String tenantId)`
- `getTenantId()`
- `clear()`

実装は `ThreadLocal<String>`。

### 実装事実

Tenant IDはrequest thread単位のThreadLocalとして保持される。

### 注意

ThreadLocalなのでasync / scheduler / new threadへ自動伝播しない。

HTTP request以外からRuleを呼ぶ場合、TenantContext設定責務を別途持つ必要がある。

---

## 4. HTTP TenantFilter

主要ファイル:

`backend/src/main/java/com/project/backend/app/tenant/filter/TenantFilter.java`

class:

`TenantFilter extends OncePerRequestFilter`

処理:

```text
request
 ↓
X-Tenant-ID header取得
 ↓
headerあり?
 ├─ yes → TenantContext.setTenantId(header)
 │         Hibernate tenantFilter enable
 │         Hibernate softDeleteFilter enable
 └─ no  → どちらもenableしない
 ↓
Controller/Service
 ↓
finally
   TenantContext.clear()
   tenantFilter disable
   softDeleteFilter disable
```

### 重要な実装事実

`X-Tenant-ID`がnullの場合、tenantFilterだけでなくsoftDeleteFilterもenableされない。

したがってBaseEntityの自動filterに依存するQueryでは、tenant headerなしrequestの挙動を明示的に考える必要がある。

### 未決事項

このFilter内には:

```text
X-Tenant-IDがログインUserに許可されたtenantか
```

の照合がない。

認証Filter/Security側で完全保証されているかを共通Security調査と突合する必要がある。

---

## 5. BaseEntityのTenant Filter

主要ファイル:

`backend/src/main/java/com/project/backend/app/base/entity/BaseEntity.java`

Hibernate:

```text
@FilterDef(name="tenantFilter", tenantId:String)
@Filter(condition="tenant_id = :tenantId")

@FilterDef(name="softDeleteFilter")
@Filter(condition="deleted_at IS NULL")
```

Entity作成/更新:

```text
@PrePersist
 tenantId == null
   → TenantContext.getTenantId()

@PreUpdate
 tenantId == null
   → TenantContext.getTenantId()
```

### 確定仕様

BaseEntityを継承するJPA Entityは、Hibernate Sessionでfilterが有効ならtenant条件を受ける。

### 注意

BaseEntity自身が常時filterをenableするわけではない。enable責務はTenantFilter。

また `tenantId == null` の時だけContextから補完するため、EntityへtenantIdを明示setした場合はその値が優先される。

---

## 6. RuleMaster Entity

主要ファイル:

`features/system/rule/entity/RuleMaster.java`

`RuleMaster extends BaseEntity`。

DB unique:

```text
(tenant_id, rule_name)
```

したがってDB schema思想としてRuleNameはtenant内unique。

これはRuleをtenant単位の定義として扱う設計。

---

## 7. RuleMasterRepository — 混在

主要ファイル:

`features/system/rule/repository/RuleMasterRepository.java`

Tenant明示あり:

```text
findByTenantIdAndActiveFlagTrue...
findByTenantIdAndRuleTypeInAndActiveFlagTrue...
existsByTenantIdAndRuleNameAndActiveFlagTrue...
findByTenantIdAndRuleNameAndActiveFlagTrue...
```

Tenant明示なし:

```text
findAllByDeletedAtIsNullOrderByIdAsc()
findByActiveFlagTrueAndDeletedAtIsNullOrderByIdAsc()
findByIdAndDeletedAtIsNull(id)
findByRuleNameAndDeletedAtIsNull(ruleName)
findByRuleNameAndActiveFlagTrueAndDeletedAtIsNull(ruleName)
existsByRuleNameAndDeletedAtIsNull(ruleName)
existsByRuleNameAndIdNotAndDeletedAtIsNull(ruleName,id)
```

### 実装事実

Tenant明示なしmethodはHTTP request中でHibernate tenantFilterが有効ならfilterに依存する。

つまりRepository APIだけを見てもtenant-safeか判断できず、呼出Contextに依存する。

### 修正候補

Tenant境界を一方式へ統一する。

V2候補としては:

```text
業務Service/Repository
  tenantIdを引数として受け取らない

TenantScope infrastructure
  Security principalからtenantを確定
  JPA/JDBC双方へ自動適用
```

が希望仕様に合う。

---

## 8. RuleMasterQueryService

主要ファイル:

`features/system/rule/service/RuleMasterQueryService.java`

functions:

- `findAll()`
- `findActiveRules()`
- `findDetail(Long id)`
- `findByRuleName(String ruleName)`

全てtenantId引数なしRepository methodを使用する。

### 実装事実

HTTP TenantFilterが有効な通常requestではHibernate tenantFilterに依存してtenant分離する。

### リスク

同ServiceをHTTP外で呼び、Hibernate filterをenableしていない場合はRepository method自体にtenant条件がない。

**修正候補: 高**

Tenant safetyをHTTP Filterの副作用だけに依存させない。

---

## 9. RuleLoader — Runtime Rule選択

主要ファイル:

`features/system/rule/service/loader/RuleLoader.java`

`loadActive(String ruleName)`:

```text
repository.findByRuleNameAndActiveFlagTrueAndDeletedAtIsNull(ruleName)
```

Tenant IDを明示しない。

### 通常HTTP Rule Test

TenantFilter有効
→ Hibernate filterでtenant限定
→ tenant内active Ruleを取得

### 業務Service / batch / scheduler

呼出経路でHibernate filterが有効である保証が必要。

これは給与等からRuleExecutionServiceを直接呼ぶ将来構成では特に重要。

---

## 10. RuleDataSourceCatalog

Entity:

`features/system/rule/entity/RuleDataSourceCatalog.java`

`BaseEntity`継承。

unique:

```text
(tenant_id, source_code)
```

Catalog自体もtenant単位。

また:

```text
tenantScopedFlag = true default
```

を持つ。

---

## 11. Catalog Repository / Service

Repository:

`RuleDataSourceCatalogRepository`

```text
findByActiveFlagTrueAndDeletedAtIsNullOrderBySourceCodeAsc()
findBySourceCodeAndActiveFlagTrueAndDeletedAtIsNull(sourceCode)
```

tenantId明示なし。

Service:

`RuleDataSourceCatalogService#findActive()`
`RuleDataSourceCatalogService#findRequired(sourceCode)`

もTenantContextを直接見ない。

### 実装事実

Catalog tenant分離もHibernate tenantFilterへ依存する。

### テスト不足

現在の`RuleDataSourceCatalogServiceTest`はMockito Repository testであり、Hibernate tenantFilterによるtenant isolationを保証しない。

---

## 12. GeneralDataFetcher — JPAとは別世界

主要ファイル:

`features/system/rule/service/fetcher/GeneralDataFetcher.java`

ここは `NamedParameterJdbcTemplate` を使用する。

したがってHibernate `tenantFilter` は一切効かない。

このため独自Tenant処理が入っている。

---

## 13. GeneralDataFetcherのTenant処理

Catalog利用時:

```text
catalog.tenantScopedFlag == true
 ↓
TenantContext.getTenantId()
 ↓
空なら IllegalStateException
 ↓
whereClauseTemplateに :tenantId があるか確認
 ↓
なければ IllegalStateException
 ↓
queryParameters.put("tenantId", TenantContext値)
```

### 良い点

caller paramsに:

```text
tenantId = "spoofed"
```

が入っていても最後にTenantContext値で上書きする。

`GeneralDataFetcherTest#fetch_shouldUseCatalogAndForceTenantParameter()` がこれを保証している。

Testでは:

```text
params tenantId = spoofed
TenantContext = tenant-a
```

でも最終JDBC parameterは:

```text
tenantId = tenant-a
```

になる。

これは重要な安全仕様。

---

## 14. ただしtenantIdがRule DSLから完全に消えてはいない

現在のCatalog例:

```sql
tenant_id = :tenantId AND employee_id = :employeeId
```

GeneralDataFetcherは`:tenantId`を自動注入するが、whereClauseTemplate側にはtenant条件を明示記述する必要がある。

したがって現在は:

```text
Rule作者がtenantId値を渡す必要はない
```

までは実現しているが、

```text
Catalog作者もtenant条件を意識しなくてよい
```

にはなっていない。

### 希望仕様との比較

希望:

```text
employee_id = :employeeId
```

だけ定義すれば、Infrastructureが自動で:

```text
tenant_id = currentTenant
```

を付ける。

現状:

```text
tenant_id = :tenantId AND employee_id = :employeeId
```

をCatalogに書く必要がある。

**未達として記録する。**

---

## 15. Catalogを使わないLegacy DataSource

`GeneralDataFetcher#loadCatalog()`はcatalogCodeが空ならnull。

その場合:

```text
tableName = source.tableName
whereClause = source.whereClause
maxRows = 1000
```

となる。

そしてtenantScopedFlag判定自体がCatalog依存なので、CatalogなしDataSourceではGeneralDataFetcherがtenant条件を強制しない。

### 重要

これは汎用化時の最大のTenant gapの一つ。

RuleMasterValidatorもCatalogなしDataSourceを許可し、tableName/whereClauseを検証して登録できる。

つまり現状は:

```text
Catalog DataSource
  → tenantScopedFlag=trueならTenant強制

Legacy raw table DataSource
  → Tenant強制なし
```

となる。

**修正候補: P0**

V2ではtenant-scoped DB accessはCatalog経由必須にし、raw table accessを廃止またはsystem-onlyへ限定する。

---

## 16. tenantScopedFlag=false

Catalogには `tenantScopedFlag` があるため、falseならGeneralDataFetcherはtenant条件を要求しない。

これは:

- 全国共通税率
- system global master
- tenant非依存lookup

等を想定できる。

ただしCatalog Entity自身はBaseEntity継承でtenant単位保存。

つまり:

```text
Catalog定義はtenant別
参照先データはglobal
```

という組み合わせが可能。

これを確定仕様とするかは未決事項。

---

## 17. RuleReferenceCheckerのTenant gap

主要ファイル:

`features/system/rule/service/validation/RuleReferenceChecker.java`

参照判定:

```text
AllowanceMasterRepository.existsByRuleNameAndDeletedAtIsNull(ruleName)
DeductionMasterRepository.existsByRuleNameAndDeletedAtIsNull(ruleName)
```

この2 methodはtenantId明示なし。

Allowance/Deduction Repositoryの通常CRUD methodはtenantId明示が多い一方、Rule参照確認だけはtenantIdなし。

Hibernate filterが有効ならtenant限定されるが、Repository method単体の契約としてはtenant非明示。

### 影響

HTTP Rule CRUDではfilter依存で通常tenant内判定になる。

HTTP外呼出やfilter未設定では他tenant参照を拾う可能性がある。

**修正候補: 高**

ReferenceCheckerもTenantScope infrastructureに統一する。

---

## 18. Duplicate Validationの意味

`RuleMasterValidator#validateDuplicate()`:

```text
existsByRuleNameAndDeletedAtIsNull(ruleName)
existsByRuleNameAndIdNotAndDeletedAtIsNull(ruleName,id)
```

tenantId明示なし。

DB unique constraintは:

```text
(tenant_id, rule_name)
```

なので、設計上はtenant内duplicateのみ禁止したい。

HTTP filterが正しく有効なら整合するが、filter未設定ではapplication validationとDB constraintの意味がずれる。

---

## 19. X-Tenant-ID信頼境界

TenantFilterはrequest header値を直接:

```text
TenantContext
Hibernate filter parameter
```

へ設定する。

このクラスには:

- tenant existence確認
- authenticated userとのmembership確認
- role別tenant access確認

はない。

### 未決事項

Security層で別途保証されている可能性があるため、この時点で「任意tenantへアクセス可能」とは断定しない。

ただしRule Tenant安全性の最終保証にはSecurity側との突合が必須。

---

## 20. Scheduler / Batch / Async

TenantContextはThreadLocal、Hibernate filterはHTTP `TenantFilter`でenableされる。

そのため:

```text
scheduler
batch
async
message consumer
background job
```

からRuleExecutionServiceを呼ぶ場合、HTTP TenantFilterは通らない。

### 必要になる設計

```text
TenantExecutionScope.run(tenantId, () -> ...)
```

のような共通境界で:

- TenantContext set/clear
- JPA filter enable/disable
- JDBC tenant binding

をまとめる方が安全。

---

## 21. 希望するV2 Tenantモデル

Rule DSL / Rule Parameter / Catalog whereClauseからtenantIdを消す。

```text
Authenticated Principal / Job Tenant
          ↓
    TenantExecutionScope
          ↓
 ┌────────┴────────┐
 JPA              JDBC Rule Query
 ↓                 ↓
Hibernate          Query Builder
Filter             auto tenant predicate
 ↓                 ↓
tenant_id           tenant_id
=currentTenant      =currentTenant
```

Rule作者が見るもの:

```text
employeeId
targetDate
amount
hours
```

Rule作者が見ないもの:

```text
tenantId
security principal
raw tenant predicate
```

---

## 22. Catalog V2案

現在:

```text
physicalName
whereClauseTemplate
 tenant_id = :tenantId AND employee_id = :employeeId
```

候補:

```text
physicalName = vw_rule_employee_basic
tenantScope = TENANT
key predicates:
  employee_id = :employeeId
```

Infrastructure SQL Builder:

```text
SELECT <allowlisted columns>
FROM <allowlisted physical source>
WHERE tenant_id = <trusted current tenant>
  AND employee_id = :employeeId
LIMIT ...
```

これならtenant条件の書き忘れを構造的に防げる。

---

## 23. テストで保証されていること

現在確認できたRule固有test:

`GeneralDataFetcherTest#fetch_shouldUseCatalogAndForceTenantParameter`

保証:

```text
caller supplied tenantIdを信用しない
TenantContext値で上書きする
```

これは残すべき重要な回帰test。

---

## 24. 不足しているTenant Test

最低限追加候補:

```text
1. tenant Aでtenant BのRuleMasterを取得できない
2. tenant Aでtenant BのRule ID detailを取得できない
3. tenant Aで同名Ruleを作成可能
4. 同tenantで同名Ruleは作成不可
5. Catalogもtenant A/Bで分離
6. TenantContextなしtenantScoped Catalog実行はfail closed
7. spoofed tenantId parameterは常に無視
8. Catalog whereClauseからtenant条件が欠ければfail closed（現V1）
9. raw table DataSourceのtenant漏洩検証
10. RuleReferenceCheckerが他tenant参照を拾わない
11. headerなしrequestでtenant-scoped endpointが拒否される
12. authenticated userが未所属tenant headerを指定した場合拒否
13. scheduler/batch TenantExecutionScope
14. asyncでThreadLocal漏洩しない
```

---

## 25. 優先度

### P0

- raw table DataSourceでtenant強制がない
- TenantをRule/Catalog記述から完全自動化する設計
- authenticated principal と X-Tenant-ID の信頼境界確認/保証
- HTTP外Rule実行時のTenantScope

### P1

- RuleMaster Repositoryのtenant明示/Filter依存混在を統一
- Catalog Repositoryの同様の混在解消
- RuleReferenceChecker tenant scope統一
- duplicate validationとDB unique意味の一致を構造的に保証

### P2

- tenantScope enum化（TENANT/GLOBAL等）
- TenantExecutionScope共通API
- tenant isolation integration tests拡充

---

## 26. 現時点の判定

### 確定仕様 / 実装事実

- RuleMaster/CatalogはBaseEntityを継承しtenant_idを持つ
- DB uniqueはtenant単位
- HTTPではX-Tenant-IDからTenantContextとHibernate filterを設定
- JDBC Rule DataFetcherではTenantContextからtenantIdを自動注入
- callerがtenantIdをspoofしてもCatalog tenantScoped実行ではContext値に上書き

### 推測ではなく未決事項

- X-Tenant-IDと認証Userの所属照合が別Security層でどこまで保証されるか
- tenantScopedFlag=false Catalogの正式用途
- HTTP外Rule実行の標準Tenant初期化方法

### V2候補

- tenantIdをRule Parameter/DSL/Catalog whereClauseから完全排除
- TenantExecutionScopeをJPA/JDBC共通Infrastructureとして導入
- Catalog経由SQL Builderがtenant predicateを自動付与
- raw table accessを廃止または強く制限

---

## 27. 次の調査

Tenant境界だけを見るとSecurityとの接続確認が残る。

次はRule Controllerへの認証・権限とTenant headerの信頼境界を小さく追加確認し、その結果をこの章へ追記する。その後、system/rule全体の総まとめとして:

- 未決事項
- 修正候補
- V2汎用化設計
- Base Calculator / Annotation catalog
- RuleFunctions / RuleUtils
- BigDecimal / scale / RoundingMode / MathContext
- Version / Revision
- Tenant完全自動化
- Migration順序

をまとめる。
