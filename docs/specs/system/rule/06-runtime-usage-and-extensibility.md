# system/rule 詳細設計 06 — 実利用経路・ドメイン依存除去の設計候補

## 1. 対象範囲

この文書は以下を整理する。

- RuleExecutionServiceの現在確認できる実利用経路
- Rule/Catalog実登録データのRepository内可視性
- 現行のJavaBean Rule登録方式
- ドメイン依存を減らす設計候補
- Base class / interface / annotation / registryの比較
- RuleType enumを動的Catalogへ移行する考え方

基準コード:

- branch: `main`
- commit: `12c91a72b409df16b9d4be0b416247a07a8f170a`

**この調査ではコード修正を行わない。**

---

## 2. 現在確認できるRule実行入口

Backend公開API:

```text
POST /api/system/rules/execution/fire
```

`RuleExecutionController` が:

```text
request.ruleName
request.context
```

を `RuleExecutionService#execute()` へ渡す。

Controllerは `SYS_ADMIN` 専用。

Frontend `RuleTestTab.vue` はこのAPIをRule管理画面のテスト機能として呼び出す。

初期テストJSONも:

```json
{
  "employeeId": 1,
  "targetDate": "2026-07-01",
  "hours": 8
}
```

となっている。

**実装事実**: 少なくとも管理画面のRule Test機能は employeeId / targetDate / hours のような汎用Parameterを渡してRuleExecutionServiceを実行する。

---

## 3. 業務側からの直接RuleExecutionService呼出

Repository code searchでは、今回の検索条件で `RuleExecutionService` / `ruleExecutionService` の業務機能側直接参照を特定できなかった。

現在確実に確認できる呼出は:

```text
RuleTestTab
→ /api/system/rules/execution/fire
→ RuleExecutionController
→ RuleExecutionService
```

である。

### 注意

GitHub code search indexの検索結果が空になるケースがあるため、これだけで「業務呼出が絶対に存在しない」とは断定しない。

後続の給与/日報/手当/控除詳細設計時に、それぞれの計算ServiceからruleName利用箇所を個別に追う。

---

## 4. 手当・控除のRule参照

`AllowanceMaster`:

```text
rule_name
```

`DeductionMaster`:

```text
rule_name
```

を持つ。

したがってデータモデル上は:

```text
AllowanceMaster ──ruleName──> RuleMaster
DeductionMaster ──ruleName──> RuleMaster
```

という参照がある。

ただしEntityにruleNameがあることと、現時点の給与計算ServiceがRuleExecutionServiceを呼んでいることは別問題。

後続で「ruleNameを実際にいつfireするか」を業務Service単位で確認する。

---

## 5. 実Rule/Catalog seedデータ

現行Flyway migrationは:

```text
V1__init.sql
```

1ファイルのみで、Rule/Catalogの正本seedとして利用できるMigration群は確認できない。

したがってRepository内resourcesから:

```text
rule_master
rule_parameter
rule_data_source
rule_column_mapping
rule_data_source_catalog
```

の本番相当初期データを網羅的に再現できる状態ではない。

**未決事項**: 現行DEV DBに実際に登録されているRule/Catalog値はDB実データ確認が必要。

この調査チャットではDB変更は行わない。

---

## 6. 現在のJavaBean Rule方式

現行コードには既にPlugin/Registry的な構造がある。

`RuleBeanCatalogService` constructor:

```java
Map<String, Rule> ruleBeans
```

Springが `org.jeasy.rules.api.Rule` 型BeanをMapとして注入する。

そのMapのkeyはSpring Bean名。

Catalog Serviceは:

- beanName一覧
- beanName存在確認

を提供する。

`JavaBeanDslExecutor` は:

```text
rule.ruleBeanName
→ ApplicationContext#getBean(beanName)
→ Easy Rules Ruleであること確認
→ RulesEngine.fire()
```

で実行する。

**実装事実**: JavaBean計算は既に「実装BeanをSpringへ追加すればCatalogへ現れる」方向の設計を部分的に持つ。

---

## 7. ユーザー提案: Base classから具体計算Classを実装

この方向は現行構造と相性が良い。

