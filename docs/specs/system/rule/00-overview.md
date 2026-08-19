# system/rule 詳細設計 00 — 全体地図

## 1. 調査基準

- 対象: `system/rule`
- 基準branch: `main`
- 基準commit: `12c91a72b409df16b9d4be0b416247a07a8f170a`
- 設計書作成branch: `agent/v1-common-architecture-spec`
- アプリケーションコード変更: なし

今回の目的は、`system/rule` を一気に詳細化せず、**機能全体を分解するための地図だけを作ること**。

以後もこの調査チャットではコード修正を行わず、不整合は「実装事実」「未決事項」「修正候補」として記録する。

---

## 2. 第一印象

**実装事実**: `system/rule` は単純なRuleマスタCRUDではない。

少なくとも以下の責務を1サブシステム内に持つ。

```text
Rule定義管理
├─ RuleMaster
├─ RuleParameter
├─ RuleDataSource
├─ RuleColumnMapping
└─ DSL本文/実行方式

実行エンジン
├─ RuleLoader
├─ RuleParameterResolver
├─ RuleFactBuilder
├─ GeneralDataFetcher
├─ RuleValueConverter
├─ DslExecutorDispatcher
│  ├─ MvelDslExecutor
│  ├─ JexlDslExecutor
│  └─ JavaBeanDslExecutor
└─ RuleExecutionService

補助Catalog
├─ RuleDataSourceCatalog
├─ RuleDataSourceCatalogColumn
├─ RuleBeanCatalogService
└─ RuleDataSourceCatalogService

管理UI
├─ Rule一覧
├─ Rule編集
├─ Parameter編集
├─ DataSource編集
├─ Column Mapping編集
└─ Rule実行テスト
```

したがってbackupよりも明確に、**設定管理系**と**実行エンジン系**を分離して読む必要がある。

---

## 3. Frontend全体構成

Root:

```text
frontend/src/features/system/rule/
```

配下:

```text
api/
components/
composables/
mapper/
pages/
types/
```

### 3.1 Page

`frontend/src/features/system/rule/pages/RuleManagementPage.vue`

画面タイトル:

```text
Rule管理
```

説明:

```text
控除・手当・日報・月次明細などで使用する計算Ruleを管理します。
```

**実装事実**: Ruleはsystem内部だけの技術設定ではなく、給与・日報・月次明細等の業務計算に利用する共通ルール機構として設計されている。

Pageは:

```text
RuleManagementPage
→ useRulePage
→ RuleTable
→ RuleEditDialog
```

という構造。

---

## 4. Frontend主要ファイル

### Page / Composable

- `pages/RuleManagementPage.vue`
- `composables/useRulePage.ts`

### API

- `api/useRulesQuery.ts`
- `api/useRuleDetailQuery.ts`
- `api/useRuleMutations.ts`
- `api/useExecuteRuleMutation.ts`
- `api/useRuleBeansQuery.ts`
- `api/useRuleDataSourceCatalogsQuery.ts`
- `api/queryKeys.ts`

### Components

- `RuleTable.vue`
- `RuleEditDialog.vue`
- `RuleParameterTab.vue`
- `RuleDataSourceTab.vue`
- `RuleTestTab.vue`

DataSource配下:

- `rule_datasource/RuleDataSourceList.vue`
- `rule_datasource/RuleDataSourceEditor.vue`
- `rule_datasource/RuleColumnList.vue`
- `rule_datasource/RuleColumnEditor.vue`

**実装事実**: 1つのRule編集Dialog内にParameter、DataSource、Column、実行テストまで含む構造であり、RuleMaster単体CRUDより編集対象が広い。

---

## 5. Backend全体構成

Root:

```text
backend/src/main/java/com/project/backend/features/system/rule/
```

配下:

```text
config/
context/
controller/
dto/
entity/
enums/
exception/
mapper/
repository/
service/
```

Service配下はさらに:

```text
builder/
converter/
executor/
fetcher/
loader/
validation/
```

へ分かれる。

---

## 6. Controller

確認できるController:

- `RuleMasterController`
- `RuleExecutionController`
- `RuleBeanCatalogController`
- `RuleDataSourceCatalogController`
- `RuleExceptionHandler`

### 6.1 RuleMasterController

Base:

```text
/api/system/rules
```

主要API:

```text
GET    /api/system/rules
GET    /api/system/rules/active
GET    /api/system/rules/{id}
GET    /api/system/rules/by-name/{ruleName}
POST   /api/system/rules
PUT    /api/system/rules/{id}
DELETE /api/system/rules/{id}
```

