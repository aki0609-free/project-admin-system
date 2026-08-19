# system/rule 詳細設計 02 — RuleParameter

## 1. 対象範囲

今回は `system/rule` のうち、**RuleParameterだけ**を対象とする。

対象経路:

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

基準コード:

- branch: `main`
- commit: `12c91a72b409df16b9d4be0b416247a07a8f170a`

**この調査ではコード修正を行わない。**

---

## 2. Parameterの役割

RuleParameterはRule実行時に外部から渡す入力値の定義である。

各Parameterは少なくとも以下を持つ。

- paramName
- dataType
- requiredFlag
- defaultValue
- description
- orderNo

実行時は `RuleParameterResolver` がRequest parameterをRuleParameter定義に従って解決し、型変換済みMapを作る。

---

## 3. Frontend UI

主要ファイル:

`frontend/src/features/system/rule/components/RuleParameterTab.vue`

画面は左一覧＋右詳細の2ペイン。

### Toolbar

- 追加
- 削除

### 一覧列

- paramName
- dataType
- required
- default
- order
- 説明

### 詳細編集項目

- paramName
- dataType
- requiredFlag
- defaultValue
- orderNo
- description

---

## 4. Parameter追加

`addParameter()`:

```text
nextOrderNo = parameters.length + 1
createEmptyRuleParameter(nextOrderNo)
→ parameters.push()
→ 追加Parameterを選択
```

初期値:

| 項目 | 値 |
|---|---|
| id | 一時負数 |
| paramName | '' |
| dataType | STRING |
| requiredFlag | false |
| defaultValue | '' |
| description | '' |
| orderNo | 末尾+1 |

---

## 5. Parameter削除

`removeParameter()` は選択中ParameterをFrontend配列から除外するだけ。

即時DELETE APIは存在しない。

Rule全体の保存時にBackend MapperがParameter全件を再構築するため、除外されたParameterはRule更新時に消える。

---

## 6. 並べ替えUI

**実装事実**: RuleParameterTabには上/下移動ボタンや自動resequence処理はない。

orderNoは右詳細のnumber inputから利用者が直接編集する。

したがってFrontend上でも:

```text
1, 3, 10
```

のような欠番や、重複orderNoを入力できる。

Backend Validatorは `orderNo > 0` のみ確認し、Parameter間のorderNo重複は検証しない。

**既知事項**: Parameter orderNoの一意性・連番性は保証されない。

今回は修正しない。

---

## 7. dataType

Frontend選択肢:

- STRING
- INTEGER
- LONG
- DECIMAL
- BOOLEAN
- DATE
- DATETIME

Backend Entityも `RuleDataType` Enumを保持する。

保存時Validatorでnullは禁止。

---

## 8. paramName Validation

`RuleMasterValidator#validateParameters()`:

- 必須
- identifier形式
- Rule内で重複不可

identifier regexは共通:

```regex
^[a-zA-Z0-9_]+$
```

**実装事実**: paramNameは英数字underscoreのみ。

大文字小文字は区別してHashSetへ入れるため:

```text
amount
AMOUNT
```

は別Parameterとして保存可能。

**未決事項**: Parameter名をcase-sensitiveとすることが正式仕様か。

---

## 9. defaultValue

DB Entityでは:

```text
default_value VARCHAR(500)
```

でStringとして保存する。

**重要**: defaultValueはdataType別の型では保存せず、常に文字列。

実行時に `RuleValueConverter` を通してParameterのdataTypeへ変換する。

---

## 10. requiredFlagとdefaultValue

実行時 `RuleParameterResolver#applyParameter()` の優先順位:

```text
1. Requestに値あり → Request値
2. Request値がmissing + defaultValueあり → defaultValue
3. それでもmissing + requiredFlag=true → Error
4. それでもmissing + requiredFlag=false → resolved Mapから除去
```

**確定仕様（現行実装）**: required=trueでもdefaultValueが設定されていればRequest省略時にエラーにならず、defaultValueを使う。

つまりrequiredFlagは「呼出元が必ず送る」ではなく、**最終解決後に値が必須**という意味。

---

## 11. missing判定

`RuleParameterResolver#isMissing()`:

missing扱い:

- null
- Stringでblank

StringUtils.hasTextを使うため:

- `""`
- `"   "`

もmissing。

一方:

- 0
- false
- empty collection

等はmissing扱いではない。

---

## 12. Requestに未定義Parameterが含まれる場合

Resolverは最初に:

```text
resolved = new LinkedHashMap<>(input)
```

