# system/rule 詳細設計 05 — 適用場所・whereClause bind・汎用性/ハードコード確認

## 1. 対象範囲

この文書は、Ruleの「どこで適用するか」「従業員単位などの絞り込みが可能か」「汎用エンジン内に特定業務ハードコードがあるか」を整理する。

基準コード:

- branch: `main`
- commit: `12c91a72b409df16b9d4be0b416247a07a8f170a`

**この調査ではコード修正を行わない。**

---

## 2. employeeIdのようなwhereClause parameterは利用可能か

**結論: 利用可能。**

RuleExecutionServiceは `RuleContextRequest.parameters` をRuleParameterResolverで解決し、そのMapをRuleFactBuilderへ渡す。

RuleFactBuilderはそのresolved parameter MapをGeneralDataFetcherへ渡す。

GeneralDataFetcherは:

```text
queryParameters.putAll(params)
```

した後、NamedParameterJdbcTemplateへ渡す。

したがってCatalogの `whereClauseTemplate` にnamed parameterを書ける。

例:

```sql
tenant_id = :tenantId
AND employee_id = :employeeId
```

実行時Request:

```json
{
  "parameters": {
    "employeeId": 123
  }
}
```

とすれば、`employeeId` をbind parameterとして利用できる。

---

## 3. tenantIdとの違い

employeeId等の通常Parameterは呼出元から渡される。

一方tenantScoped CatalogではtenantIdだけは特別扱い。

GeneralDataFetcherが:

```text
TenantContext.getTenantId()
```

を取得し:

```text
queryParameters["tenantId"] = TenantContext tenantId
```

で強制上書きする。

したがって呼出元が偽のtenantIdを渡してもtenantScoped Catalogでは利用されない。

**確定仕様**:

- employeeId: Rule実行Context由来
- tenantId: Security/TenantContext由来

---

## 4. 従業員ごとの手当・控除計算イメージ

現行Rule engineだけで以下の構造を作れる。

```text
業務側
  ↓ employeeId等をRule parameterとして指定
RuleExecutionService
  ↓
RuleParameterResolver
  ↓ employeeId型変換
GeneralDataFetcher
  ↓
Catalog whereClauseTemplate
  tenant_id = :tenantId
  employee_id = :employeeId
  ↓
対象従業員DB Row取得
  ↓
Column Mapping
  ↓
Fact
  ↓
DSL計算
```

したがって「従業員AにはAのDB情報で同じRuleを計算する」という利用方法はエンジン構造上可能。

### 注意

実際にどのCatalogがemployeeId parameterを定義しているかは、実Catalog投入データの調査が必要。

機構として可能であることと、現在の初期Catalogがそのように定義されていることは分けて扱う。

---

## 5. Ruleの適用場所をsystem/ruleが制御するか

**結論: 基本的には制御しない。**

`RuleExecutionService` の公開的な入口は概念上:

```text
execute(ruleName, contextRequest)
```

である。

Rule engine自身は:

- どの画面から呼ばれたか
- 給与計算中か
- 日報計算中か
- 手当計算中か
- 控除計算中か

をExecutionContextへ持っていない。

`RuleExecutionContext` が持つものは:

- RuleMaster
- parameters
- facts

のみ。

**確定仕様（現行構造）**: 適用場所・呼出タイミングはRule engine外側の業務機能が決める。

---

## 6. 手当・控除との紐付け

`AllowanceMaster` は:

```text
rule_name
```

を持つ。

`DeductionMaster` も:

```text
rule_name
```

を持つ。

つまり:

```text
手当マスタ → ruleName
控除マスタ → ruleName
```

という参照モデル。

Rule側が「自分はこの手当に所属する」と保持するのではなく、**業務マスタ側がRuleを参照する向き**。

これは汎用Rule engineとして自然な依存方向。

---

## 7. RuleTypeは適用場所制御か

RuleMasterには:

- ALLOWANCE
- DEDUCTION
- DAILY_REPORT
- MONTHLY_DETAIL
- PAYROLL
- GENERAL