Controller class全体:

```java
@PreAuthorize("hasRole('SYS_ADMIN')")
```

**確定仕様**: Rule管理APIはSYS_ADMIN専用。

### 6.2 RuleExecutionController

Base:

```text
/api/system/rules/execution
```

実行:

```text
POST /fire
```

Request:

```text
RuleExecutionRequest
```

Service:

```text
RuleExecutionService#execute(ruleName, context)
```

ここもSYS_ADMIN Role制御あり。

---

## 7. Entity構成

確認できるEntity:

- `RuleMaster`
- `RuleParameter`
- `RuleDataSource`
- `RuleColumnMapping`
- `RuleDataSourceCatalog`
- `RuleDataSourceCatalogColumn`

### 概念分類

#### Rule定義本体

```text
RuleMaster
├─ RuleParameter
└─ RuleDataSource
    └─ RuleColumnMapping
```

#### DataSource候補Catalog

```text
RuleDataSourceCatalog
└─ RuleDataSourceCatalogColumn
```

**推測**: CatalogはRuleごとの実設定ではなく、Rule作成時に選択可能な安全なDataSource/Column候補を提供するためのメタデータ層とみられる。

この点は次回以降コードを読み、確定させる。

---

## 8. Repository

Rule専用Repositoryとして確認できるもの:

- `RuleMasterRepository`
- `RuleDataSourceCatalogRepository`

**実装事実**: Parameter/DataSource/ColumnMappingは専用Repositoryが見当たらず、RuleMaster Entity graphのcascade等で管理される可能性が高い。

この保存方式はRule CRUD詳細時に確認する。

---

## 9. Serviceの分類

### 9.1 Rule管理

- `RuleMasterQueryService`
- `RuleMasterCommandService`

### 9.2 Rule実行

- `RuleExecutionService`
- `RuleLoader`
- `RuleParameterResolver`
- `RuleFactBuilder`
- `DslExecutorDispatcher`

### 9.3 DSL Executor

共通interface:

- `DslExecutor`

実装:

- `MvelDslExecutor`
- `JexlDslExecutor`
- `JavaBeanDslExecutor`

**実装事実**: 少なくとも3種類の実行方式をDispatcherで切り替えるPlugin的構造を持つ。

### 9.4 Data取得

- `GeneralDataFetcher`
- `RuleDataSourceCatalogService`

### 9.5 値変換

- `RuleValueConverter`

### 9.6 Catalog

- `RuleBeanCatalogService`
- `RuleDataSourceCatalogService`

### 9.7 Validation / Safety

- `RuleMasterValidator`
- `RuleDslSafety`
- `RuleReferenceChecker`
- `RuleParameterResolver`

**重要**: Ruleは文字列DSLを実行するため、Validation/Safety層は単なる入力チェックではなくセキュリティ境界である可能性が高い。

これは後半で独立して詳細化する。

---

## 10. 実行処理の最上位フロー

`RuleExecutionService#execute()` から確認できる大枠:

```text
1. RuleLoader.loadActive(ruleName)
2. RuleParameterResolver.resolve(rule, request parameters)
3. RuleFactBuilder.build(rule, resolved parameters)
4. RuleExecutionContext生成
5. DslExecutorDispatcher.execute(context)
6. resultFactKeyへ結果をfactsに格納
7. RuleExecutionResultを返す
```

つまりRuleは単に:

```text
parameters → expression → result
```

ではなく、

```text
Request Parameter
→ Parameter解決/型変換
→ DataSource等からFact生成
→ DSL Executor
→ Result
```

という実行モデル。

---

## 11. 実行Transaction

`RuleExecutionService#execute()`:

```java
@Transactional(readOnly = true)
```

**実装事実**: Rule実行はreadOnly Transaction内で行われる。

少なくともRuleExecutionService自身は業務DB更新を目的としていない構造。

ただしJavaBean Executorが任意Beanを呼べる場合、そのBean内部の副作用有無は別問題なので後で確認する。

---

## 12. Rule実行結果

`RuleExecutionService` はDispatcher結果を:

```text
result
```

として返すだけでなく、RuleMasterの `resultFactKey` があればそのkey、なければ:

```text
result
```

というkeyでfacts Mapにも格納する。

Responseには少なくとも:

- ruleName
- executed
- result
- facts
- message

が含まれる。

**実装事実**: Rule test APIは最終結果だけでなく、生成されたfactsも呼出元へ返す。

---

## 13. UIと実行系の接点