を作る。

その後、RuleParameterとして定義されている項目だけを解決・変換する。

したがってRequestにRuleParameter未定義のkeyが含まれていても、そのkeyはresolved Mapへ残る。

**実装事実**: RuleParameter定義はRequest parameterのallowlistとしては機能しない。

例:

```text
Rule定義: amountのみ
Request: amount=100, debug=true
```

の場合、`debug` もresolved parameters Mapへ残る。

**未決事項**: 未定義ParameterをDSL/Beanから参照可能にしてよいか。

セキュリティ・仕様上の確認対象。

今回は修正しない。

---

## 13. optional Parameter未指定時

required=false、defaultValueなし、Request値なしの場合:

```text
resolved.remove(paramName)
```

となる。

つまり:

```text
paramName → null
```

を残すのではなく、Map key自体を消す。

DSL側で「null」と「変数未定義」の扱いが異なる可能性があるため、Executor解析時の重要ポイント。

---

## 14. 型変換タイミング

Parameter値はRule保存時には型変換しない。

実行時:

```text
RuleParameterResolver
→ RuleValueConverter.convert()
```

で変換する。

したがって不正なdefaultValueもRule保存時には検出されない可能性がある。

例:

```text
dataType = INTEGER
defaultValue = abc
```

RuleMasterValidatorはdefaultValue内容をdataTypeへ変換確認しないため保存可能。

実行時にdefaultValueが使われた時点で変換エラーになる。

**既知事項**: defaultValueの型妥当性は保存時保証されない。

今回は修正しない。

---

## 15. STRING変換

```text
String.valueOf(value)
```

を使用する。

既にStringならその値。
Number/Boolean等もtoString相当になる。

---

## 16. INTEGER変換

値がNumberの場合:

```text
number.intValue()
```

String等の場合:

```text
Integer.valueOf(trimmed text)
```

### 注意

NumberからIntegerへの変換は範囲・小数部を厳密Validationせず `intValue()`。

例えばBigDecimal `1.9` がNumberとして渡れば1へ切り捨てられる可能性がある。

またLongの範囲外値もintValueによる縮小が起こりうる。

**既知事項**: Number→INTEGERは厳密変換ではない。

---

## 17. LONG変換

Numberなら:

```text
number.longValue()
```

それ以外は:

```text
Long.valueOf(trimmed text)
```

INTEGER同様、Number入力時の小数部等を厳密Validationしない。

---

## 18. DECIMAL変換

既にBigDecimalならそのまま。

その他:

```text
new BigDecimal(trimmed String.valueOf(value))
```

**実装事実**: Double等も一度String化してBigDecimal化する。

---

## 19. BOOLEAN変換

受理:

- Boolean true/false
- String `true` / `false`（case-insensitive）

拒否:

- 1 / 0
- yes / no
- on / off

**確定仕様（現行実装）**: Boolean文字列表現はtrue/falseのみ。

---

## 20. DATE変換

既にLocalDateならそのまま。

その他:

```java
LocalDate.parse(trimmed text)
```

Java標準ISO_LOCAL_DATE形式。

通常:

```text
yyyy-MM-dd
```

例:

```text
2026-08-19
```

---

## 21. DATETIME変換

既にLocalDateTimeならそのまま。

その他:

```java
LocalDateTime.parse(trimmed text)
```

Java標準ISO_LOCAL_DATE_TIME形式。

例:

```text
2026-08-19T22:00:00
```

**実装事実**: `yyyy-MM-dd HH:mm:ss` の空白区切り文字列はそのままではLocalDateTime.parseできない。

Timezone/offsetはLocalDateTimeなので保持しない。

---

## 22. 変換失敗Error

RuleValueConverterは内部RuntimeExceptionをcatchし:

```text
{valueName}を{dataType}へ変換できません。 value={value}
```

というIllegalArgumentExceptionへwrapする。

Parameterの場合valueNameは:

```text
Ruleパラメータ {paramName}
```

---

## 23. Parameter保存方式

`RuleMasterMapper#applyRequest()`:

```text
entity.clearParameters()
→ request.parameters
   orderNo ASC sort
→ 全件new RuleParameter()
→ addParameter()
```

Frontend Requestは既存Parameter IDを送るがBackend MapperはIDを利用しない。

**確定仕様（現行実装）**: Rule更新時Parameter IDは維持されない可能性が高い。

RuleMaster側はcascade ALL + orphanRemoval=true。

---

## 24. orderNoの役割

保存時:

```text
request.parametersをorderNo ASCでsort
```

実行時Resolverでも:

```text
RuleParameter::getOrderNo ASC
```

で処理する。

Parameter同士は基本的に独立しているため、現行Resolverでは処理順が結果に影響する箇所は少ない。

ただしResponse表示順にも利用される。

---

## 25. Parameter Entity

Table:

```text
rule_parameter
```

主な列:

- id
- rule_id
- tenant_id
- param_name
- data_type
- required_flag
- default_value
- description
- order_no
- created_at
- updated_at
- deleted_at

`RuleMaster` とManyToOne。

RuleMaster側からcascade管理される。

---

## 26. Unique Constraint

`RuleParameter` Entity自身にはparamName/orderNoのUnique constraint定義はない。

Application ValidatorではparamName重複だけを禁止。

orderNo重複は許容。

**実装事実**: 同一Rule内のparamName一意性はApplication Validationのみ。

---

## 27. Tenant / soft delete

RuleParameterはBaseEntity継承のため:

- tenantId
- deletedAt

を持つ。

RuleParameterResolverは:

```text
parameter.getDeletedAt() == null
```

を明示filterする。

Rule削除時はRuleMasterCommandServiceがParameterにも同じdeletedAtを設定する。

ただしRule更新時は全置換のため、旧ParameterはorphanRemovalの影響を受ける。

この際の物理削除/soft deleteの実DB挙動は、JPA orphanRemovalのため別途注意が必要。

**未決事項**: 更新時にclearParametersした旧Parameterが物理DELETEされるか、BaseEntity soft delete運用と整合しているかをDB/JPA動作として後で確認する。

---

## 28. Frontend Validation

RuleParameterTab自体にはZod等の独立form schemaは確認できない。

利用者はUI上:

- 空paramName
- orderNo=0
- 重複paramName

なども一時的に入力でき、保存時Backend Validatorで拒否される構造。

**実装事実**: Parameterの最終Validation責務はBackend寄り。

---

## 29. テスト保証

今回のRepository検索では:

- `RuleParameterResolverTest`
- `RuleValueConverterTest`

という専用test fileを確認できなかった。

**現時点で不足しているテスト候補**:

- Request値優先
- blank→default
- required + default
- required + missing Error
- optional missing→Map key remove
- 未定義Request keyが残る挙動
- STRING/INTEGER/LONG/DECIMAL/BOOLEAN/DATE/DATETIME全変換
- INTEGER overflow/truncation
- Boolean 1/0 rejection
- invalid defaultValue
- Parameter ID全置換
- duplicate paramName
- duplicate orderNo挙動

検索indexの制約もあるため、後続のtest tree全体確認で再確認する。

---

## 30. 今回見つかった既知事項

### A. 未定義Request Parameterをそのまま通す

重要度: 高・仕様/安全性確認

RuleParameterがallowlistとして機能しない。

修正しない。

### B. defaultValueの型妥当性を保存時に検証しない

重要度: 中

実行時初めてErrorになる。

修正しない。

### C. Number→INTEGER/LONGの変換が非厳密

重要度: 中

小数切捨て/overflowの可能性。

修正しない。

### D. Parameter orderNo重複を許容

重要度: 低〜中

UIも直接入力式。

修正しない。

### E. Parameter ID全置換

重要度: 中

更新時ID継続性なし。

修正しない。

### F. optional missingはnullではなくMapから除去

重要度: DSL互換性確認

JEXL/MVELで未定義変数の扱いに差がある可能性。

修正しない。

### G. DATETIMEはISO `T` 区切り

重要度: UI/API仕様

一般的な `yyyy-MM-dd HH:mm:ss` 入力との不一致可能性。

修正しない。

---

## 31. 次に掘る範囲

次は **RuleDataSourceとColumn Mappingの「設定構造」だけ**を解析する。

まだ実際のSQL取得 `GeneralDataFetcher` の詳細には深く入らず、まず管理画面・保存定義を固める。

追跡順:

```text
RuleDataSourceTab.vue
→ RuleDataSourceList / Editor
→ RuleColumnList / Editor
→ RuleDataSourceSaveRequest / RuleColumnMappingSaveRequest
→ RuleMasterValidator#validateDataSources / validateColumns
→ RuleMasterMapper#toDataSource / toColumnMapping
→ RuleDataSource Entity
→ RuleColumnMapping Entity
```

次段階でCatalogとの関係、tableName/whereClause、singleRowFlag、factKey、dataType、子ID全置換を確定する。
