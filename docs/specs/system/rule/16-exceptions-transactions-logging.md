# system/rule 詳細設計 16 — 例外・Transaction・ログ・監査

## 1. 方針

今回もコード修正は行わない。

基準コード: main `12c91a72b409df16b9d4be0b416247a07a8f170a`

対象:

```text
RuleExecutionController
→ RuleExecutionService
→ RuleLoader
→ RuleParameterResolver
→ RuleFactBuilder / GeneralDataFetcher
→ RuleValueConverter
→ DslExecutorDispatcher / Executors
→ RuleExceptionHandler
→ GlobalExceptionHandler
→ TraceIdFilter
```

---

## 2. API入口

`RuleExecutionController#fire()`:

```http
POST /api/system/rules/execution/fire
```

Controllerはrequestをそのまま:

```text
service.execute(request.ruleName(), request.context())
```

へ渡す。

Controller内にtry/catchはない。

`SYS_ADMIN`権限がclass単位で必要。

---

## 3. RuleExecutionServiceの例外方針

`RuleExecutionService#execute()` 自体も例外をcatchしない。

順序:

```text
RuleLoader
→ ParameterResolver
→ FactBuilder
→ Dispatcher
→ result組立
```

どこかで失敗した場合、そのExceptionは上位へ伝播する。

**確定仕様**: 部分成功Responseは返さず、all-or-exception型。

---

## 4. Transaction

`RuleExecutionService#execute()`:

```java
@Transactional(readOnly = true)
```

したがってRule定義読込・DataSource SELECT等はreadOnly Transactionの呼出範囲内。

### 重要

RuleExecutionService自身はDB更新を行わない。

ただしJavaBean Executorで呼ぶBeanが別Service/Repositoryを利用できる構造上、副作用を完全には禁止していない。

**修正候補**: V2 RuleCalculator contractをpure calculationへ寄せ、副作用禁止を設計契約化する。

---

## 5. Rule専用Exception Handler

`RuleExceptionHandler` は:

```text
RuleMasterController
RuleExecutionController
```

だけを対象にする。

Priority:

```text
Ordered.HIGHEST_PRECEDENCE
```

そのためRule Controllerでは共通GlobalExceptionHandlerより先に評価される。

---

## 6. IllegalArgumentException

Rule専用Handler:

```text
HTTP 400
code = RULE_INVALID_REQUEST
message = exception.getMessage()
traceId = MDC traceId
```

対象になりうる例:

- required parameter不足
- 型変換失敗
- unsafe DSL
- unsafe whereClause
- identifier validation
- negative給与項目result等（Rule Controller経路で発生した場合）

### 情報露出

IllegalArgumentExceptionは**例外messageをそのままAPIへ返す**。

したがってValidation messageへ:

- SQL断片
- DSL全文
- DB値
- 個人情報

を含めない設計が必要。

RuleValueConverterはvalueをmessageへ含める実装があるため、入力値が機微情報の場合はAPIへ露出する可能性がある。

**既知事項 — 優先度高**: Rule 400系messageの情報分類/サニタイズ方針が必要。

---

## 7. RuleConflictException

Rule専用Handler:

```text
HTTP 409
code = RULE_CONFLICT
message = exception.getMessage()
traceId
```

主に:

- 参照中Rule削除
- 参照中Rule無効化
- ruleName変更禁止

等。

こちらもmessageはクライアントへそのまま返る。

ただし現在確認したmessageは業務的説明が中心。

---

## 8. EntityNotFoundException

Rule専用Handlerでは捕捉しない。

そのためGlobalExceptionHandlerへ進む。

Global:

```text
ErrorCode = COMMON_RESOURCE_NOT_FOUND_ERROR
HTTP status = ErrorCode設定値
API message = 共通ErrorCodeの汎用message
traceId
```

内部 `ex.getMessage()` はAPI bodyへ出さずログだけに記録する。

### 対象例

- Rule不存在
- inactive Rule
- soft delete Rule
- Catalog不存在/無効

**良い点**: DB/内部定義名を含むNotFound詳細をAPIへ直接露出しない。

---

## 9. その他Exception

以下はRule専用Handlerで明示捕捉されない。

例:

- IllegalStateException
- JEXL RuntimeException
- MVEL RuntimeException
- DataAccessException
- JDBC Exception
- NullPointerException
- JavaBean Rule exception

