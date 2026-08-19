# system/rule 詳細設計 07 — DSL Executor・計算意味論・安全性

## 1. 対象範囲

今回は `system/rule` のうち、**JEXL / MVEL / JAVA_BEAN の実行方式と、計算結果へ影響する意味論・安全性**を対象とする。

対象経路:

```text
RuleExecutionService
→ DslExecutorDispatcher
→ JexlDslExecutor / MvelDslExecutor / JavaBeanDslExecutor
→ RuleDslSafety
→ JEXL / MVEL / Easy Rules
```

基準コード:

- branch: `main`
- commit: `12c91a72b409df16b9d4be0b416247a07a8f170a`

**この調査ではコード修正を行わない。**

---

## 2. Executor選択

`DslExecutorDispatcher` はSpringから `List<DslExecutor>` を受け取り、

```text
executor.supports() == rule.dslType
```

で最初の一致を選ぶ。

現在の実装:

- `JexlDslExecutor`
- `MvelDslExecutor`
- `JavaBeanDslExecutor`

未対応dslTypeならRuntimeException。

**実装事実**: Dispatcher自体はif/switchで各実装をハードコードせず、interface実装の追加で拡張できる。

---

## 3. JEXL実行設定

`JexlDslExecutor` はExecutor生成時に:

```java
new JexlBuilder()
    .strict(true)
    .safe(true)
    .silent(false)
    .permissions(JexlPermissions.RESTRICTED)
    .create();
```

を使用する。

### strict=true

未定義変数や不正参照を寛容にnull扱いするより、Errorとして扱う方向。

### safe=true

null navigation等についてJEXLのsafe動作を有効にする。

### silent=false

評価Errorを黙ってnullへ変換せずExceptionとして扱う。

### RESTRICTED permissions

JEXLからJava object/class/methodへ無制限にアクセスさせないためのpermissions設定。

**実装事実**: 3 Executorの中ではJEXLが最も明示的にsandbox寄り設定を持つ。

---

## 4. JEXL Context

RuleFactBuilderで構築したfactsを1件ずつ:

```text
JexlContext#set(key, value)
```

する。

したがってJEXL式から参照できる入力は原則facts Mapのtop-level key。

例:

```text
hours
params
employee
attendance
```

Map/Listのproperty/index参照能力はJEXL標準評価規則に依存する。

---

## 5. JEXLはExpressionとして評価

```text
jexl.createExpression(rule.dslText)
→ expression.evaluate(context)
```

を使用する。

**実装事実**: Script APIではなくExpression API。

したがって現行設計のJEXLは、複数statementの汎用scriptより「1つの式を評価して値を返す」用途を想定した構造。

---

## 6. MVEL実行

`MvelDslExecutor` は:

```text
variables = new HashMap<>(facts)
MVEL.eval(rule.dslText, variables)
```

を呼ぶ。

JEXLのような専用permissions objectやsandbox configurationは設定されていない。

実行前に `RuleDslSafety.validate()` は行う。

**実装事実**: MVELの安全境界は主に共通Regexチェックに依存している。

---

## 7. RuleDslSafety

共通禁止pattern:

```text
import
new
class
getClass(
forName(
.class
Runtime
System
ProcessBuilder
ClassLoader
```

case-insensitiveで検知する。

検知すると:

```text
IllegalArgumentException("DSLに使用できない構文が含まれています。")
```

### 性質

これはAST allowlistではなく文字列Regex blacklist。

**実装事実**: 「安全な構文だけ許可」ではなく「既知の危険文字列を拒否」する方式。

JEXLではこれに加えてRESTRICTED permissionsがある。

MVELには追加sandbox設定がない。

---

## 8. JEXLとMVELの安全性差

### JEXL

```text
RuleDslSafety regex
+
JexlPermissions.RESTRICTED
+
strict/safe/silent設定
```

### MVEL