というRuleTypeがある。

しかし今回確認した `RuleExecutionService` はRuleTypeを見て適用可否を分岐していない。

**実装事実**: RuleTypeは分類metadataとして存在するが、RuleExecutionService自体の適用場所制御には使われていない。

後続でRuleLoader/呼出元がRuleTypeを利用しているかRepository全体を確認する。

---

## 8. 汎用性の評価

### 汎用的に作られている部分

#### 8.1 Rule名指定実行

```text
execute(ruleName, parameters)
```

型なので業務画面固定ではない。

#### 8.2 ParametersがMap

`RuleContextRequest` は:

```text
Map<String,Object> parameters
```

のみ。

employeeId、targetDate、hours、amount等をRule engine本体へJava field追加せず拡張可能。

#### 8.3 DataSourceがmetadata駆動

- Catalog
- table/view
- whereClauseTemplate
- Column Mapping
- factKey

で計算入力を定義する。

#### 8.4 DSL Executor抽象化

- JEXL
- MVEL
- JAVA_BEAN

をDispatcherで切り替える。

#### 8.5 FactもMap

固定DTOではなくMap/List構造。

**評価**: 中心の実行エンジンはかなり汎用化を意識した設計。

---

## 9. 現在確認できる「業務寄りハードコード」

汎用設計ではあるが、完全な業務非依存ではない。

### H-01 RuleType enum

RuleTypeに:

```text
ALLOWANCE
DEDUCTION
DAILY_REPORT
MONTHLY_DETAIL
PAYROLL
GENERAL
```

をJava enumで固定している。

新しい業務分類を追加するにはコード変更・deployが必要。

**分類**: 意図的な業務分類ハードコード。

重要度: 中。

---

### H-02 RuleReferenceChecker

Rule削除/無効化時の参照確認がJavaコードで:

- AllowanceMasterRepository
- DeductionMasterRepository

だけに固定されている。

つまり新たに日報/給与/別マスタがruleNameを参照してもReferenceCheckerへコード追加しない限り削除保護されない。

**分類**: 拡張性を阻害する明確な業務ハードコード。

重要度: 高。

---

### H-03 最大1000行

GeneralDataFetcher / RuleFactBuilderに最大1000件という固定上限がある。

Catalog.maxRowsも1〜1000へ制限。

**分類**: 安全性/性能のための技術ハードコード。

必ずしも悪いハードコードではないがconfig化されていない。

重要度: 中。

---

### H-04 singleRow検出用LIMIT 2

singleRow DataSourceはSQL LIMIT=2固定。

これは複数行検知用の意図的な技術定数。

**分類**: 妥当な実装定数。

---

### H-05 `params` Fact key

RuleFactBuilderが:

```text
facts["params"] = parameters
```

を固定している。

さらにParameterをtop-levelへも展開する。

**分類**: DSL contractハードコード。

これは正式なRule DSL仕様として文書化すべき値。

---

### H-06 default result key `result`

RuleのresultFactKey未設定時:

```text
result
```

を固定defaultとして使用。

**分類**: DSL contractハードコード。

---

### H-07 `tenantId` parameter名

tenantScoped CatalogはwhereClauseに文字列 `:tenantId` があることを固定要求する。

Tenant parameter名を変更できない。

**分類**: Security contractハードコード。

意味は妥当だが、Catalog DSL仕様として明文化が必要。

---

### H-08 identifier regex

Rule/DataSource/Fact等の名前は多くの箇所で:

```regex
^[a-zA-Z0-9_]+$
```

固定。

**分類**: Safety contract。

---

### H-09 direct whereClause禁止keyword

RuleMasterValidatorではdirect DataSource用whereClauseに対し固定keyword blacklistを持つ。

Catalog方式とは安全性モデルが異なる。

**分類**: Security実装ハードコード。

---

## 10. 現在確認できる「危険なハードコード」と「妥当な固定仕様」の区別