Frontendには `RuleTestTab.vue` と `useExecuteRuleMutation.ts` がある。

したがって管理画面上でRule保存だけでなく、Ruleを実際に実行して結果を確認するテスト機能を持つ。

このテストタブは次回以降、Rule実行エンジン詳細と一緒に追う。

---

## 14. 現時点で分けるべきサブテーマ

`system/rule` は以下の単位に分割して調査する。

### A. RuleMaster CRUD

```text
RuleTable
→ RuleEditDialog基本情報
→ useRulePage
→ useRuleMutations
→ RuleMasterController
→ RuleMasterCommand/QueryService
→ Validator/Mapper
→ RuleMaster
```

### B. Parameter

```text
RuleParameterTab
→ RuleParameter
→ RuleParameterResolver
→ RuleValueConverter
```

### C. DataSource / Column Mapping

```text
RuleDataSourceTab
→ RuleDataSourceEditor/List
→ RuleColumnEditor/List
→ RuleDataSource / RuleColumnMapping
→ GeneralDataFetcher
```

### D. DataSource Catalog

```text
useRuleDataSourceCatalogsQuery
→ RuleDataSourceCatalogController
→ Service
→ Catalog Entity/Column
```

### E. DSL

```text
Rule DSL definition
→ RuleDslSafety
→ DslExecutorDispatcher
→ MVEL/JEXL/JavaBean
```

### F. Fact生成

```text
RuleFactBuilder
→ Parameter
→ DataSource
→ Column Mapping
→ RuleExecutionContext
```

### G. JavaBean Catalog

```text
useRuleBeansQuery
→ RuleBeanCatalogController
→ RuleBeanCatalogService
→ JavaBeanDslExecutor
```

### H. Rule実行テスト

```text
RuleTestTab
→ useExecuteRuleMutation
→ RuleExecutionController
→ RuleExecutionService
```

### I. 例外/Security/Tenant/Test

最後に横断総括する。

---

## 15. backupとの規模比較

### backup

中心モデル:

```text
Target
→ Columns
→ SQL
→ CSV/ZIP
→ Storage
→ History
```

### rule

中心モデル:

```text
RuleMaster
├─ Parameters
├─ DataSources
│  └─ ColumnMappings
├─ DSL / Executor Type
├─ Bean reference
├─ DataSource Catalog
└─ Result Fact

Execution
→ Parameter resolve
→ Fact build
→ DB fetch
→ Value conversion
→ DSL dispatch
→ MVEL/JEXL/JavaBean
```

**評価**: ruleはbackupより大きいだけでなく、**実行時に業務計算へ直接影響する共通基盤**なので、変更影響も広い可能性が高い。

特にRuleを給与・控除・日報・月次明細等から参照している箇所は、rule配下だけでなくRepository全体を検索して依存先を洗い出す必要がある。

これは後半の「変更影響」で実施する。

---

## 16. 現時点ではまだ確定しない事項

以下はファイル名・最上位コードだけでは確定しないため、今は推測しない。

- RuleMasterとParameter/DataSourceのcascade保存方式
- Rule更新時に子IDを維持するか
- Rule DSLの具体的な安全制限
- MVEL/JEXLで呼べるmethod/class範囲
- JavaBeanで呼べるBeanの選定方法
- GeneralDataFetcherが任意SQLを許すか
- DataSource Catalogがallowlistとして機能するか
- Tenant条件をDataSource queryへどう付与するか
- soft delete条件
- Parameter型変換規則
- null/default/requiredの処理
- 実行結果型
- Ruleから別Ruleを参照できるか
- 循環参照防止
- 実行履歴/監査の有無
- timeoutの有無
- DSL無限処理対策
- キャッシュ有無

これらは順番にコードを読み、確定仕様/実装事実/未決事項へ分類する。

---

## 17. 次に掘る範囲

最初の詳細解析は **RuleMasterの一覧・新規登録・編集・削除の「基本情報だけ」** とする。

まだParameter/DataSource/DSL実行には入らない。

追跡順:

```text
RuleManagementPage.vue
→ useRulePage.ts
→ RuleTable.vue
→ RuleEditDialog.vue の基本情報部分
→ useRulesQuery / useRuleDetailQuery / useRuleMutations
→ RuleMasterController
→ RuleMasterQueryService
→ RuleMasterCommandService
→ RuleMasterValidator の基本項目
→ Mapper
→ RuleMasterRepository
→ RuleMaster Entity
```

ここでまずRuleそのもののライフサイクルと不変項目、soft delete、Tenant境界を固める。
