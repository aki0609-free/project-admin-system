# system/rule 詳細設計 01 — RuleMaster基本CRUD

## 1. 対象範囲

今回は `system/rule` のうち、**RuleMasterの基本情報に関する一覧・詳細・新規登録・更新・削除だけ**を対象とする。

Parameter / DataSource / Column Mapping / DSL実行詳細はまだ掘らない。ただしCRUD処理の都合上、子Entityが保存・削除時にどう扱われるかという事実だけは記録する。

基準コード:

- branch: `main`
- commit: `12c91a72b409df16b9d4be0b416247a07a8f170a`

**この調査ではコード修正を行わない。**

---

## 2. 処理経路

```text
RuleManagementPage.vue
→ useRulePage.ts
→ RuleTable.vue / RuleEditDialog.vue
→ ruleFormMapper.ts
→ useRulesQuery / useRuleMutations
→ RuleMasterController
→ RuleMasterQueryService / RuleMasterCommandService
→ RuleMasterValidator
→ RuleReferenceChecker
→ RuleMasterMapper
→ RuleMasterRepository
→ RuleMaster Entity
→ DB
```

---

## 3. 画面入口

`frontend/src/features/system/rule/pages/RuleManagementPage.vue`

画面:

```text
Rule管理
```

Pageは以下を組み合わせる。

```text
useRulePage()
├─ RuleTable
└─ RuleEditDialog
```

Toolbarには `新規作成` 1ボタン。

Rule一覧行クリック時は `openEdit(rule)`。

---

## 4. Frontend一覧取得

`useRulesQuery.ts`:

```http
GET /api/system/rules
```

Response:

```text
RuleMasterResponse[]
```

`RuleMasterResponse` はRule基本情報だけでなくparameters / dataSourcesも含むResponse型。

**実装事実**: 一覧APIでも `RuleMasterMapper#toResponse()` を使用するため、各RuleのParameter/DataSource/Column MappingまでResponseへ組み立てる。

### Detail APIとの関係

`useRuleDetailQuery.ts` は存在し:

```http
GET /api/system/rules/{id}
```

を呼べる。

しかし現在の `useRulePage#openEdit()` は一覧行の `RuleMasterResponse` をそのままDialogへ渡しており、詳細Queryを使っていない。

**実装事実**: 現行管理画面では一覧Response自体が編集用詳細データとして利用されている。

**修正候補（記録のみ）**: Rule数や子定義が増えた場合、一覧をsummary化し、編集時だけdetail取得する責務分離を検討できる。

---

## 5. RuleEditDialog基本情報

主要ファイル:

`frontend/src/features/system/rule/components/RuleEditDialog.vue`

Dialogのタブ:

- Basic
- DSL
- Parameter
- DataSource
- Test

今回はBasic領域のみ対象。

### 左側共通項目

- `ruleName`
- `ruleDisplayName`
- `ruleType`
- `dslType`
- `resultFactKey`
- `priority`
- `activeFlag`

Basicタブ:

- `description`
- JAVA_BEAN時のみ `ruleBeanName`

---

## 6. 新規フォーム初期値

`createEmptyRuleMasterForm()`:

| 項目 | Frontend初期値 |
|---|---|
| id | 0 |
| ruleName | `''` |
| ruleDisplayName | `''` |
| ruleType | GENERAL |
| dslType | JEXL |
| dslText | `''` |
| ruleBeanName | `''` |
| resultFactKey | result |
| description | `''` |
| priority | 100 |
| activeFlag | true |

### 注意: DSL default差異

Backend Entity `RuleMaster` のdefault `dslType` は `MVEL`。

Backend Mapperでもrequest.dslTypeがnullなら `MVEL`。

一方Frontend新規フォームは `JEXL`。

**実装事実**: 管理画面から通常新規作成する場合はJEXLが送信されるため、Frontend defaultが実質適用される。

**未決事項**: JEXLとMVELのどちらをV1標準としたいか。

今回は修正しない。

---

## 7. ruleType選択肢

Frontendで確認できる値:

- GENERAL
- ALLOWANCE
- DEDUCTION
- DAILY_REPORT
- MONTHLY_DETAIL
- PAYROLL