GlobalExceptionHandlerの:

```text
@ExceptionHandler(Exception.class)
```

で処理される。

Response:

```text
COMMON_INTERNAL_ERROR
汎用message
traceId
```

**確定仕様**: 未分類内部例外の具体message/stack traceはAPIへ返さない。

---

## 10. GlobalExceptionHandlerのログ

Unhandled Exceptionではerror logへ:

```text
errorCode
path
traceId
exception class
exception message
stack trace
```

を記録する。

EntityNotFound/Validation/BusinessExceptionはwarn log。

### 良い点

クライアントには汎用message、運用者はtraceIdで詳細ログへ追跡できる。

### 注意

`exception message` とstack traceはログへ残る。

JDBC/JEXL/MVEL/JavaBean由来messageに:

- SQL
- table名
- parameter値
- DSL内容
- 個人情報

が含まれる可能性がある。

**修正候補**: logging sanitizer / secret masking / parameter redaction。

---

## 11. TraceId

`TraceIdFilter` は全HTTP requestごとにUUIDを生成する。

```text
MDC.traceId
MDC.httpMethod
MDC.httpPath
```

を設定し、Response Header:

```text
X-Trace-Id
```

へ返す。

終了時:

```text
httpStatus
durationMs
```

をMDCへ追加し、request completion logを出す。

最後に `MDC.clear()`。

### 確定仕様

Rule ErrorResponseのtraceIdとHTTP completion logを関連付けられる。

---

## 12. Request/Responseの監査

TraceIdFilterが記録するのは:

- method
- path
- status
- duration

であり、Rule固有の:

- ruleName
- employeeId
- targetDate
- result
- ruleVersion

を自動監査するものではない。

RuleExecutionServiceにもexecution history保存はない。

**実装事実**: HTTP observabilityはあるが、Rule業務監査はない。

---

## 13. Rule Test成功時の情報露出

成功Responseは:

```text
result
facts全体
```

を返す。

これはExceptionではないためGlobal Handlerのサニタイズ対象外。

したがってDataSource Factに不要なColumnが入れば、その値は管理画面Rule Testで表示される。

以前確認した:

```text
Column Mapping 0件 → SELECT * → Fact全Column
```

との組み合わせは情報露出範囲を広げる。

**修正候補: 高**

- SELECT *禁止
- Fact allowlist必須
- Test response factsをdebug-safe DTOへ限定

---

## 14. Parameter変換Errorのmessage

RuleValueConverterは変換失敗を:

```text
{valueName}を{dataType}へ変換できません。 value={value}
```

のIllegalArgumentExceptionへwrapする。

Rule Controller経路ではRuleExceptionHandlerがmessageをそのまま400へ返す。

### 影響

例えばParameterが:

```text
個人番号
口座番号
秘密値
```

等なら入力値そのものがResponseへ出る可能性。

現在のRule用途ではemployeeId/amount/date等が中心だが、汎用化するとリスクが増える。

**V2候補**:

```text
RuleParameterConversionException
- parameterName
- expectedType
- safeErrorCode
```

として値をAPI messageへ含めない。

---

## 15. DataFetcher Error

DataFetcher層では:

- tenant contextなし
- tenant placeholderなし
- table identifier不正
- maxRows不正
- SQL実行失敗

等が起こりうる。

RuleExecutionServiceではcatchしないため、そのままController Adviceへ伝播。

### 分類差

IllegalArgumentExceptionなら400。
IllegalStateException/DataAccessException等なら通常500系共通Errorになる。

**未決事項**: Catalog設定ミスを4xx管理設定Errorとするか5xx system errorとするか、Error taxonomyを整理する余地。

---

## 16. DSL Executor Error

### JEXL/MVEL

Executor評価ExceptionはRuleExecutionServiceで変換しない。

通常Global internal errorへ落ちる可能性が高い。

### 管理画面TestのUX

Rule作者がDSL syntax errorを起こした場合でも、種類によっては:

```text
COMMON_INTERNAL_ERROR
```

しか画面へ出ない可能性がある。

安全性としては良いが、Rule Test UXとしては原因が分かりにくい。

### V2候補

管理者Preview/Test専用では安全にsanitizedした:

```text
RULE_DSL_SYNTAX_ERROR
line/column
safe message
```

