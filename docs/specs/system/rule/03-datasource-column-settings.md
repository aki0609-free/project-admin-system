# system/rule 詳細設計 03 — RuleDataSource / RuleColumnMapping 設定構造

## 1. 対象範囲

今回は `system/rule` のうち、**RuleDataSourceとRuleColumnMappingの設定構造だけ**を対象とする。

まだ `GeneralDataFetcher` が実際に発行するSQLや、JEXL/MVELでの計算式評価には入らない。

対象経路:

```text
RuleDataSourceTab.vue
→ RuleDataSourceList.vue
→ RuleDataSourceEditor.vue
→ RuleColumnList.vue
→ RuleColumnEditor.vue
→ ruleFormMapper.ts
→ RuleMasterValidator#validateDataSources / validateColumns
→ RuleMasterMapper#toDataSource / toColumnMapping
→ RuleDataSource Entity
→ RuleColumnMapping Entity
→ RuleDataSourceCatalog / CatalogColumn
```

基準コード:

- branch: `main`
- commit: `12c91a72b409df16b9d4be0b416247a07a8f170a`

**この調査ではコード修正を行わない。**

---

## 2. 計算仕様との関係

RuleDataSourceは、Rule計算式へ投入するDBデータの取得元定義。

RuleColumnMappingは、取得したDB物理ColumnをDSLから参照するFactへ変換する定義。

概念:

```text
DB table
  ↓
RuleDataSource
  ↓
physical column
  ↓
RuleColumnMapping
  ↓
factKey
  ↓
sourceName.factKey
  ↓
JEXL / MVEL / JavaBean計算
```

したがって計算仕様を正確に把握するには、式だけではなく以下すべてを追う必要がある。

- どのtableを読むか
- WHERE条件
- tenant条件
- 何行取るか
- どのColumnを使うか
- DB値を何型へ変換するか
- どのfactKey名になるか
- single/listでFact形がどう変わるか
- 最終DSLがそのFactをどう計算するか

後続設計ではこの順番で追跡する。

---

## 3. Frontend DataSource画面

`RuleDataSourceTab.vue` は3領域に近い構造。

```text
左:
  RuleDataSourceList

右上:
  RuleDataSourceEditor

右下:
  RuleColumnList
  RuleColumnEditor
```

選択状態:

- selectedDataSource
- selectedColumn
- selectedCatalog

DataSourceを選び、そのDataSourceに属するColumn Mappingを編集する。

---

## 4. DataSourceの意味

`RuleDataSourceList.vue` の画面説明:

```text
Ruleで使用する取得元データ
```

つまりDataSourceは外部接続設定ではなく、**Rule計算で使用するDB取得元**。

1つのRuleMasterに複数DataSourceを持てる。

---

## 5. DataSource追加

Frontend:

```text
createEmptyRuleDataSource(dataSources.length + 1)
→ form.dataSources.push()
→ 新規DataSourceを選択
```

初期値:

| 項目 | 値 |
|---|---|
| id | 一時負数 |
| sourceName | '' |
| catalogCode | '' |
| tableName | '' |
| whereClause | '' |
| singleRowFlag | true |
| activeFlag | true |
| orderNo | 末尾+1 |
| columns | [] |

---

## 6. DataSource削除

Frontend配列から除外するだけ。

専用DELETE APIはない。

RuleMaster全体保存時にBackend MapperがDataSourceを全置換する。

---

## 7. DataSource Editorの入力項目

現行UIで編集できるもの:

- sourceName
- catalogCode
- orderNo
- singleRowFlag
- activeFlag

### 重要

`tableName` と `whereClause` は現行DataSourceEditor上では入力欄がない。

画面にも:

```text
物理テーブルと抽出条件はデータソースカタログ側で管理されます。
```

と表示される。

**確定仕様（現行UI）**: 管理画面利用者は通常、物理table名やWHERE句をRuleごとに直接入力せず、DataSource Catalogを選択する。

---

## 8. catalogCode

DataSourceは `catalogCode` を持つ。

Catalog選択時Backend MapperはRequestのtableName/whereClauseをそのまま使わず:

```text
RuleDataSourceCatalogService.findRequired(catalogCode)
→ tableName = catalog.physicalName
→ whereClause = catalog.whereClauseTemplate
```

へ置き換える。

**確定仕様**: catalogCodeが設定されている場合、実保存されるtableName / whereClauseはCatalogが正本。

これはRuleごとの任意SQL入力を抑制する重要な安全境界。

---

## 9. catalogCode無し経路

Backend Mapper / ValidatorにはcatalogCodeが無い場合のlegacy/direct設定経路が残る。

その場合:

- tableName必須
- tableName identifier validation
- whereClause safety validation

を行う。

ただし現行Frontend DataSourceEditorにはtableName / whereClause入力UIがない。

**実装事実**: Backend APIとしてはdirect DataSource設定を受けられる構造が残るが、通常UIはCatalog方式。

**未決事項**: direct設定を正式サポートとして残すか、互換経路なのか。

今回は修正しない。

---

## 10. sourceName

DataSourceをDSL Fact namespaceとして識別する名前。

Backend Validation:

- 必須
- `^[a-zA-Z0-9_]+$`
- 同一Rule内で重複不可

RuleColumnMapping Entityコメントから、Column Factは概念上:

```text
sourceName.factKey
```

で参照する設計。

例:

```text
sourceName = employee
factKey = hourlyWage

DSL参照イメージ:
employee.hourlyWage
```

具体的なFact Map形状はRuleFactBuilder解析で確定する。

---

## 11. singleRowFlag

DataSource Entity:

```text
single_row_flag
```

default true。

Frontendでは:

```text
single
list
```

として表示する。

**推測ではなく構造上確定していること**: DataSourceは「1行取得」と「複数行取得」を区別する設計。

ただし実際に:

- 0行時何になるか
- 2行以上なのにsingle=trueの場合どうなるか
- list時どの型のFactになるか

は `GeneralDataFetcher / RuleFactBuilder` 解析で次回以降確定する。

---

## 12. activeFlag

DataSourceごとにactive/inactiveを持つ。

RuleMaster自体のactiveFlagとは別。

**実装事実**: Ruleを有効のまま、特定DataSourceだけ無効化できるモデル。

実行時にinactive DataSourceを除外するかはRuleFactBuilderで確認する。

---

## 13. orderNo

DataSourceにもorderNoあり。

Frontendはnumber input直接編集。

上/下移動・自動resequenceはない。

Backend Validator:

```text
orderNo > 0
```

のみ。

DataSource同士のorderNo重複は禁止していない。

**既知事項**: orderNo一意性・連番性は保証されない。

---

## 14. Column Mappingの役割

`RuleColumnList.vue` の説明:

```text
DBカラムをFactキーへ変換します。
```

RuleColumnMappingは:

```text
physical columnName
→ factKey
```

のmapping。

例:

```text
DB column: hourly_wage
Fact key: hourlyWage
```

DSL側ではsourceNameと組み合わせて参照する設計。

---

## 15. Column追加

選択DataSourceに対して:

```text
createEmptyRuleColumn(columns.length + 1)
→ columns.push()
```

初期値:

| 項目 | 値 |
|---|---|
| id | 一時負数 |
| columnName | '' |
| factKey | '' |
| dataType | STRING |
| requiredFlag | false |
| orderNo | 末尾+1 |

---

## 16. Column選択UI

Catalogを選択すると `selectedCatalog.columns` が `RuleColumnEditor` へ渡る。

columnNameはtext inputではなくselect。

Catalog Columnを選択すると:

```text
column.columnName = selected.columnName
column.dataType = selected.dataType
```

を同時設定する。

さらにdataType selectはdisabled。

**確定仕様（現行UI）**: Catalog方式では利用者がColumnのdataTypeを任意変更できず、Catalog定義に従う。

---

## 17. Column Validator — Catalog方式

`RuleMasterValidator#validateCatalogDataSource()` はCatalogのactiveかつdeletedAt=nullのColumn Mapを作る。

各RuleColumnMappingについて:

1. Catalogで許可されたcolumnNameか
2. CatalogColumn.dataTypeとMapping.dataTypeが一致するか

を確認する。

したがってFrontend改ざん/API直接呼出でも、Catalog外ColumnやdataType変更をBackendで拒否する。

**確定仕様**: CatalogがColumn allowlistとして機能する。

---

## 18. Column基本Validation

Catalog有無にかかわらず:

### columnName

- 必須
- identifier形式

### factKey

- 必須
- identifier形式
- 同一DataSource内で重複不可

### dataType

- 必須

### orderNo

- 1以上

### identifier

```regex
^[a-zA-Z0-9_]+$
```

---

## 19. factKey重複範囲

ValidatorはDataSourceごとに新しいHashSetを作るため、factKey重複禁止は**同一DataSource内**。

異なるsourceNameなら同じfactKeyを使える。

例:

```text
employee.amount
company.amount
```

は設計上可能。

---

## 20. requiredFlag — Column

RuleColumnMappingにもrequiredFlagがある。

これはParameter requiredFlagとは別。

意味としては、DBから取得した行に対象Column値が存在しない/null等の場合の扱いに使用すると考えられる。

具体的な判定は `GeneralDataFetcher / RuleFactBuilder` で確定するため、今回は断定しない。

---

## 21. dataType — Column

RuleColumnMappingのdataTypeはRuleDataType:

- STRING
- INTEGER
- LONG
- DECIMAL
- BOOLEAN
- DATE
- DATETIME

Catalog方式ではCatalogColumnと完全一致必須。

Parameterと異なり、Column dataTypeは**DB取得値をFactへ変換する仕様**に直結する可能性が高い。

次回Fetcher解析で `RuleValueConverter` がどこで適用されるか確認する。

---

## 22. DataSource Catalog

Entity:

`RuleDataSourceCatalog`

主要項目:

- sourceCode
- displayName
- physicalName
- whereClauseTemplate
- tenantScopedFlag
- maxRows
- description
- activeFlag
- columns

Unique:

```text
(tenant_id, source_code)
```

---

## 23. Catalogが計算仕様へ与える影響

Catalogは単なる画面候補一覧ではない。