ruleTypeはBackend Validatorで必須。

詳細な各typeの意味・利用先は変更影響解析時に別途確定する。

---

## 8. dslType選択肢

Frontend:

- JEXL
- MVEL
- JAVA_BEAN

基本CRUD段階でのValidation差:

### JAVA_BEAN

- ruleBeanName必須
- identifier形式必須
- RuleBeanCatalogServiceで登録済みBeanであること必須

### JEXL / MVEL

- dslText必須
- `RuleDslSafety.validate()` 対象

DSL安全性の具体的内容は後続設計で掘る。

---

## 9. 新規作成Frontend

`useRulePage#openCreate()`:

```text
dialogRule = null
dialogVisible = true
```

Dialog open watchで:

```text
createEmptyRuleMasterForm()
```

を設定する。

Footer:

- キャンセル
- 作成

削除ボタンは表示されない。

---

## 10. 編集Frontend

`useRulePage#openEdit(rule)`:

```text
dialogRule = 一覧行Response
dialogVisible = true
```

Dialog側:

```text
toRuleMasterForm(rule)
```

へ変換する。

編集判定:

```text
form.id > 0
```

### ruleName

Frontendで:

```text
:disabled="isEdit"
```

となる。

Backendでも変更禁止Validationがあるため二重防御。

**確定仕様**: ruleNameは作成後変更不可。

---

## 11. 保存Frontend

`useRulePage#save(form)`:

```text
1. toRuleMasterSaveRequest(form)
2. id > 0 → PUT
   else   → POST
3. mutation完了
4. rulesQuery.refetch()
5. Dialog close
6. dialogRule=null
```

Mutation側でもQuery invalidateを行うため、さらにPage側で明示refetchしている。

**実装事実**: 保存後はinvalidate + explicit refetchの二重更新経路がある。

---

## 12. 新規API

```http
POST /api/system/rules
```

Controller:

`RuleMasterController#create()`

Service:

`RuleMasterCommandService#create()`

処理:

```text
1. validator.validateForCreate(request)
2. new RuleMaster()
3. mapper.applyRequest(entity, request)
4. repository.save(entity)
5. mapper.toResponse(saved)
```

`@Transactional`。

---

## 13. 更新API

```http
PUT /api/system/rules/{id}
```

Service:

```text
1. repository.findByIdAndDeletedAtIsNull(id)
2. validator.validateForUpdate(entity, request)
3. Active→InactiveならReferenceChecker確認
4. mapper.applyRequest()
5. repository.save()
6. mapper.toResponse()
```

`@Transactional`。

---

## 14. 基本Validation

`RuleMasterValidator` の基本情報Validation。

### ruleName

- 必須
- 最大150文字
- `^[a-zA-Z0-9_]+$`
- 重複不可
- 更新時変更不可

### ruleDisplayName

- 必須
- 最大200文字

### ruleType

- 必須

### resultFactKey

値がある場合:

```regex
^[a-zA-Z0-9_]+$
```

### priority

- 1以上必須

### dslType

nullの場合Validator内部ではMVEL扱い。

MVEL/JEXLならdslText必須。
JAVA_BEANならruleBeanName必須。

---

## 15. ruleName重複判定

Validator:

新規:

```text
existsByRuleNameAndDeletedAtIsNull(ruleName)
```

更新:

```text
existsByRuleNameAndIdNotAndDeletedAtIsNull(ruleName, id)
```

### 重要: Tenant条件

これらのRepository methodには `tenantId` が引数にない。

Entity DB Uniqueは:

```text
UNIQUE(tenant_id, rule_name)
```

であり、DB設計上はTenant単位のruleNameを想定している。

しかしApplication重複Validation method自体はtenantIdを明示しない。

**未決事項**: Hibernate tenantFilterがこのRepository呼出時に必ず有効であることを共通Tenant機構側で確定する必要がある。

もしfilterが適用されない経路があれば、他Tenantの同名Ruleを重複扱いする可能性がある。

今回は修正しない。

---

## 16. 一覧・詳細のTenant境界

`RuleMasterQueryService`:

一覧:

```text
findAllByDeletedAtIsNullOrderByIdAsc()
```

Active一覧:

```text
findByActiveFlagTrueAndDeletedAtIsNullOrderByIdAsc()
```

詳細:

```text
findByIdAndDeletedAtIsNull(id)
```

ruleName検索:

```text
findByRuleNameAndDeletedAtIsNull(ruleName)
```

いずれもCRUD画面経路ではtenantIdを明示していない。

一方同Repositoryには別用途として:

```text
findByTenantIdAndActiveFlagTrue...
findByTenantIdAndRuleTypeInAndActiveFlagTrue...
findByTenantIdAndRuleNameAndActiveFlagTrue...
```

も存在する。

**実装事実**: RuleMasterRepositoryでは「tenantId明示Query」と「Hibernate Filter依存とみられるQuery」が混在している。

**重要な確認対象**: 共通Tenant Filter有効化ライフサイクル。

後続の横断設計で必ず確認する。

---

## 17. 権限

`RuleMasterController` class全体:

```java
@PreAuthorize("hasRole('SYS_ADMIN')")
```

したがって:

- 一覧
- Active一覧
- 詳細
- ruleName検索
- 新規
- 更新
- 削除

すべてSYS_ADMIN専用。

---

## 18. Active→Inactive制御

更新時:

```text
現在 activeFlag=true
AND request.activeFlag=false
```

の場合だけReferenceCheckerを呼ぶ。

参照中なら:

```text
RuleConflictException
```

で拒否。

Message:

```text
参照中のRuleは無効化できません。
```

### 現在確認できるReference先

`RuleReferenceChecker`:

- 手当マスタ `AllowanceMasterRepository`
- 控除マスタ `DeductionMasterRepository`

**確定仕様（現行実装）**: 少なくとも手当・控除から参照中のRuleは無効化できない。

### 注意

Page説明には日報・月次明細等からRuleを利用するとあるが、ReferenceCheckerが現在確認するのは手当/控除のみ。

**未決事項**: 他機能からのruleName参照が存在する場合、その参照も無効化防止対象にすべきか。

後続の変更影響検索で確定する。

---

## 19. 削除Frontend

編集時のみ削除ボタン表示。

`useRulePage#remove(form)`:

```text
id <= 0 → return
DELETE /api/system/rules/{id}
rulesQuery.refetch()
Dialog close
```

### 確認ダイアログ

**実装事実**: `useRulePage` / `RuleEditDialog` の現在コードでは、削除前の `window.confirm()` 等は確認できない。

削除ボタン押下でそのままdelete emitへ進む。

**修正候補**: 誤操作防止確認を設けるか業務仕様確認。

今回は修正しない。

---

## 20. 削除Backend

`RuleMasterCommandService#delete()`:

```text
1. findByIdAndDeletedAtIsNull(id)
2. ReferenceChecker.findReferenceTypes(ruleName)
3. 参照ありならRuleConflictException
4. Instant now = Instant.now()
5. RuleMaster.deletedAt = now
6. Parameters.deletedAt = now
7. DataSources.deletedAt = now
8. Columns.deletedAt = now
```

明示repository.deleteは呼ばない。

**確定仕様**: Rule削除はsoft delete。

---

## 21. 削除時Reference制御

無効化と同様に、手当/控除から参照されていれば削除不可。

削除時は `findReferenceTypes()` の結果をMessageにも含める。

例概念:

```text
references=[手当マスタ, 控除マスタ]
```

---

## 22. 削除Clock

BackupではDI Clockを使用していたが、RuleMaster deleteは:

```java
Instant.now()
```

を直接使用する。

**実装事実**: Rule削除時刻はApplication Clock Beanを利用していない。

**修正候補**: 共通Clock戦略へ統一。

今回は修正しない。

---

## 23. 子Entityの保存方式 — CRUD上の重要事実

今回Parameter/DataSource詳細には入らないが、RuleMaster更新方式には重要なので記録する。

`RuleMasterMapper#applyRequest()`:

```text
entity.clearParameters()
→ request.parametersを全件new RuleParameter

entity.clearDataSources()
→ request.dataSourcesを全件new RuleDataSource
  → columnsも全件new RuleColumnMapping
```