ただし、汎用性を最大化するなら**継承Base classを主契約にするよりInterfaceを主契約にする方を推奨**する。

理由:

- Javaは単一継承
- 各ドメインClassが別Base classを必要とする可能性
- 計算Contractと共通実装を分離できる
- test doubleを作りやすい

推奨概念:

```java
public interface RuleCalculator {
    RuleCalculationResult calculate(RuleCalculationContext context);
}
```

共通処理が必要なら:

```java
public abstract class AbstractRuleCalculator
        implements RuleCalculator {

    // 共通null check
    // money rounding helper
    // parameter helper
}
```

を任意利用する。

つまり:

```text
契約 = interface
共通実装 = optional abstract base class
```

が柔軟。

---

## 8. Annotation自己登録方式

ユーザー提案のAnnotation方式は有効。

例概念:

```java
@RuleComponent(
    code = "ALLOWANCE_HOUSING",
    category = "ALLOWANCE",
    displayName = "住宅手当計算"
)
@Component
public class HousingAllowanceCalculator
        implements RuleCalculator {
    ...
}
```

Startup時にSpring Beanをscanし:

```text
RuleCalculator beans
→ @RuleComponent metadata
→ RuleComponentRegistry
```

を構築する。

Registry:

```text
code -> calculator bean
code -> displayName
code -> category
code -> parameter schema
code -> result type
```

を提供できる。

---

## 9. Enumは実行時に動的追加できるか

**Java enumそのものへ実行時に項目を安全に追加することはできない。**

したがって:

```java
enum RuleType {
    ALLOWANCE,
    DEDUCTION,
    ...
}
```

をAnnotation scanで「enum値を追加」する設計にはできない。

### 代わりに行うこと

EnumをUI/業務分類の正本にせず、Registry Catalogを正本にする。

例:

```text
GET /api/system/rules/component-catalog
```

Response:

```json
[
  {
    "code": "ALLOWANCE",
    "displayName": "手当",
    "source": "annotation"
  },
  {
    "code": "SPECIAL_CALC",
    "displayName": "特殊計算",
    "source": "annotation"
  }
]
```

Frontend dropdownはAPI Responseから動的生成する。

**つまり「Enumを動的にする」のではなく「Enumを廃止/限定し、Catalogを動的にする」方が正しい。**

---

## 10. RuleTypeをどう扱うか

現在RuleTypeは:

- ALLOWANCE
- DEDUCTION
- DAILY_REPORT
- MONTHLY_DETAIL
- PAYROLL
- GENERAL

固定Enum。

### V2候補A: categoryをStringへ変更

```text
rule_type VARCHAR
```

Registryが利用可能categoryを返す。

長所:

- 新domain追加でRule module変更不要

短所:

- compile-time type safety低下

### V2候補B: RuleTypeは大分類として維持

RuleType enumは大分類だけに限定し、具体計算種類はAnnotation Registryにする。

例:

```text
RuleType = ALLOWANCE
calculatorCode = HOUSING_ALLOWANCE
```

長所:

- 大分類のtype safetyを維持
- 具体calculatorは動的

**現行からの移行ではBが安全。**

---

## 11. ドメイン依存除去の候補構成

現状:

```text
system/rule
  └─ RuleReferenceChecker
      ├─ AllowanceMasterRepository
      └─ DeductionMasterRepository
```

候補:

```text
system/rule
  ├─ RuleCalculator
  ├─ RuleComponentRegistry
  ├─ RuleReferenceProvider
  └─ RuleUsageRegistry

master/allowance
  ├─ HousingAllowanceCalculator
  └─ AllowanceRuleReferenceProvider

master/deduction
  ├─ IncomeTaxCalculator
  └─ DeductionRuleReferenceProvider
```

Rule moduleはdomain Repositoryをimportしない。

SpringがProvider/Calculator一覧を収集する。

---

## 12. RuleReferenceProvider候補

Interface:

```java
public interface RuleReferenceProvider {
    String domainCode();
    boolean isReferenced(String ruleName);
    List<RuleReference> findReferences(String ruleName);
}
```

各domain:

```java
@Component
public class AllowanceRuleReferenceProvider
        implements RuleReferenceProvider {
    ...
}
```

Rule側:

```text
List<RuleReferenceProvider>
```

だけを注入する。

新domainを追加してもsystem/rule変更不要。

---

## 13. 「適用場所」もAnnotationで登録する案

適用場所も固定EnumにせずProvider化できる。

例:

```java
@RuleUsage(
    code = "PAYROLL_ALLOWANCE",
    displayName = "給与計算・手当"
)
@Component
class PayrollAllowanceRuleUsageProvider {
    ...
}
```

ただし重要なのは、**Rule engine自身が適用場所を決めない設計は維持する**こと。

Registryは「利用可能な適用場所metadata」を提供しても、実際のfireは給与/日報等のdomain Serviceが行う。

---

## 14. 計算Base classに何を入れるべきか

入れてよい共通処理:

- Parameter取得helper
- required Parameter check
- BigDecimal変換
- Money scale/rounding helper
- Date conversion
- Fact取得helper
- Domain-neutral Validation

入れるべきでないもの:

- employee repository直接参照
- AllowanceMaster repository
- DeductionMaster repository
- 特定手当code
- 税率固定値
- 業務Table名

これらをBaseへ入れると再度domain依存が中心へ戻る。

---

## 15. DBアクセスはCalculatorに持たせるか

汎用性を保つなら基本は:

```text
Calculator
→ 与えられたfactsで計算
```

とし、Calculator自身がRepositoryから任意取得するのは避けた方がよい。

現行Rule engineの良い点は:

```text
DataSource Catalog
→ FactBuilder
→ Calculator/DSL
```

とデータ取得と計算を分離していること。

JavaBean Calculatorも可能な限りこの境界を守る方が再現性・テスト性が高い。

---

## 16. 現行JavaBean方式を活かした段階移行

全面作り直しは不要。

### Step 1

現在のEasy Rules `Rule` Bean Catalogを維持。

### Step 2

`@RuleComponent` annotation metadataを追加。

### Step 3

RuleBeanCatalogServiceを:

```text
beanName一覧
```

から:

```text
code / displayName / category / beanName / parameter schema
```

を返すRegistryへ拡張。

### Step 4

RuleReferenceCheckerをProvider方式へ移す。

### Step 5

必要ならRuleType Enumを大分類だけ残すかString Catalogへ移行。

---

## 17. 現時点の評価

ユーザー提案の方向性は、現行設計を壊すものではなく**むしろ現在のJavaBean Catalog構造を一般化する方向**。

特に:

```text
interface
+ annotation
+ Spring Bean scan
+ registry/catalog API
```

はsystem/ruleのdomain依存除去に適している。

ただし:

> AnnotationによってJava Enumそのものを動的拡張する

のではなく:

> AnnotationからRegistryを動的構築し、FrontendやValidationがRegistryを見る

という設計にする。

---

## 18. 現時点の未決事項

- RuleTypeを完全動的にする必要があるか
- 大分類Enumは残すか
- Calculator codeとRuleMaster.ruleNameの関係
- 1 Rule = 1 Calculatorか
- JEXL/MVELとJavaBean Calculatorを同じRegistryで扱うか
- Annotation metadataをDBへsnapshotするか
- Calculator versioningが必要か
- 過去給与再計算で旧Calculator versionを再現する必要があるか

給与計算では最後のversioningが特に重要。

---

## 19. 次に解析する範囲

次は計算仕様の核心として、**JEXL / MVEL / JAVA_BEAN Executorを1つずつ詳細解析する。**

順番:

```text
1. DslExecutorDispatcher
2. JexlDslExecutor
3. MvelDslExecutor
4. RuleDslSafety
5. JavaBeanDslExecutor
6. Easy Rules Fact入出力
```

特に以下を細かく確定する。

- 四則演算の数値型
- BigDecimal挙動
- null演算
- boolean条件
- ternary / if
- list/map access
- method call
- collection aggregation
- rounding
- scale
- division
- 例外
- result type
- security sandbox
- class/method access制限

その後、実Ruleが取得できたものから計算式を1Ruleずつ詳細化する。