計算時に取得可能なデータ範囲を定義する。

### physicalName

どの物理table/viewを読むか。

### whereClauseTemplate

どのRowを抽出するか。

### tenantScopedFlag

Tenant条件を強制するためのフラグとみられる。
実SQLでの利用方法はFetcher解析で確定する。

### maxRows

取得上限。
計算対象Row数へ直接影響する。

### columns

Ruleから利用可能なDB Column allowlist。

**重要**: 正確な計算仕様書にはDSL式だけでなくCatalog定義も含める必要がある。

---

## 24. Catalog Column

`RuleDataSourceCatalogColumn`:

- columnName
- displayName
- dataType
- orderNo
- activeFlag

Unique:

```text
(tenant_id, catalog_id, column_name)
```

Rule設定時に利用できるColumnと型を統制する。

---

## 25. tableName / whereClauseの保存

Catalog方式ではMapperがCatalog値をRuleDataSource Entityへcopyする。

つまりRuleDataSourceはcatalogCodeだけでなく:

- tableName
- whereClause

もsnapshot的に保持する。

### 重要な未決事項

Catalog設定を後から変更した場合、既存RuleDataSourceの保存済みtableName/whereClauseが自動更新されるかは、このMapperだけではない。

実行時に:

- RuleDataSource保存値を使うのか
- catalogCodeからCatalogを再解決するのか

をFetcher解析で確認する必要がある。

これは計算結果がCatalog変更後に変わるタイミングに直結する。

---

## 26. DataSource / Column保存方式

RuleMaster更新時:

```text
entity.clearDataSources()
→ request.dataSourcesをorderNo ASC
→ 全件new RuleDataSource
→ 各DataSource columnsも全件new RuleColumnMapping
```

Frontend Requestに既存IDはあるがBackend MapperはIDを使用しない。

**確定仕様（現行実装）**: Rule更新時、DataSource ID / ColumnMapping IDは維持されない可能性が高い。

---

## 27. orphanRemoval

RuleMaster → RuleDataSource:

```text
cascade = ALL
orphanRemoval = true
```

RuleDataSource → RuleColumnMappingも同様。

したがってRule更新の全置換では旧子Entityがorphan扱いになる。

BaseEntityのsoft delete思想との整合は後のDB/JPA横断確認事項。

---

## 28. orderNo重複

Parameterと同様、DataSource / Column Mappingも:

- orderNo > 0

のみで重複禁止なし。

Frontendも直接number入力。

**既知事項**: 同一orderNoが複数存在可能。

後続でsort安定性や計算順への影響を見る必要がある。

---

## 29. 計算仕様を今後どこまで追うか

ユーザー要望に基づき、計算仕様は通常の設計書より細かく記載する。

後続ではRuleごとに可能な限り以下まで追う。

```text
入力Parameter
  ↓ 型変換/default
DataSource Catalog
  ↓ physical table/view
WHERE template
  ↓ bind parameter
Tenant条件
  ↓
取得Row
  ↓ single/list判定
Column Mapping
  ↓ dataType変換
Fact構造
  ↓
DSL式
  ↓ 演算順序
  ↓ null条件
  ↓ 丸め
  ↓ 比較/条件分岐
  ↓
Result
  ↓ resultFactKey
  ↓ 呼出元業務機能
```

特に給与・控除・手当等の金額計算では:

- BigDecimalかdoubleか
- scale
- rounding mode
- 小数切捨て/四捨五入
- 税率/率計算
- 上限/下限
- 日付境界
- null/0
- 条件分岐順

までコード根拠で記載する。

---

## 30. 現時点の既知事項

### A. Catalog方式が安全境界

重要度: 高

物理table/where/Column/typeをCatalogが統制。

### B. Backendにはdirect table/where経路も残る

重要度: 高・要仕様確認

通常UIからは使えない。

修正しない。

### C. Catalog変更と既存Ruleへの反映タイミング未確定

重要度: 高・計算結果影響

Fetcherで確認する。

### D. DataSource/Column ID全置換

重要度: 中

修正しない。

### E. orderNo重複可能

重要度: 中

実行順への影響を後で確認。

### F. factKeyはDataSource内だけ一意

重要度: 仕様上重要

sourceName namespaceと組み合わせる設計。

---

## 31. 次に掘る範囲

次は **DataSource CatalogとGeneralDataFetcherの「実データ取得仕様」** を解析する。

ここから計算仕様の入力側へ本格的に入る。

追跡順:

```text
RuleDataSourceCatalogController
→ RuleDataSourceCatalogService
→ RuleDataSourceCatalogRepository
→ Catalog / CatalogColumn
→ RuleFactBuilder
→ GeneralDataFetcher
→ RuleValueConverter
```

次回確定する項目:

- Catalog一覧のTenant境界
- active/deleted filtering
- physicalNameの使われ方
- whereClauseTemplateのbind parameter
- Tenant条件の強制方法
- SQL組立
- SQL injection防御
- maxRows
- singleRow/list
- 0件/複数件
- Column null/required
- DB値→RuleDataType変換
- Fact Map/Listの正確な形
- DataSource処理順
- Transaction
- テスト保証