```text
RuleDslSafety regex
+
MVEL.eval
```

**既知事項 — 優先度高**: DSL Typeを切り替えると計算構文だけでなくセキュリティモデルも変わる。

「JEXLとMVELは同等の安全性」という仕様にはできない。

今回は修正しない。

---

## 9. 数値型の基本入力

Rule engineのParameter / Column変換では:

- INTEGER → Integer
- LONG → Long
- DECIMAL → BigDecimal
- BOOLEAN → Boolean
- DATE → LocalDate
- DATETIME → LocalDateTime

をfactsへ入れる。

したがって金額計算を正確にしたい場合、入力定義をDECIMALにすればDSLへBigDecimalを渡すことはできる。

ただし**DSLの演算結果型・promotion・division・roundingは各engineの演算規則に依存**する。

---

## 10. BigDecimalと四則演算

現行Rule layerには:

- Money専用型
- MathContext
- scale
- RoundingMode
- 共通round helper

をDSL評価前後に強制する処理は確認できない。

**確定仕様（現行コード）**: 金額計算の丸め・scaleをRule engine共通層では保証していない。

したがってDSLで:

```text
amount * rate
amount / divisor
```

と書いた場合の結果はJEXL/MVELの数値演算仕様とoperand型に依存する。

給与・控除・税等ではこの点をRuleごとに確認する必要がある。

---

## 11. 除算

現行コードに:

```text
setScale(...)
RoundingMode
MathContext
```

の自動適用はない。

BigDecimal除算で割り切れない値を扱う場合、DSL engineがどう処理するかをテストで契約化していない。

**未決事項 — 計算精度上重要**:

- JEXL BigDecimal / BigDecimal division
- MVEL BigDecimal / BigDecimal division
- Integer division
- mixed Number division

の正式仕様。

給与計算用途では実DSL任せにせず、Money helper/Calculator側の明示roundingを推奨候補とする。

---

## 12. 丸め

Rule engine共通層では丸めない。

したがって四捨五入・切捨て・切上げを必要とする計算は:

- DSL内で明示
- JavaBean Calculator内で明示
- 呼出元で明示

のいずれかが必要。

**既知事項**: 現在「金額は最終的に円単位でどのRoundingMode」といったRule全体共通contractは存在しない。

---

## 13. null

Fact生成段階で:

- optional Parameter missing → key自体を除去
- optional DataSource Column null → `factKey -> null`
- single DataSource 0件 → 空Mapまたはnull値Map
- list 0件 → empty List

となる。

この違いを各DSLがどう評価するかで計算結果が変わる。

### JEXL

strict=trueのため未定義top-level variableはErrorになる可能性が高い一方、safe=trueでsafe navigationの意味も持つ。

### MVEL

MVEL独自のnull/未定義variable規則に依存する。

**実装事実**: Engine共通のnull normalization layerはない。

---

## 14. 条件分岐

Rule engine側でif/ternaryをラップする共通APIはない。

JEXL/MVELそれぞれのexpression languageが提供する条件演算を使用する。

したがって:

```text
条件
→ true値
→ false値
```

の式そのものはRule DSL textに保存される。

**設計上の意味**: 業務条件の順序・境界値はJava ServiceではなくRule DB定義側に移せる。

---

## 15. List/Map計算

FactBuilderはlist DataSourceを:

```text
List<Map<String,Object>>
```

として渡す。

single DataSourceは:

```text
Map<String,Object>
```

として渡す。

DSLからList要素/index/propertyへアクセスできる能力は各engine標準に依存する。

ただしRule layer自身に:

- sum
- average
- max
- min
- filter

等のdomain-neutral aggregate helperは確認できない。

**実装事実**: 集計構文・method call能力はJEXL/MVEL仕様に依存する。

---

## 16. Method call

RuleDslSafetyはmethod call全般を禁止していない。

禁止しているのは特に:

- getClass
- forName
- 危険class名等

