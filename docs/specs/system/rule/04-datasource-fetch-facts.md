# system/rule 詳細設計 04 — DataSource実データ取得・Fact生成

## 1. 対象範囲

今回は `system/rule` のうち、**DataSource Catalogから実DBデータを取得し、DSLへ渡すFactを構築するまで**を対象とする。

対象経路:

```text
RuleDataSourceCatalogService
→ RuleDataSourceCatalogRepository
→ RuleFactBuilder
→ GeneralDataFetcher
→ NamedParameterJdbcTemplate
→ DB
→ RuleValueConverter
→ facts Map
```

基準コード:

- branch: `main`
- commit: `12c91a72b409df16b9d4be0b416247a07a8f170a`

**この調査ではコード修正を行わない。**

---

## 2. 計算入力生成の全体フロー

Rule実行時の計算入力は以下の順で作られる。

```text
Resolved Parameters
  ↓
RuleFactBuilder
  ├─ facts["params"] = parameters Map
  ├─ parametersをtop-level factsにも展開
  ↓
active RuleDataSourceをorderNo順に処理
  ↓
GeneralDataFetcher
  ├─ Catalog再読込
  ├─ physicalName決定
  ├─ whereClauseTemplate決定
  ├─ SELECT Column決定
  ├─ Tenant Parameter付与
  ├─ LIMIT付与
  └─ queryForList
  ↓
RuleFactBuilder
  ├─ singleRow → Map
  └─ list      → List<Map>
  ↓
Column Mapping
  ↓
RuleValueConverter
  ↓
facts[sourceName]
```

ここで作られたfactsが後段JEXL/MVEL/JavaBean計算の入力になる。

---

## 3. Catalog一覧

`RuleDataSourceCatalogService#findActive()`:

```text
findByActiveFlagTrueAndDeletedAtIsNullOrderBySourceCodeAsc()
```

Catalog Columnも:

```text
deletedAt == null
AND activeFlag == true
```

だけをResponseへ返す。

### Tenant境界

Repository methodにはtenantId引数がない。

RuleMaster CRUDと同様、共通Hibernate Tenant Filter等に依存している可能性がある。

**未決事項**: Catalog取得時のTenant Filter有効化を横断確認する。

---

## 4. Catalogは実行時に再読込される

`GeneralDataFetcher#loadCatalog(source)`:

```text
source.catalogCodeあり
→ catalogService.findRequired(catalogCode)
```

を毎実行時に行う。

その後:

```text
tableName   = catalog.physicalName
whereClause = catalog.whereClauseTemplate
maxRows     = catalog.maxRows
```

を使う。

**確定仕様（現行実装）**: RuleDataSource Entityに保存されたtableName / whereClauseより、catalogCodeが存在する場合は**現在のCatalog定義が実行時に優先される**。

したがってCatalogを変更すると、既存Ruleを再保存しなくても次回実行から計算入力が変わる。

### 計算影響

変更対象:

- physicalName
- whereClauseTemplate
- tenantScopedFlag
- maxRows
- Catalog active/deleted状態

は既存Ruleの計算結果へ即時影響しうる。

**重要**: Catalog変更はRule式変更と同等以上の変更影響を持つ。

---

## 5. Catalog無効化の影響

`findRequired()` は:

```text
sourceCode
AND activeFlag=true
AND deletedAt is null
```

のみ取得する。

そのため既存Ruleが参照するCatalogをinactive/soft-deleteすると、Rule実行時にEntityNotFoundExceptionとなる。

**実装事実**: Rule保存時にCatalog内容をsnapshot保持していても、Catalogが無効なら保存済みtableNameへfallbackしない。

---

## 6. SQL SELECT構造

`GeneralDataFetcher` が生成する基本形:

```sql
SELECT {columns}
FROM {tableName}
[WHERE {whereClause}]
LIMIT :__ruleLimit
```

JPAではなく:

```text
NamedParameterJdbcTemplate#queryForList
```

を使用する。

したがってHibernate tenantFilter / softDeleteFilterはこのSQLには自動適用されない。

---

## 7. tableName防御

`tableName` は:

```regex
^[a-zA-Z0-9_]+$
```

で検証される。

schema.tableやquote、空白、SQL記号は許可されない。

**確定仕様**: tableNameはidentifier allowlist方式。

---

## 8. SELECT Column決定

`selectColumns(source)`:

### Column Mappingあり

```text
source.columns
→ deletedAt == null
→ columnName identifier validation
→ distinct
→ comma join
```

例:

```sql
SELECT hourly_wage, employee_id FROM employee_master ...
```

### Column Mappingなし

```sql
SELECT *
```

となる。

---

## 9. Column Mapping 0件時の重要挙動

FactBuilder側もColumn Mappingが0件なら:

```text
mapped.putAll(row)
```

を行う。

つまり:

```text
Column Mappingなし
→ SELECT *
→ DB Row全Column
→ Fact Mapへそのまま格納
```

となる。

### Catalog方式との関係

Rule保存時Catalog Validatorは「RequestにColumnがある場合」はCatalog allowlist/typeを確認するが、Column 0件自体は禁止していない。

そのためCatalog方式でもColumn Mappingを空にすると、Catalog Column allowlistを使用せず物理tableの全Columnを取得・Fact化する可能性がある。

**既知事項 — 優先度高**:

> Catalog Column allowlistを安全境界と考えた場合、Column Mapping 0件の `SELECT * / putAll(row)` はその境界を迂回する可能性がある。

今回は修正しない。

---

## 10. WHERE句

Catalog方式:

```text
catalog.whereClauseTemplate
```

をSQL文字列へそのまま連結する。

Direct方式:

```text
source.whereClause
```

を使用する。

### Parameter bind

WHERE句中の:

```text
:paramName
```

はNamedParameterJdbcTemplateがbindする。

queryParameters初期値にはresolved parameters Map全体をcopyする。

したがってRuleParameterで解決された値をWHERE条件に使用できる。

例:

```text
whereClauseTemplate:
employee_id = :employeeId

parameters:
employeeId = 123
```

---

## 11. WHERE句の安全性

GeneralDataFetcher自身はwhereClauseを再Validationせず、そのままSQLへ連結する。

Catalog方式ではCatalogを信頼済み設定として扱う構造。

Direct方式はRule保存時 `RuleMasterValidator#validateWhereClause()` で危険keyword等を拒否する。

### 重要

**実装事実**: CatalogのwhereClauseTemplate変更時にGeneralDataFetcherでSQL Safety再検証する処理はない。

Catalogを登録・更新する経路のValidationが安全境界になる。

Catalog管理経路は後続で確認する。

---

## 12. Tenant条件

Catalog `tenantScopedFlag=true` の場合:

```text
TenantContext.getTenantId()
```

を取得する。

blankなら実行拒否。

さらにwhereClauseに文字列として:

```text
:tenantId
```

が含まれることを必須とする。

含まれなければ:

```text
テナント対象のRuleデータソースに:tenantId条件がありません。
```

で拒否。

その後:

```text
queryParameters["tenantId"] = TenantContext tenantId
```

を強制上書きする。

**確定仕様**: 呼出元ParameterでtenantIdを偽装しても、tenantScoped CatalogではTenantContext値で上書きされる。

---

## 13. Tenant条件は自動追加ではない

重要な設計。

GeneralDataFetcherは:

```sql
AND tenant_id = :tenantId
```

を自動追加しない。

代わりにCatalog whereClauseTemplate内に `:tenantId` が存在することだけを確認する。

したがって実際の条件列はCatalog定義に依存する。

例として正しい想定:

```sql
tenant_id = :tenantId AND employee_id = :employeeId
```

しかし単に:

```sql
some_other_column = :tenantId
```

でも文字列contains判定自体は通る。

**既知事項 — セキュリティ上要確認**:

`:tenantId` placeholderの存在は保証するが、それが本当に `tenant_id` 列へ比較されていることまではFetcherは検証しない。

今回は修正しない。

---

## 14. soft delete条件

GeneralDataFetcherは:

```text
deleted_at IS NULL
```

を自動追加しない。

したがって対象tableがsoft deleteを採用していても、Catalog whereClauseTemplateに条件が無ければdeleted Rowも計算入力へ入る。

**確定仕様（現行実装）**: soft delete除外はCatalog WHERE定義に依存する。

これは給与・手当等の計算値へ直接影響しうるため、実Catalog定義の精査が必要。

---

## 15. maxRows

### singleRowFlag=true

論理maxRows = 1。

SQL LIMITは:

```text
2
```

とする。

理由は1行だけ取得して黙って切り捨てず、2行目の存在を検知するため。

### singleRowFlag=false + Catalogあり

Catalog.maxRowsを使用。

許容範囲:

```text
1 ～ 1000
```

範囲外ならIllegalStateException。

SQL LIMIT:

```text
maxRows + 1
```

として、上限超過を検知する。

### Direct方式

Catalogなしlist DataSourceはmaxRows=1000固定。

---

