# system/rule 詳細設計 08 — 実行結果・Rule Test・例外処理

## 1. 対象範囲

今回は `system/rule` のうち、以下だけを対象とする。

- `RuleExecutionService` の最終結果組立
- `RuleTestTab.vue` の実行方法
- 保存前/保存後Ruleの扱い
- 実行成功Response
- Rule固有例外Handler
- 実行失敗時の現在の扱い

基準コード:

- branch: `main`
- commit: `12c91a72b409df16b9d4be0b416247a07a8f170a`

**この調査ではコード修正を行わない。**

---

## 2. 実行API

```http
POST /api/system/rules/execution/fire
```

Controller:

```text
RuleExecutionController#fire
→ RuleExecutionService#execute(ruleName, context)
```

Controller全体は:

```java
@PreAuthorize("hasRole('SYS_ADMIN')")
```

**確定仕様**: 現行の汎用Rule実行APIはSYS_ADMIN専用。

---

## 3. RuleExecutionServiceの全体順序

`RuleExecutionService#execute()`:

```text
1. RuleLoader.loadActive(ruleName)
2. RuleParameterResolver.resolve()
3. RuleFactBuilder.build()
4. RuleExecutionContext生成
5. DslExecutorDispatcher.execute()
6. resultFactKey決定
7. facts[resultFactKey] = result
8. RuleExecutionResult生成
```

`@Transactional(readOnly = true)`。

---

## 4. Active Ruleしか実行しない

`RuleLoader#loadActive()` は:

```text
findByRuleNameAndActiveFlagTrueAndDeletedAtIsNull(ruleName)
```

を使う。

したがって:

- inactive Rule
- soft deleted Rule
- 存在しないRule

は実行不可。

見つからない場合:

```text
EntityNotFoundException
```

を投げる。

---

## 5. resultFactKey

Executorが返した `result` を:

```text
rule.resultFactKey
```

へ入れる。

rule.resultFactKeyがblankなら:

```text
result
```

をdefault keyとして使用する。

例:

```text
resultFactKey = allowanceAmount
result = 12000

facts["allowanceAmount"] = 12000
```

---

## 6. Resultはfactsへ後書きされる

Executor実行時点のfactsと、Responseへ返すfactsは完全に同一ではない。

順序:

```text
facts生成
→ Executorへ渡す
→ Executor result取得
→ facts[resultFactKey] = result
→ Response
```

したがってJEXL/MVEL式自身がresultFactKeyを参照している場合、そのkeyが事前に存在する保証はない。

上位Serviceがresultを書き込むのはExecutor完了後。

---

## 7. JavaBeanとの違い

JavaBean ExecutorはEasy Rules Factsから:

```text
resultFactKey
```

を読み取って戻り値にする。

その後RuleExecutionServiceが同じresultFactKeyへresultを再度putする。

JEXL/MVEL:

```text
expression return value
→ ServiceがresultFactKeyへput
```

JAVA_BEAN:

```text
Bean自身がFacts[resultFactKey]へput
→ Executorがその値をreturn
→ Serviceが同じkeyへput
```

**実装事実**: result contractはExecutorにより少し異なる。

---

## 8. RuleExecutionResult

Response record:

```text
ruleName : String
executed : boolean
result   : Object
facts    : Map<String,Object>
message  : String
```

成功時:

```text
executed = true
message = "Ruleを実行しました。"
```

factsは:

```java
new LinkedHashMap<>(facts)
```

としてResponseへcopyする。

---

## 9. executed=false経路

現行 `RuleExecutionService#execute()` では成功時しか `RuleExecutionResult` を生成しない。

途中で失敗した場合はExceptionをthrowする。

**実装事実**: 現在 `executed=false` のRuleExecutionResultを返す経路は確認できない。

したがって `executed` fieldは現状ほぼ常にtrueの成功Response用field。

**V2候補**:

- fieldを削除
- またはBusiness failureも200 Responseで返す設計ならfalseを正式利用

今回は修正しない。

---

## 10. Rule Test画面

`RuleTestTab.vue` は左右2ペイン。

左:

```text
Input Parameters (JSON)
```

右:

```text
Execution Result
```

Toolbar:

- 実行
- クリア

---

## 11. Test初期入力

Frontend初期値:

```json
{
  "employeeId": 1,
  "targetDate": "2026-07-01",
  "hours": 8
}
```

これはRule固有Parameterを自動生成したものではなく固定サンプル。

**実装事実**: RuleParameter定義からTest JSONフォームを自動構築していない。

---

## 12. Test入力Validation

FrontendではJSON parseのみ。

受理条件:

- JSON object

拒否:

- null
- Array
- primitive
- JSON syntax error

Parameter required/dataType/default等はBackend RuleParameterResolverで最終判定する。

---

## 13. 保存前Ruleをテストできるか

**結論: できない。**

Test requestへ送るものは:

```text
ruleName
context.parameters
```

のみ。

画面で編集中の:

- dslText
- dslType
- parameters
- dataSources
- columns
- resultFactKey

をRequestには含めない。

BackendはruleNameから:

```text
RuleLoader.loadActive(ruleName)
```

でDB上の保存済みActive Ruleを再読込する。

**確定仕様（現行実装）**: Rule Testは保存済みDB定義を実行する。

---

## 14. 編集中内容とのズレ

例:

```text
DB保存済みDSL = hours * 1000
画面編集中DSL   = hours * 1200
```

保存せずTest実行すると:

```text
hours * 1000
```

が実行される。

画面上の編集中DSLはテストされない。

**既知事項 — UX/誤認リスク**: 利用者が「現在画面に見えている内容をテストした」と誤認する可能性がある。

画面文言は「保存済みRuleでの実行を推奨」だが、実装上は推奨ではなく保存済みRuleが実行対象。

今回は修正しない。

---

## 15. 新規未保存Rule

ruleNameを入力していてもDBに存在しなければRuleLoaderで失敗する。

したがって新規Ruleを保存前に試算する機能はない。

---

## 16. inactive RuleのTest

RuleLoaderがactive=trueを要求するため、inactive RuleはTest画面からも実行できない。

**実装事実**: 「無効化したRuleを管理者がテストだけする」機能はない。

---

## 17. Test Responseでfactsが見える

成功Responseはresultだけでなくfacts Map全体を返す。

RuleTestTabはResponseを:

```text
JSON.stringify(response, null, 2)
```

でそのまま表示する。

したがって管理者は:

- resolved Parameter
- DataSource Fact
- resultFact

を確認できる。

**良い点**: 計算式のデバッグ時に「入力DB値が何だったか」まで追いやすい。

---

## 18. Factに機微情報が含まれる可能性

Column Mapping 0件時のSELECT *等により、factsへ本来計算に不要なDB Columnが入る可能性がある。

Rule Test Responseはfacts全体をFrontendへ返す。

そのためSYS_ADMIN限定ではあるが、Fact設計とTest Responseの情報露出は連動する。

**既知事項**: Test APIのfacts返却は便利だが、Fact allowlist不備がそのまま情報露出範囲になる。

---

## 19. Rule固有Exception Handler

`RuleExceptionHandler` は:

```text
RuleMasterController
RuleExecutionController
```

だけを対象にする `@RestControllerAdvice`。

`@Order(HIGHEST_PRECEDENCE)`。

---

## 20. IllegalArgumentException

Rule専用Handler:

```text
HTTP 400
code = RULE_INVALID_REQUEST
message = exception.message
traceId = MDC traceId
```

対象例:

- RuleDslSafety違反
- RuleValueConverter変換失敗
- Validatorの一部

---

## 21. RuleConflictException

Rule専用Handler:

```text
HTTP 409
code = RULE_CONFLICT
```

主に:

- 参照中Rule削除
- 参照中Rule無効化

で利用される。

---

## 22. Rule専用Handlerで捕捉しない例外

確認したRuleExceptionHandlerでは専用捕捉がないもの:

- EntityNotFoundException
- RuntimeException
- IllegalStateException
- JEXL evaluation exception
- MVEL evaluation exception
- JDBC/DataAccess exception

これらは共通例外Handler等の後続Adviceへ委譲されると考えられるが、今回該当共通Handlerの正確なパスを特定できなかったためHTTP status/ErrorCodeはここでは断定しない。

**未決事項**: Rule実行失敗を400/404/422/500等へどう分類するか、共通Exception設計横断時に確定する。

---

## 23. EntityNotFoundExceptionの意味が混在