JEXLはさらにRESTRICTED permissions。

MVELは一般method callが可能かどうかをMVEL標準評価へ委ねる。

**既知事項 — セキュリティ/再現性**: 「DSLでどのmethodまで呼べるか」をRule engine独自allowlistとして定義していない。

JEXLはlibrary permissionsが防御するが、MVELは共通allowlist無し。

---

## 17. JavaBean Executor

`JavaBeanDslExecutor`:

```text
ruleBeanName
→ ApplicationContext#getBean
→ instanceof org.jeasy.rules.api.Rule
→ Factsへcontext.factsをcopy
→ RulesEngine.fire
→ Easy Rules Factsをcontext.factsへcopy back
→ facts[resultFactKey]を返す
```

### 特徴

JEXL/MVELと違い、計算ロジックはJavaコード。

したがって:

- BigDecimal
- scale
- RoundingMode
- 複雑な条件
- helper method

をcompile-time codeとして明示できる。

**計算精度が重要な給与/税/保険計算には、JavaBean方式が最も契約化しやすい。**

---

## 18. JavaBeanの副作用

JavaBean Executorが要求する型はEasy Rules `Rule` だけ。

そのBeanが:

- Repository
- Service
- 外部API

をDIしてはいけないという制約はコード上ない。

上位RuleExecutionServiceはreadOnly Transactionだが、呼び出したBeanが別Transactionを開始する可能性までは禁止していない。

**既知事項**: JavaBean RuleはDSLより強力で、その分副作用を持てる。

汎用化V2では `RuleCalculator` をpure calculation contractに寄せる設計が望ましい候補。

---

## 19. JavaBeanのBean選択安全性

RuleMaster保存時にはRuleBeanCatalogServiceが登録済みEasy Rules Rule Beanか確認する。

実行時も:

```text
instanceof Rule
```

を再確認する。

任意Spring Bean名を無条件実行するわけではない。

**良い点**: JavaBean実行対象はEasy Rules Rule型へ限定されている。

---

## 20. Easy Rules Engine

`RuleEngineConfig` は:

```java
new DefaultRulesEngine()
```

を1 Beanとして提供する。

JavaBeanDslExecutorは1回の実行で1 Ruleだけregisterした `Rules` をfireする。

**実装事実**: RuleMasterのpriorityを使って複数Easy Rulesを一括競合実行する構造ではない。

RuleMaster.priorityは少なくともこのJavaBean実行経路で複数Rule orderingには利用されない。

---

## 21. resultFactKey

JavaBean方式ではEasy Rules実行後:

```text
facts[resultFactKey]
```

を戻り値にする。

JEXL/MVEL方式ではexpressionのevaluate結果が直接戻り値。

その後上位RuleExecutionServiceがresultFactKeyへ結果を書き戻す。

### 差

- JEXL/MVEL: expression戻り値が正
- JAVA_BEAN: BeanがFactsへresultFactKeyをputすることが前提

**実装事実**: Executorごとにresult contractが少し異なる。

---

## 22. result型

RuleExecutionResult.resultはObject。

RuleMasterにresult data type定義はない。

したがって:

- Integer
- Long
- BigDecimal
- Boolean
- String
- Map
- List
- null

等を技術的に返しうる。

**未決事項**: 呼出元が期待するresult typeをRule定義で宣言・検証する仕組みがない。

給与金額RuleならDECIMAL等のresult schemaを持たせるV2候補がある。

---

## 23. Engine間互換性

同じ見た目のDSL式でもJEXL/MVELで:

- numeric promotion
- null
- property access
- method access
- collection操作
- exception

が完全一致する保証はない。

**確定仕様にすべきではないこと**:

> dslTypeをJEXLからMVELへ変更しても同じ計算結果になる

現行コードには互換性保証層がない。

---

## 24. dslType変更

RuleMaster更新時dslTypeを変更できる。

