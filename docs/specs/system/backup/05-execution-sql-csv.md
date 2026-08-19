# system/backup 詳細設計 05 — 実行Validation・SQL生成・データ取得・CSVフォーマット

## 1. 対象範囲

この文書は `system/backup` のうち、**実行Validation・SQL生成・データ取得・CSVフォーマット**だけを対象とする。

対象経路:

```text
BackupExecutionService
→ BackupExecutionValidator
→ BackupDefinitionService
→ BackupExportColumnResolver
→ BackupSchemaInspector
→ BackupSqlBuilder
→ BackupDataFetcher
→ BackupCsvDataBuilder
→ CsvFileWriter
```

基準コードは `main` / `12c91a72b409df16b9d4be0b416247a07a8f170a`。

**今回もアプリケーションコードは変更しない。** 不整合・改善候補は記録のみとする。

---

## 2. 実行入口

通常画面実行は `BackupExecutionService#execute(List<String> targetCodes)`。

内部で `BackupRequest` を構築する。

```text
targetCodes = 指定値
encoding    = UTF-8
zipOutput   = null
```

その後 `execute(BackupRequest)` に委譲。

---

## 3. 実行Validation

`BackupExecutionValidator#validate()` が最初に実行される。

### request

null不可。

### targetCodes

- 1件以上必須
- 最大50件
- null/blank code不可
- 1code最大100文字
- comma連結後最大2000文字
- 重複不可

重複判定は `HashSet` で完全一致。

**実装事実**: 大文字小文字を正規化しないため `ABC` と `abc` はValidator上は別値。ただしBackupTargetのtargetCode自体は登録時に英大文字限定。

---

## 4. Validationと履歴保存の境界

`BackupExecutionService#execute(BackupRequest)` は:

```java
validator.validate(request);
BackupExecutionResult result = null;
try {
   ...
} catch (...) {
   saveFailureHistory(...)
}
```

という順序。

**重要な実装事実**: `BackupExecutionValidator` で失敗した例外は `try` の外で発生するため、失敗履歴保存処理には入らない。

したがって:

- request null
- targetCodes empty
- >50件
- blank code
- 重複

等はBackupHistoryにFAILED履歴を残さない。

**未決事項**: 入力Validation失敗も履歴対象とするか要仕様確認。

今回は修正しない。

---

## 5. Target定義取得

各targetCodeごとに `BackupSingleFileBuilder#build()` が呼ばれる。

最初に:

```text
BackupDefinitionService#getBackupTargetDefinition(targetCode)
```

を呼ぶ。

Repository条件:

```text
tenantId = current Tenant
AND targetCode = 指定値
AND backupEnabled = true
AND activeFlag = true
AND deletedAt IS NULL
```

**確定仕様**: 無効・停止・削除済みTargetは実行不可。

存在しない場合もRuntimeException。

---

## 6. 出力Column確定

`BackupExportColumnResolver#resolve()`:

```text
target.columns
→ exportFlag=trueのみ
→ orderNo ASC
```

0件ならRuntimeException。

通常は保存Validationでも最低1件保証されるが、DB直接変更等に備え実行時にも再確認する。

---

## 7. 実行時Schema確認

`BackupSchemaInspector#inspect(tableName)` を毎回実行する。

SQL:

```sql
SELECT * FROM tableName WHERE 1 = 0
```

からJDBC metadataを取得。

確認結果:

- Column名集合
- tenant_idの存在有無

**実装事実**: 実行時にも物理Table存在は再確認される。

ただし保存済みexport Column全件の存在確認を明示的に行うのではなく、最終SELECTで存在しないColumnがあればDBエラーになる。

---

## 8. SQL生成

`BackupSqlBuilder#buildSelectSql()`。

基本形:

```sql
SELECT col1, col2, col3 FROM table_name
```

tenantScopedの場合:

```sql
SELECT col1, col2, col3
FROM table_name
WHERE tenant_id = :tenantId
```

---

## 9. Identifier防御

BackupSqlBuilderはtableName / columnNameに対し:

```regex
^[a-zA-Z0-9_]+$
```

を要求する。

Target保存時のSchema Inspectorより若干緩く、先頭数字も許容するregexだが、保存Validation側では先頭英字を要求するため通常経路では一致しない値は入らない。

**実装事実**: 実行時にもDynamic SQL identifierを再検証している。

---

## 10. SQL Injection境界

テーブル名・Column名はbind parameter化できないため、identifier regexで防御。

tenantIdはNamed Parameter:

```text
:tenantId
```

としてbindする。

**確定仕様**: tenantIdはSQL文字列連結しない。

---

## 11. tenantId parameter

`BackupSqlBuilder#buildParameters()`:

### tenantScoped=false

```text
Map.of()
```

### tenantScoped=true

Tenant ID必須。

null/blankならRuntimeException。

```text
Map.of("tenantId", tenantId)
```

---

## 12. tenantScopedではないTable

物理Column `tenant_id` がないTableはWHERE条件なし。

```sql
SELECT ... FROM table
```

となる。

**確定仕様（現行）**: tenant_idを持たない共通マスタ等は全Rowをバックアップする。

**未決事項**: tenant_id欠落が設計ミスの業務Tableでも全Tenant相当データを出力するため、Schema設計そのものがセキュリティ境界になる。

今回は修正しない。

---

## 13. ORDER BY

生成SQLには `ORDER BY` がない。

**確定仕様（実装上）**: CSV Row順序はDBが返す自然順に依存し、順序保証されない。

同じバックアップを複数回実行してもRow順序が完全一致する保証はない。

**V2候補**:

- Target定義にsort keyを持つ
- Primary Key自動検出
- deterministic backupの仕様化

今回は修正しない。

---

## 14. WHERE条件

現行Backup SQLが追加する条件はtenantScoped時の:

```sql
WHERE tenant_id = :tenantId
```

のみ。

`deleted_at IS NULL` や `active_flag=true` 等は自動追加しない。

**実装事実**: soft delete済みRowも物理Tableに残っていればバックアップ対象になる。

これは「DB完全バックアップ」に近い意味なら合理的だが、「現在有効データexport」とは異なる。

**未決事項**: soft deleted Rowを含むことを正式仕様とするか要確認。

今回は修正しない。

---

## 15. データ取得

`BackupDataFetcher#fetch(sql, parameters)`:

```text
NamedParameterJdbcTemplate.queryForList()
```

を使用する。

戻り値:

```text
List<Map<String,Object>>
```

全Rowを一括取得。

**実装事実**: fetch size / streaming ResultSet / paginationは使っていない。

---

## 16. Transaction

`BackupExecutionService` 自体には `@Transactional` がない。

`BackupDataFetcher` にもない。

したがってDynamic SELECT全体を1つの明示Spring Transactionで囲ってはいない。

**実装事実**: 複数Targetを順次バックアップする間に元DBデータが更新された場合、Target間で同一時点snapshotになる保証はない。

単一TargetでもDB isolation / connection autocommit動作に依存する。

**未決事項**: backupにtransaction-consistent snapshotが必要か要確認。

**V2候補**: read-only transaction / DB snapshot戦略を明文化。

今回は修正しない。

---

## 17. DB負荷

SQLは対象Table全件SELECT。

LIMITなし。

対象ColumnはexportFlag=trueのみなのでSELECT列数は抑えられるが、Row数上限はない。

**実装事実**: 大規模TableでDB I/O・network・JVM heap負荷が比例して増える。

---

## 18. MapのColumn key

`NamedParameterJdbcTemplate#queryForList()` のMap keyはJDBC/Springが返すColumn label/nameに依存する。

その後 `BackupCsvDataBuilder` は:

```text
row.get(column.columnName())
```

で値を取る。

**潜在的な実装依存**: DB/JDBC driverのMap key case handlingと、保存されたcolumnName caseが一致しない場合、値取得がnullになる可能性を確認する余地がある。