## 16. singleRow 0件

DataFetcher結果が0件の場合、FactBuilderは:

```text
row = Map.of()
```

としてmapRowする。

### Column Mappingあり

各columnについて:

```text
row.get(columnName) → null
```

required=trueならError。

required=falseなら:

```text
factKey → null
```

をMapへ格納する。

### Column Mappingなし

空MapがそのままFactになる。

**確定仕様**: singleRowで0件はDataSource自体のエラーではなく、Column required条件によって結果が決まる。

---

## 17. singleRow 2件以上

SQLはLIMIT 2。

rows.size()>1ならFactBuilderで:

```text
singleRowFlagのデータソースが複数行を返しました。
```

として実行失敗。

**確定仕様**: singleRowは「先頭1件を採用」ではなく、複数行を異常として検出する。

これは計算の再現性上重要な仕様。

---

## 18. list 0件

list DataSourceでは0件なら:

```text
facts[sourceName] = []
```

空Listになる。

---

## 19. list 上限超過

Fetcherが `maxRows + 1` 件取得し:

```text
rows.size() > maxRows
```

ならError。

FactBuilderにもさらに:

```text
rows.size() > 1000
```

の固定上限チェックがある。

Catalog maxRowsは最大1000なので、通常Catalog経路ではFetcher側チェックが先に効く。

**実装事実**: maxRows制御がFetcherとFactBuilderで二段存在する。

---

## 20. ORDER BY

GeneralDataFetcherはORDER BYを自動付与しない。

whereClauseTemplate自体は名称上WHERE句であり、保存ValidationではSQL keyword制御があるため、通常ORDER BYを記述する設計ではないと考えられる。

**実装事実**: DataSource取得Row順を明示保証するコードはGeneralDataFetcherにない。

### 計算影響

list FactをDSLで:

- 先頭行参照
- 最終行参照
- 順序依存処理

する場合、DB返却順に依存する可能性がある。

**未決事項**: list DataSourceのRow順を正式に不定とするか、Catalogにsort仕様が別途あるか。

---

## 21. DB値→Fact型変換

Column Mappingありの場合:

```text
DB value
→ RuleValueConverter.convert(value, column.dataType)
→ mapped[factKey]
```

Parameterと同じ変換規則を使う。

### null

RuleValueConverterはnullならnullを返す。

ただしその前にrequiredFlagを確認する。

### required=true

DB値がnullなら実行Error。

### required=false

factKey自体はMapに残り:

```text
factKey → null
```

となる。

Parameter optional missingの「key削除」と挙動が異なる。

---

## 22. Fact構造 — Parameters

RuleFactBuilderの最初:

```text
facts["params"] = params
facts.putAll(params)
```

したがってParameterは2経路で参照できる構造。

例:

```text
params.employeeId
employeeId
```

Executorごとの具体的参照可能性はDSL解析で確定する。

---

## 23. Fact構造 — single DataSource

例:

```text
sourceName = employee
columns:
  hourly_wage → hourlyWage
  age         → age
```

生成概念:

```text
facts = {
  params: {...},
  employeeId: 123,
  employee: {
    hourlyWage: BigDecimal(...),
    age: Integer(...)
  }
}
```

つまりDSLからの想定参照:

```text
employee.hourlyWage
```

---

## 24. Fact構造 — list DataSource

複数行の場合:

```text
facts[sourceName] = List<Map<String,Object>>
```

例:

```text
attendance: [
  { workDate: ..., hours: ... },
  { workDate: ..., hours: ... }
]
```

DSL側でlist aggregateをどう記述するかはJEXL/MVEL解析で詳細化する。

---

## 25. sourceNameとParameter名の衝突

FactBuilder処理順:

```text
1. facts["params"] = params
2. facts.putAll(params)
3. DataSourceを処理し facts[sourceName] = ...
```

したがってDataSource.sourceNameがtop-level Parameter名と同じ場合、**DataSource FactがParameter Factを上書きする**。

例:

```text
Parameter: employee
DataSource sourceName: employee
```

最終facts.employeeはDataSourceになる。

ValidatorはParameter名とsourceNameの相互重複を確認しない。

**既知事項 — 計算仕様上重要**: Parameter/DataSource namespace衝突が可能。

今回は修正しない。

---

## 26. sourceName = params の場合

さらに `sourceName="params"` もidentifierとして有効で、予約語禁止はない。

この場合DataSource処理で:

```text
facts["params"]
```

を上書きする。

結果、元Parameter Mapへのnamespaced参照が失われる。