を返す。

本番業務呼出では詳細を隠す。

---

## 17. Caller側Error wrapping

Allowance/Deduction AUTOの共通給与項目側ではRule実行失敗を:

```text
給与項目のRule計算に失敗しました。
code=...
ruleName=...
```

というIllegalStateExceptionへwrapする実装を確認済み。

これがHTTPへ到達すればGlobal handlerで汎用internal errorとなり、詳細messageはログだけに残る。

### 良い点

業務Context（給与項目code/ruleName）が運用ログへ残る。

### 注意

将来employeeId等までwrap messageへ足す場合は個人情報ログ方針が必要。

---

## 18. Rollback

RuleExecutionServiceはreadOnlyで自身のwriteなし。

Rule Master create/update/deleteは各CommandServiceで`@Transactional`。

RuntimeException系で通常rollback対象。

参照中Rule deleteでは例外を出す前にdelete副作用を行わないtestも存在する。

### JavaBean

JavaBean Ruleが別Transaction (`REQUIRES_NEW`等) を開始した場合、上位readOnly rollbackでは完全に巻き戻せない可能性。

このためV2でpure calculator契約化する意義が大きい。

---

## 19. Audit不足

backupではexecution historyがあったがRuleにはない。

現在残るもの:

```text
HTTP trace log
RuleMaster BaseEntity created/updated/deleted metadata
給与結果（将来接続部分）
```

不足:

```text
誰がRuleを変更したかの詳細before/after
誰がRuleを実行したか
rule version
parameter snapshot
catalog version
calculator version
result
success/failure
```

### 修正候補

RuleDefinitionAudit / RuleExecutionSnapshotを別責務で検討。

ただしRule Test実行履歴と本番給与Execution Snapshotは分けた方がよい。

---

## 20. Error taxonomy候補

現在:

```text
RULE_INVALID_REQUEST
RULE_CONFLICT
COMMON_RESOURCE_NOT_FOUND_ERROR
COMMON_INTERNAL_ERROR
```

中心。

V2候補:

```text
RULE_NOT_FOUND
RULE_INACTIVE
RULE_PARAMETER_INVALID
RULE_PARAMETER_MISSING
RULE_DATASOURCE_INVALID
RULE_DATASOURCE_FETCH_FAILED
RULE_DSL_SYNTAX_ERROR
RULE_DSL_EXECUTION_FAILED
RULE_CALCULATION_FAILED
RULE_VERSION_NOT_FOUND
```

ただしAPIへ詳細内部情報は返さず、ErrorCodeで安全に分類する。

---

## 21. 現時点の評価

### 良い点

- Rule固有4xx分類あり
- 未分類内部Errorは汎用messageへ隠蔽
- EntityNotFound詳細もAPIへ隠蔽
- traceIdをResponse/API/logで連携
- stack traceは運用側ログへ保持
- RuntimeException rollback標準に乗る

### 注意点

- IllegalArgumentException messageを400へそのまま返す
- Converter Errorで入力valueをmessageへ含める
- 内部logにException message/stack traceを残すためredaction必要
- Rule Test success時facts全量返却
- DSL syntax errorとsystem errorの分類が粗い
- Rule execution audit/version snapshotなし
- JavaBean副作用を禁止していない

---

## 22. 修正候補 — 優先度

### P0

- Rule Parameter変換Errorから生値をAPI messageへ出さない
- SELECT */Fact全量露出の改善
- Tenant完全自動scope
- JavaBean Calculator副作用方針

### P1

- Rule Error taxonomy細分化
- Test/Preview用sanitized DSL Error
- structured logging redaction
- Rule Definition Audit
- Execution Snapshot/version記録

### P2

- Rule Test execution history
- observability metric（ruleName別件数/latency/error rate。ただしlabel cardinality注意）

---

## 23. 次の調査

次は横断的なTenant機構を確認する。

これまでRuleMaster/Catalog RepositoryでtenantId明示Queryと非明示Queryが混在していたため、

```text
TenantContext
→ TenantFilter / Hibernate Filter
→ BaseEntity
→ Repository
→ JdbcTemplate Rule DataSource
```

を一本で追い、Rule管理CRUDとRule実行のTenant安全性を最終確定する。

その後、system/rule全体の未決事項・修正候補とV2汎用化案へ進む。