MySQL通常利用では大きな問題になりにくいが、テストで明示保証されていない。

**V2候補**: RowMapperでkey normalization。

今回は修正しない。

---

## 19. BackupCsvDataBuilder

取得Rowを新しい `LinkedHashMap` に移す。

Column定義順に:

```text
result.put(columnName, row.get(columnName))
```

を行う。

LinkedHashMapのため挿入順は保持される。

ただし後段CsvFileWriterはcolumnKeysを別引数で指定するため、Map insertion order自体に依存しすぎない構造。

---

## 20. CSV列順

`BackupSingleFileBuilder` で:

```text
columnKeys = columns.map(columnName)
headerNames = columns.map(csvHeaderName)
```

`columns` は `BackupExportColumnResolver` によりorderNo ASC。

したがってCSV列順は `orderNo ASC`。

**確定仕様**: Row順は不定だがColumn順は定義順で保証される。

---

## 21. CSV encoding

`CsvFileWriter` は:

```text
UTF-8
```

固定。

先頭に3byte BOM:

```text
EF BB BF
```

を出力する。

**確定仕様**: UTF-8 with BOM。

`BackupRequest.encoding` は通常 `UTF-8` が設定されるが、CsvFileWriterはこの値を参照せずUTF-8固定。

**実装事実**: BackupRequest.encodingは現行CSV出力の切替には使われていない。

---

## 22. CSV Header

`includeHeader=true` の場合のみ:

```text
csvHeaderName一覧
```

を1行目に出力。

falseならHeaderなし。

Header数とColumn key数が不一致の場合CsvFileWriterでRuntimeException。

---

## 23. CSV quote / escape

OpenCSV `CSVWriter` を使用する。

そのためcomma、quote、改行を含む文字列はOpenCSV標準ルールでquote/escapeされる。

**確定仕様（ライブラリ依存）**: 手作業の文字列連結ではなくCSVライブラリでescapingする。

---

## 24. null表現

`CsvFileWriter#formatValue(null)`:

```text
""
```

つまりCSV上では空文字フィールド。

**重要**: DB NULLとDB空文字列はCSV上で区別できない。

**未決事項**: restore用途を考える場合、この不可逆性を許容するか要確認。

今回は修正しない。

---

## 25. LocalDateTime

Java値が `LocalDateTime` の場合:

```text
yyyy-MM-dd HH:mm:ss
```

例:

```text
2026-08-19 21:30:00
```

millisecond/nanosecondは出力しない。

---

## 26. LocalDate

Java値が `LocalDate` の場合:

```text
yyyy-MM-dd
```

---

## 27. Instant / Timestamp等

CsvFileWriterが特別扱いするのは:

- LocalDateTime
- LocalDate

だけ。

それ以外は:

```text
String.valueOf(value)
```

**実装事実**: JDBC driverが `java.sql.Timestamp` や `Instant` を返した場合、専用formatではなくtoString依存になる。

MySQL driver / Spring JDBCの実際の返却型に依存する。

**V2候補**: temporal type normalization。

今回は修正しない。

---

## 28. DECIMAL

`BigDecimal` 等は特別formatせず:

```text
String.valueOf(value)
```

通常はplain decimal文字列になるが、scaleは取得値に依存する。

BackupColumn.dataType=DECIMALはformat制御に使われない。

---

## 29. BOOLEAN

特別formatなし。

実際のJava値に応じて:

```text
true / false
0 / 1
```

等になり得る。

MySQL BIT/TINYINTのJDBC mappingに依存する。

**未決事項**: CSV上のboolean表現を固定するか要確認。

---

## 30. INTEGER / LONG

特別formatなし。

`String.valueOf()`。

---

## 31. dataTypeとの関係

前文書で確認した通りBackupColumn.dataTypeはCSV formatterで参照されない。

**確定仕様（現行実装）**: CSV値表現は「定義されたBackupDataType」ではなく「JDBCから返ったJava型」に依存する。

---

## 32. 改行

OpenCSVへ文字列を渡すため、フィールド内改行はquoteされたCSV fieldとして出力される。