RuleLoaderのEntityNotFoundExceptionは:

- ruleName不存在
- inactive
- soft delete済み

を同じ「Ruleが見つかりません」として扱う。

またCatalog findRequired等でもEntityNotFoundExceptionを利用する。

**実装事実**: Rule定義不存在と実行不能状態の区別がAPI error contract上弱い。

---

## 24. 実行失敗履歴

backupにはBackupHistoryがあったが、system/ruleで以下のようなRule実行履歴Entity/Serviceは今回確認範囲で見当たらない。

- ruleName
- parameters
- result
- executedAt
- success/failure
- error
- rule version

**実装事実**: RuleExecutionService自身は実行監査履歴を保存しない。

給与等の業務側が計算結果を別Entityへ保存する可能性は後続で確認する。

---

## 25. 計算再現性への影響

Rule実行結果Responseには:

- ruleName
- result
- facts

はあるが:

- RuleMaster ID
- Rule更新日時/version
- dslType
- DSL hash
- Catalog version
- Calculator version

は含まれない。

したがってResponse単体から後日「どのRule版で計算したか」を厳密再現できない。

**既知事項 — 給与計算では重要**: Rule versioning/audit設計が必要になる可能性が高い。

---

## 26. Transactionと失敗

RuleExecutionServiceはreadOnly Transaction。

途中Exceptionはcatchせずそのまま上へthrowする。

成功Responseを部分返却することはない。

**確定仕様**: 実行はall-or-exception型。

---

## 27. Test APIは保存/実行を分離している

Rule Testは現在:

```text
Save Rule
↓
Fire Rule
```

が必要。

Draft Definitionを直接fireするAPIではない。

### V2候補

管理者用preview endpoint:

```text
POST /api/system/rules/execution/preview
```

Requestへ:

- unsaved Rule definition
- parameters

を送り、DBへ保存せずValidator→Fact→Executorを通す。

これならRule編集画面の現在内容を正確にテストできる。

今回は修正しない。

---

## 28. Test入力UIの拡張候補

現在は自由JSON editor。

RuleParameter metadataはすでに存在するため、将来:

```text
employeeId [number]
targetDate [date]
hours      [decimal]
```

のような型付きTest Formを自動生成可能。

さらに:

- required表示
- default表示
- DATE picker
- BOOLEAN checkbox
- DECIMAL number field

等へ拡張できる。

自由JSON modeもAdvancedとして残せる。

---

## 29. 金額計算拡張案への記録

最終V2汎用化文書では、ユーザー要望に基づき以下を独立セクション化する。

### Money計算基盤候補

- BigDecimal統一
- scale policy
- MathContext
- RoundingMode
- 円未満四捨五入
- 円未満切捨て
- 円未満切上げ
- 小数点n桁丸め
- percentage helper
- divide helper
- 0除算policy
- rate精度
- intermediate rounding / final roundingの区別

### 設計候補

```text
MoneyMath
MoneyRoundingPolicy
CalculationPrecisionPolicy
```

ただし給与・税・保険ごとに法定端数規則が異なるため、1つのglobal RoundingModeを強制するのではなく、Rule/Calculator metadataで明示できる方式を検討する。

---

## 30. 今回の重要既知事項

### A. Testは保存前Definitionを実行しない

重要度: 高・管理UX。

### B. inactive RuleをTestできない

重要度: 中。

### C. executed=false Response経路なし

重要度: 低〜中・API整理。

### D. Rule実行履歴/version記録なし

重要度: 高・給与再現性。

### E. facts全量をTest Responseへ返す

重要度: 高・Fact allowlistと連動。

### F. Rule固有Error分類は一部だけ

重要度: 中〜高。

### G. result type/version情報がResponse contractにない

重要度: 高・業務連携。

---

## 31. 次に掘る範囲

次から業務側へ接続する。

まず**手当**を1機能として追う。

```text
AllowanceMaster.ruleName
→ Allowanceの実際のvalue provider / payroll provider
→ Rule呼出有無
→ employeeId等Parameters
→ Rule結果の金額反映先
```

その後:

```text
控除
→ 給与
→ 日報/月次明細
```

の順でRule適用場所を確定する。

実計算がJava側にハードコードされている場合は、式・丸め・率・条件を1つずつ詳細に記録する。