**既知事項 — 優先度高**: `params` が予約Fact keyとして保護されていない。

今回は修正しない。

---

## 27. DataSource処理順

RuleFactBuilderは:

```text
activeFlag=true
AND deletedAt=null
```

のDataSourceのみを:

```text
orderNo ASC
```

で処理する。

orderNo重複時は明示tie-breakerがない。

ただし各DataSourceが異なるsourceNameなら通常Fact上書きは起きない。

sourceName重複は保存Validatorで禁止される。

---

## 28. DataSource間依存

Fetcherへ渡すparamsはRule Parameter Mapのみ。

先に生成したDataSource Factを後続DataSource WHERE parameterとして渡していない。

つまり現行FactBuilderでは概念上:

```text
DataSource A結果
→ DataSource BのWHERE bind
```

という連鎖はできない。

DataSourceは全て同一resolved parametersを入力に独立取得する。

**確定仕様（現行実装）**: DataSource間のDB取得依存関係はない。

---

## 29. query parameter extras

GeneralDataFetcherはresolved params Map全体をNamedParameterJdbcTemplateへ渡す。

WHEREで使わないParameterもMapに含まれる。

NamedParameterJdbcTemplateはSQL中で参照されるnamed parameterのみをbind対象として扱う構造なので、Rule側は共通Parameter Setを複数DataSourceへ渡せる。

---

## 30. Transaction

上位 `RuleExecutionService#execute()` は:

```java
@Transactional(readOnly = true)
```

その中で全DataSource取得を行う。

**実装事実**: Rule実行中のJDBC SELECT群はSpring readOnly Transactionの呼出範囲内。

ただしDB isolation level / consistent snapshotの具体保証はDatasource/Transaction設定次第なので、複数DataSourceが完全同一snapshotかはこのコードだけでは断定しない。

---

## 31. Catalog変更と計算再現性

重要な実装特性。

Rule実行時Catalogを再読込するため、同じ:

- ruleName
- RuleMaster version相当
- Parameters

でも、Catalogが途中で変更されると結果が変わりうる。

現行EntityにはRule/Catalogのversion snapshotやeffective dateは確認できない。

**未決事項**: 過去計算の再現性が必要な給与計算等で、Catalog変更履歴をどう扱うか。

後で呼出元と監査機構を確認する。

---

## 32. soft delete Rowと計算

Fetcherが自動でdeleted_atを除外しないため、CatalogごとにWHEREを確認しないと計算仕様は確定できない。

したがって後続では**実際に登録されているRuleDataSourceCatalogの初期データ/DDL/Runtime schema SQL**を検索し、Catalogごとに:

```text
physicalName
whereClauseTemplate
tenantScopedFlag
maxRows
columns
```

を一覧化する必要がある。

---

## 33. 現時点で特に重要な既知事項

### A. Column Mapping 0件でSELECT * / 全Column Fact化

重要度: 高

Catalog allowlist迂回の可能性。

修正しない。

### B. Tenantは`:tenantId`存在確認のみ

重要度: 高

実際にtenant_id列へ比較している保証はCatalog定義依存。

修正しない。

### C. soft delete除外はCatalog依存

重要度: 高・計算値影響

修正しない。

### D. Catalog変更が既存Ruleへ即時反映

重要度: 高・計算再現性

修正しない。

### E. Parameter/DataSource Fact namespace衝突

重要度: 高

DataSourceがtop-level Parameterを上書き可能。

修正しない。

### F. `params`予約keyも上書き可能

重要度: 高

修正しない。

### G. list Row順が不定

重要度: 中〜高

順序依存計算がある場合結果へ影響。

修正しない。

### H. DataSource間の取得依存なし

仕様上重要。

全DataSourceはresolved parametersから独立取得。

---

## 34. 次に掘る範囲

次は **DSL実行前のFact仕様を完成させるため、実Catalog定義とRuleの実登録データを調査する**。

具体的にはRepository全体から:

```text
rule_data_source_catalog
rule_data_source_catalog_column
rule_master
rule_parameter
rule_data_source
rule_column_mapping
```

の初期投入SQL/runtime schema/test fixtureを探す。

ここで実際の業務計算について:

- どのRuleが存在するか
- ALLOWANCE / DEDUCTION / PAYROLL等の具体Rule
- 各RuleのParameters
- 使用Catalog
- physical table
- WHERE
- Column→Fact
- DSL text

を洗い出す。

その後、**JEXL / MVEL / JavaBeanの演算仕様をエンジン別に解析し、実Ruleごとの計算式を1本ずつ詳細化する。**