CSV physical line数とDB Row数が一致しないケースはあり得る。

これは正しいCSV仕様上の挙動。

---

## 33. CSV Writerのメモリ

`ByteArrayOutputStream` に全CSVを書き込む。

したがって:

```text
DB全Row List<Map>
+ CSV byte[]
```

を同時保持する時間帯がある。

ZIP時はさらにZIP byte[]も追加。

**実装事実**: backup全体の主要スケーラビリティ制約はJVM heap。

---

## 34. 複数Target実行順

`request.targetCodes().stream().map(singleFileBuilder::build).toList()`。

つまりRequestで指定された順に逐次buildする。

並列処理ではない。

**確定仕様（実装上）**: 複数Targetは順次処理。

メリット:

- DB同時負荷が増えにくい

デメリット:

- 大量Targetでは総実行時間が長くなる
- snapshot時点がTargetごとにずれる

---

## 35. 途中失敗

例えば3Target中2件目でSQL失敗した場合:

- 1件目CSVはメモリ生成済み
- 最終BackupExecutionResultはまだ作られていない
- Storage保存は最終ResultBuilder段階なので通常まだ行われていない
- catchでFAILED履歴保存
- HTTPは失敗

**実装事実**: 個別Target単位の部分成功レスポンスはない。Request全体として失敗。

---

## 36. History errorMessage

SQL/JDBC/CSV例外は上位へRuntimeExceptionとして伝播し、BackupExecutionService catchで失敗履歴へmessageを保存する。

ただしExecutionValidator失敗だけは前述の通り履歴に残らない。

---

## 37. 現在のテスト保証

### BackupSqlBuilderTest

存在する。

主に:

- tenantScoped SQL
- parameter
- identifier validation

を保証している。

### BackupTargetValidatorTest

保存時物理Column存在確認等を保証。

### CsvFileWriter

今回 `features/system/backup` 配下には専用テストを確認していない。

共通file service側に別テストが存在する可能性はテスト総括時に再確認する。

---

## 38. 不足しているテスト

今回範囲で不足:

- BackupExecutionValidator全分岐
- Validation失敗時に履歴が残らない挙動
- 無効Target実行拒否
- tenantScoped / non-tenant table integration
- Row ORDER BYなしの挙動
- soft deleted Rowを含むこと
- queryForList大容量
- Map key case
- UTF-8 BOM
- includeHeader true/false
- null vs empty
- comma/quote/newline escape
- LocalDate / LocalDateTime
- Timestamp / Instant
- Decimal scale
- Boolean mapping
- 複数Target途中失敗
- Transaction snapshot consistency

---

## 39. 今回見つかった既知事項

### A. ExecutionValidator失敗は履歴に残らない

重要度: 中

validatorがtry-catch外。

修正しない。

### B. Row順序が非決定的

重要度: 中

ORDER BYなし。

修正しない。

### C. soft deleted Rowも出力対象

重要度: 要仕様確認

tenant_id以外のWHERE条件なし。

修正しない。

### D. 複数Targetで一貫したDB snapshotではない

重要度: 中〜高

明示Transactionなし・逐次実行。

修正しない。

### E. BackupRequest.encodingは実質未使用

重要度: 低〜中

UTF-8固定。

修正しない。

### F. NULLと空文字がCSV上で区別不能

重要度: restore用途なら高

修正しない。

### G. Temporal / Boolean表現がJDBC Java型依存

重要度: 中

dataType設定と連動しない。

修正しない。

---

## 40. 次に掘る範囲

次は `system/backup` の **テスト・例外・監査・運用・Infrastructure連携の総括** に限定する。

ここで:

```text
Backend unit/integration tests
Frontend tests
GlobalExceptionHandler
Audit
Clock
Docker env
AWS S3/IAM
Terraform lifecycle
CI
運用監視
```

をbackup機能に限って横断確認する。

これを終えると、backupサブシステムのV1詳細設計として一旦全体を閉じられる見込み。