ruleNameだけがimmutable。

したがって同じruleNameのRuleを:

```text
JEXL → MVEL
MVEL → JAVA_BEAN
```

へ変更できる。

**計算再現性上重要**: ruleNameが同じでもdslType変更で意味論が変化しうる。

Version履歴がない場合、過去計算再現が難しくなる。

---

## 25. Timeout / 無限処理

JEXLはExpression APIで比較的限定的。

MVELは`MVEL.eval()`。

JavaBeanは任意Java Rule code。

RuleExecutionServiceレベルに:

- timeout
- circuit breaker
- execution instruction limit
- thread interruption policy

は確認できない。

**既知事項**: 長時間実行/高負荷Ruleに対する明示的execution timeoutがない。

---

## 26. DSL size

RuleMaster `dslText` はLOB/TEXT系Entity定義で保存される。

RuleDslSafetyは内容の危険keywordのみ確認し、式長上限は今回確認範囲で見当たらない。

**未決事項**: 巨大DSL textの上限。

---

## 27. Test保証

GitHub code searchでは今回:

- `JexlDslExecutorTest`
- `RuleDslSafetyTest`

の専用testを確認できなかった。

MVEL/JavaBeanも含め、後続のtest tree総括で再確認する。

### 特に必要な契約test候補

- BigDecimal addition/multiplication/division
- Integer/Long mixed
- Decimal + Integer
- rounding
- null
- undefined variable
- ternary
- List/Map
- forbidden class/method
- JEXL permissions
- MVEL escape attempt
- JEXL/MVEL同式差異
- JavaBean resultFactKey
- JavaBean副作用禁止方針
- timeout

---

## 28. 給与計算に関する現時点の評価

### 単純な条件式・設定変更頻度が高い計算

JEXLは比較的向いている。

理由:

- DB定義で変更可能
- RESTRICTED permissions
- expression中心

### 金額精度・法令計算・丸めが重要

現行仕様のまま自由DSLへ全て任せるよりJavaBean Calculator方式の方が安全性・testabilityが高い。

例:

- 所得税
- 社会保険
- 雇用保険
- 複雑な端数処理
- 年度別制度ロジック

### 単純手当

例:

```text
固定額
時間 × 単価
条件 ? 金額 : 0
```

はJEXL等でも適用しやすい。

---

## 29. V2汎用化設計との接続

最終的に別文書で詳細化するV2案では:

```text
RuleCalculator interface
+ @RuleComponent
+ Registry
```

を導入し、Money計算共通contractを持たせる候補がある。

例:

```text
MoneyMath
├─ multiply
├─ divide
├─ roundYen
└─ percentage
```

ただし**丸め方そのものを全業務で1つに固定するのではなく、Calculator/Rule metadataで明示する**方がよい。

税・保険・給与項目ごとに法定端数規則が異なりうるため。

---

## 30. 今回の重要既知事項

### A. JEXLとMVELでsecurity modelが異なる

重要度: 高。

### B. Money rounding contractがない

重要度: 高・給与計算。

### C. result type schemaがない

重要度: 高・呼出契約。

### D. JavaBeanは副作用可能

重要度: 高・設計方針。

### E. Engine変更で計算互換性保証なし

重要度: 高・再現性。

### F. execution timeoutなし

重要度: 中〜高。

### G. RuleDslSafetyはRegex blacklist

重要度: 高、特にMVEL。

---

## 31. 次に掘る範囲

次は**Rule管理画面の実行テストとRuleExecutionServiceの最終結果組立・例外処理**を詳細化する。

その後、Repository内の給与・手当・控除計算実装を個別に追い、

```text
業務項目
→ ruleName
→ parameters
→ Catalog
→ Facts
→ DSL/Calculator
→ result
→ 金額反映先
```

まで接続する。

最後に独立文書:

```text
V2-generic-rule-architecture.md
```

として、汎用化の実装案をまとめる。