### 改善優先度高

- RuleReferenceCheckerが手当/控除だけ
- Catalog Column 0件時SELECT *
- params/sourceName namespace衝突
- tenant条件が`:tenantId`文字列存在確認だけ

### 仕様として固定してよい可能性が高い

- default result key=`result`
- Parameter namespace=`params`
- singleRow LIMIT 2
- identifier安全文字

ただし後者も設計書へ「予約語/DSL contract」として明記するべき。

### 要件次第でconfig化候補

- maxRows最大1000
- RuleType enum

---

## 11. 「適用場所を外で制御する」設計のメリット

現行依存方向:

```text
Allowance / Deduction / Payroll等
       ↓ ruleName
system/rule
```

この方向ならRule engineは:

- 従業員
- 手当
- 控除
- 日報

のEntityを直接知らずに済む。

RuleContextへ必要なID・日付・数値をParameterとして渡し、Catalog経由でFact生成できる。

**設計上はこの方向を維持する方が汎用性が高い。**

---

## 12. ただし現在のReferenceCheckerは逆依存を作っている

Rule engine内の `RuleReferenceChecker` が:

```text
AllowanceMasterRepository
DeductionMasterRepository
```

を直接importする。

これはsystem/ruleがmaster/allowance・master/deductionを知っている状態。

**実装事実**: Rule実行エンジン自体は汎用だが、Rule管理の削除保護部分に業務機能への逆依存がある。

### V2候補

例:

```text
RuleReferenceProvider interface
  ├─ AllowanceRuleReferenceProvider
  ├─ DeductionRuleReferenceProvider
  ├─ PayrollRuleReferenceProvider
  └─ ...
```

を各業務機能側で実装し、Rule側はProvider一覧だけを見る構造。

今回は修正しない。

---

## 13. 従業員別Ruleを作る際の想定形

汎用性を維持するならRule本体へ `employeeId` fieldを追加するのではなく、Parameterとして定義する。

例:

```text
RuleParameter
  employeeId : LONG required
  targetDate : DATE required
```

Catalog:

```sql
tenant_id = :tenantId
AND employee_id = :employeeId
AND effective_from <= :targetDate
```

Column Mapping:

```text
hourly_wage → hourlyWage DECIMAL
```

DSL:

```text
employee.hourlyWage * hours
```

この方式なら同じRuleを異なるemployeeIdへ再利用できる。

**これは現行engine構造と整合する。**

---

## 14. 今後の詳細解析で確認すること

### 適用場所

Repository全体で:

- RuleExecutionService呼出元
- `ruleName` field利用箇所
- RuleType検索箇所

を洗い出す。

### 実Rule

Runtime schema / SQL / fixtureから:

- rule_master
- rule_parameter
- rule_data_source
- rule_column_mapping
- rule_data_source_catalog

の実データを探す。

### ハードコード

実DSL ExecutorとJavaBean実装を読み:

- 特定Fact名
- 特定Parameter名
- 特定手当/控除コード
- 金額丸め
- 固定率
- 固定日数
- 固定時間

等がJavaコードへ埋め込まれていないか確認する。

---

## 15. 現時点の回答まとめ

### Q1 employeeId等をwhereClauseに指定できるか

**できる。**

Named ParameterとしてRule Parametersからbind可能。

### Q2 Rule適用場所はsystem/ruleで制御するか

**基本的にはしない。**

呼出元業務側がruleNameを選び、必要Parametersを渡す構造。

手当/控除マスタもruleNameを持つ。

### Q3 変なハードコードはあるか

**中心エンジンはかなり汎用的だが、いくつかある。**

特に現在重要なのは:

1. RuleReferenceCheckerが手当/控除Repositoryへ直接依存
2. RuleTypeが業務分類enum固定
3. maxRows=1000固定上限
4. params/result/tenantId等の固定DSL contract
5. Catalog Column 0件時SELECT *という例外的経路

このうち1と5は優先度高で改善候補として扱う。