`RuleMaster`:

```text
@OneToMany(cascade=ALL, orphanRemoval=true)
```

**実装事実**: 子定義は更新時に差分mergeせず全置換方式。

Frontend Requestには既存Parameter/DataSource/Column IDを送るが、Backend MapperはそれらのIDを利用していない。

これはbackupのColumn全置換と似た構造だが、Ruleでは3階層分ある。

詳細影響はParameter/DataSource設計時に掘る。

---

## 24. RuleMaster Entity

Table:

```text
rule_master
```

主要項目:

- id
- tenant_id（BaseEntity）
- rule_name
- rule_display_name
- rule_type
- dsl_type
- dsl_text
- rule_bean_name
- result_fact_key
- description
- priority
- active_flag
- created_at
- updated_at
- deleted_at

Unique:

```text
(tenant_id, rule_name)
```

Children:

```text
RuleMaster
├─ parameters
└─ dataSources
```

両方cascade ALL + orphanRemoval。

---

## 25. Mapperの基本default

Backend `applyRequest()`:

| 項目 | request null/不正時default |
|---|---|
| ruleType | GENERAL |
| dslType | MVEL |
| resultFactKey | result |
| priority | <=0なら100 |

ただしValidatorが先に実行されるため、例えばpriority<=0はMapperへ到達する前に拒否される。

**実装事実**: Mapper defaultとValidator制約が一部重複している。

---

## 26. Error分類

基本Validationでは以下が混在する。

- RuntimeException
- IllegalArgumentException
- RuleConflictException
- EntityNotFoundException

`system/rule` には専用 `RuleExceptionHandler` も存在する。

今回はCRUD基本だけなので詳細HTTP status/ErrorResponse mappingは後続の例外設計で確認する。

---

## 27. Transaction

### create

`@Transactional`

Validation → Parent/children構築 → save。

### update

`@Transactional`

Lookup → Validation → Reference確認 → 全置換Mapper → save。

### delete

`@Transactional`

Reference確認 → Parent/children soft delete。

**確定仕様**: RuleMasterと子定義の変更は1つのDB Transaction内。

---

## 28. 現時点の既知事項

### A. CRUD RepositoryのTenant明示が混在

重要度: 高・要確認

RuleMasterRepositoryにはtenantId明示methodがある一方、管理CRUDはtenant無しmethodを多用する。

Hibernate filterが保証しているか後で確定する。

修正しない。

### B. Frontend default JEXL / Backend default MVEL

重要度: 中

通常UI作成ではJEXLになる。

標準DSL方針を確認する。

修正しない。

### C. 一覧APIが詳細データ全体を返す

重要度: 性能/責務

編集時detail Queryは存在するが現在未使用。

修正しない。

### D. 削除確認UIなし

重要度: UX/誤操作

soft deleteだが参照されていないRuleは即削除APIへ進む。

修正しない。

### E. 削除ClockがInstant.now直呼び

重要度: 低〜中

Clock統一対象候補。

修正しない。

### F. ReferenceChecker対象は現在手当・控除のみ

重要度: 高・変更影響確認待ち

日報/月次明細等の参照実態をRepository全体検索する必要あり。

修正しない。

### G. 子定義は全置換

重要度: 中〜高

Parameter/DataSource/Column IDは更新時維持されない可能性が高い。

次回以降詳細化。

---

## 29. 次に掘る範囲

次は **RuleParameterだけ** を解析する。

まだDataSourceやDSL Executorへは入らない。

追跡順:

```text
RuleParameterTab.vue
→ ruleFormMapper.ts
→ RuleParameterSaveRequest / Response
→ RuleMasterValidator#validateParameters
→ RuleMasterMapper#toParameter / toParameterResponses
→ RuleParameter Entity
→ RuleParameterResolver
→ RuleValueConverter
```

ここで以下を確定する。

- Parameter追加・削除・並べ替え
- paramName
- dataType
- requiredFlag
- defaultValue
- orderNo
- Request parameter解決優先順位
- null/default/required
- String→型変換規則
- 更新時ID維持の有無
- Tenant/soft delete
- テスト保証
