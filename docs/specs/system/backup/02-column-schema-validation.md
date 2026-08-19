# system/backup 詳細設計 02 — Column編集・Schema Validation

## 1. 対象範囲

この文書は `system/backup` のうち、**Column編集とSchema Validation** だけを対象とする。

対象経路:

```text
BackupTargetEditDialog.vue
→ useBackupColumnEditor.ts
→ backupColumnSchema
→ BackupTargetSaveRequest.columns
→ BackupTargetValidator#validateColumns / validateColumn
→ BackupSchemaInspector#inspect
→ JDBC DataSource / ResultSetMetaData
→ BackupColumn Entity
→ BackupExportColumnResolver
→ BackupSingleFileBuilder
→ BackupCsvDataBuilder
→ CsvFileWriter
```

基準コードは `main` / `12c91a72b409df16b9d4be0b416247a07a8f170a`。

**今回もアプリケーションコードは変更しない。** 不整合・改善候補は記録のみとする。

---

## 2. 画面構成

`BackupTargetEditDialog.vue` の `Column` タブは左右2ペイン構成。

左:

- Column一覧 `SimpleTable`
- Toolbar
  - 追加
  - ↑
  - ↓
  - 削除

右:

- 選択中Column詳細
- `GridBasedForm`

画面上には以下の注意文を表示する。

> 保存時に実DBのテーブル・カラムと照合します。存在しない項目は登録できません。

---

## 3. Frontend Column Model

主要型:

`frontend/src/features/system/backup/types/backupFormTypes.ts`

Columnフォームが保持する主な項目:

- `id`
- `columnName`
- `csvHeaderName`
- `dataType`
- `exportFlag`
- `orderNo`

新規Columnは `createEmptyBackupColumnForm()` で作る。

初期値:

| 項目 | 初期値 |
|---|---|
| id | 一時的な負数 |
| columnName | `''` |
| csvHeaderName | `''` |
| dataType | `STRING` |
| exportFlag | `true` |
| orderNo | 指定値 |

一時IDは `-1`, `-2`, ... と減算される。

保存時には `id <= 0` を `null` に変換する。

---

## 4. Column一覧表示

`useBackupColumnEditor()` は `formModel.columns` を `orderNo ASC` でsortして一覧表示する。

表示列:

- 順 `orderNo`
- `columnName`
- `csvHeaderName`
- `dataType`
- Export

Export欄は:

```text
exportFlag=true  → "出力"
exportFlag=false → "-"
```

---

## 5. Column追加

`useBackupColumnEditor#add()`:

```text
1. createEmptyBackupColumnForm(columns.length + 1)
2. 現在配列末尾へ追加
3. resequence()
4. 新規Columnを選択状態にする
```

`resequence()` は全件に対し配列順で:

```text
orderNo = index + 1
```

を再設定する。

**実装事実**: Column追加時にDB SchemaからColumn候補を取得する処理はない。利用者が `columnName` を手入力する。

---

## 6. Column削除

`remove()`:

```text
1. selectedColumnIndexを取得
2. 選択Columnを配列からsplice
3. resequence()
4. 先頭Columnを選択状態にする
```

Frontend上の削除は即時APIではなく、ダイアログ内Form配列から除外するだけ。

実DBへの反映はTarget全体を保存した時点。

更新時はBackend MapperがColumn全件を再構築するため、削除されたColumnは `orphanRemoval=true` により削除対象となる。

---

## 7. 並べ替え

### 上へ

`moveUp()` は選択Columnと1つ前の要素をswapする。

### 下へ

`moveDown()` は選択Columnと1つ後の要素をswapする。

いずれもswap後に `resequence()` を実行する。

**確定仕様**: orderNoは画面操作後常に1始まりの連番になる。

Backend Validationは「1以上・重複なし」のみを要求し、欠番自体は直接禁止していない。

---

## 8. Frontend Column Validation

`backupColumnSchema`:

### columnName

- 必須

### csvHeaderName

- 必須

### dataType

以下のみ:

- STRING
- INTEGER
- LONG
- DECIMAL
- BOOLEAN
- DATE
- DATETIME

### exportFlag

boolean

### orderNo

- number
- 1以上

### 注意

Frontend Column schemaでは以下を検証しない。

- 実DBにColumnが存在するか
- columnName重複
- csvHeaderName重複
- orderNo重複
- DB物理型とdataTypeの一致
- identifier文字種

これらの一部はBackendで検証する。

---

## 9. 保存Request

`toBackupTargetSaveRequest()` はColumnを `orderNo ASC` でsortしてRequestへ変換する。

各Column:

```text
id            : >0ならその値、<=0ならnull
columnName    : trim
csvHeaderName : trim
dataType       : そのまま
exportFlag     : そのまま
orderNo        : そのまま
```

ただしBackend MapperではColumn IDをignoreするため、更新時のID維持には利用されない。

---

## 10. Backend Validation全体

`BackupTargetValidator#validate()` はTarget基本項目確認後、

```text
BackupSchemaInspector.inspect(tableName)
→ validateColumns(request, schema)
```

を実行する。

Column Validationは、**実DB Schema照合を含む**。

---

## 11. BackupSchemaInspector

主要クラス:

`backend/src/main/java/com/project/backend/features/system/backup/service/validation/BackupSchemaInspector.java`

### 11.1 tableName identifier検証

regex:

```regex
[A-Za-z][A-Za-z0-9_]{0,199}
```

したがって:

- 先頭: 英字
- 以降: 英字・数字・underscore
- 最大200文字

schema名・`.`・quote等は許可しない。

### 11.2 Schema取得SQL

以下を実行する。

```sql
SELECT * FROM {tableName} WHERE 1 = 0
```

実データは取得しない。

JDBC:

```text
DataSource.getConnection()
→ PreparedStatement
→ executeQuery()
→ ResultSetMetaData
```

### 11.3 Column名取得

`ResultSetMetaData#getColumnName(index)` を全Columnに対して呼び、lowercase化してSet化する。

### 11.4 tenantScoped判定

取得Column Setに:

```text
tenant_id
```

が存在する場合 `tenantScoped=true`。

**確定仕様**: テーブルのテナント対象判定はannotationや設定ファイルではなく、物理Column `tenant_id` の存在有無で決まる。

---

## 12. information_schemaは直接使わない

**実装事実**: `BackupSchemaInspector` はMySQL `information_schema` を直接検索していない。

`SELECT * ... WHERE 1=0` + JDBC `ResultSetMetaData` を使う。

そのためコード上はDB製品固有SQLを極力避けた実装になっている。

ただし接続DataSourceが対象DBへSELECT権限を持つ必要はある。

---

## 13. table存在確認

存在しないtableNameを指定するとSQL実行に失敗する。

catchして:

```text
RuntimeException(
  "バックアップ対象テーブルを確認できません。 tableName=..."
)
```

へ変換する。

**確定仕様**: 実在しないテーブルは保存不可。

---

## 14. Column identifier検証

`BackupTargetValidator#validateColumn()` は:

```text
schemaInspector.validateIdentifier(column.columnName(), "columnName")
```

を呼ぶ。

tableNameと同じregexを使うため、Column名も:

```regex
[A-Za-z][A-Za-z0-9_]{0,199}
```

のみ許可。

**実装事実**: MySQLとして利用可能でも、`$`, 日本語Column名、予約語エスケープ前提の名前等はこの機能から指定できない。

---

## 15. 物理Column存在確認

`BackupSourceSchema#containsColumn()` により物理Column存在を確認する。

Schema Inspector側でColumn名をlowercase化して保持するため、存在確認は大文字小文字を吸収する構造。

存在しない場合:

```text
"対象テーブルに存在しないカラムです。 columnName=..."
```

で拒否。

`BackupTargetValidatorTest#missingPhysicalColumnIsRejected()` がこの挙動をテストしている。

---

## 16. Column重複Validation

1 Target内で以下を禁止。

### columnName

lowercase化して比較するためcase-insensitive重複禁止。

例:

```text
employee_id
EMPLOYEE_ID
```

は同一扱い。

### csvHeaderName

完全一致で重複禁止。

### orderNo

同一数値禁止。

---

## 17. exportFlag

`BackupTargetValidator#validateColumns()` は最低1件:

```text
exportFlag != false
```

のColumnを要求する。

したがって全Columnを `exportFlag=false` にして保存することはできない。

実行時は `BackupExportColumnResolver#resolve()` が:

```text
exportFlag=true のColumnのみ
→ orderNo ASC
```

で抽出する。

**確定仕様**: `exportFlag=false` のColumnは設定としてDBには保持されるが、CSV SELECT対象から除外される。

---

## 18. orderNo

Backendでは:

- null不可
- 1以上
- 重複不可

を確認する。

ただし:

```text
1, 3, 10
```

のような欠番はValidator上許容される。

Frontend通常操作ではresequenceされるため通常は連番となる。

**実装事実**: APIを直接呼ぶ場合は欠番を作成可能。

---

## 19. dataType

Entity `BackupColumn` は `BackupDataType` を保持する。

許可値:

- STRING
- INTEGER
- LONG
- DECIMAL
- BOOLEAN
- DATE
- DATETIME

### 19.1 DB物理型との一致検証

**実装事実**: `BackupSchemaInspector` はColumn名だけを取得し、JDBC type / type nameを保持していない。

`BackupTargetValidator` も `dataType != null` しか確認しない。

したがって例えば:

```text
DB: VARCHAR
設定: DATETIME
```

でも保存時点では拒否されない。

### 19.2 CSV生成時の利用

`BackupCsvDataBuilder` は `BackupColumnDefinition.dataType` を参照していない。

単純に:

```text
row.get(columnName)
```

を新しいMapへ移す。

`CsvFileWriter#formatValue()` はJava実値の型だけを見る。

- `LocalDateTime` → `yyyy-MM-dd HH:mm:ss`
- `LocalDate` → `yyyy-MM-dd`
- null → 空文字
- その他 → `String.valueOf(value)`

**確定仕様（現行実装）**: CSV出力フォーマットはBackupColumnの `dataType` ではなく、JDBCから返ったJavaオブジェクトの実型に依存する。

### 19.3 dataTypeの現在の意味

**実装事実**: 現在確認範囲では、`dataType` は保存・表示される定義情報だが、CSV変換ロジックやSchema型整合判定には使われていない。

**未決事項**: 将来の型別フォーマット・restore/import用途・UI説明用途を想定した項目なのかはコードだけでは確定できない。

**V2候補**: dataTypeを正式仕様にするなら、DB物理型との互換性Validationか、CSV formatterへの利用方針を定義する。

今回は修正しない。

---

## 20. tenant_idをColumnとして選べるか

**実装事実**: `BackupTargetValidator` は `tenant_id` を予約Columnとして除外していない。

対象テーブルに `tenant_id` が存在し、Column定義として登録すればexport対象に設定できる。

また `created_at`, `updated_at`, `deleted_at` 等についても特別な除外処理は確認できない。

したがって物理的に存在しidentifier条件を満たすColumnは設定可能。

**未決事項**: `tenant_id` をCSVへ出力してよいかは業務・セキュリティ仕様として要確認。

今回は修正しない。

---

## 21. tenantScopedとSELECT

Schemaに `tenant_id` がある場合:

```text
tenantScoped = true
```

となり、実行時 `BackupSqlBuilder` が:

```sql
WHERE tenant_id = :tenantId
```

を付ける。

つまり `tenant_id` をexport Columnに含めるかどうかとは独立して、**Row絞り込み条件として必ず利用される**。

---

## 22. Schema変更時の挙動

BackupTarget保存時点では物理Table/Column存在を確認する。

しかし保存後にDB Schemaが変更される可能性がある。

実行時 `BackupSingleFileBuilder` でも:

```text
BackupDefinitionService
→ BackupExportColumnResolver
→ BackupSchemaInspector.inspect(tableName)
→ BackupSqlBuilder
```

を行うため、実行時にも再度Schemaを読む。

### 注意

**実装事実**: 実行時Schema Inspectorはtable全体のColumn集合を取得するが、保存済み設定Columnの存在をここで再度明示Validationする処理は `BackupSingleFileBuilder` 内にはない。

その後SELECT SQLを実行するため、設定Columnが削除されていればSQL execution errorで失敗する。

**V2候補**: 実行前に保存済みColumn定義と現在Schemaを明示照合し、より明確なBusiness Errorを返す。

今回は修正しない。

---

## 23. Column Entity

`BackupColumn`:

```text
Table: backup_column
```

主要項目:

- id
- target_id
- tenant_id
- column_name
- csv_header_name
- data_type
- export_flag
- order_no
- created_at
- updated_at
- deleted_at

Unique:

```text
(target_id, column_name)
(target_id, order_no)
```

Application Validationでは `csvHeaderName` も重複禁止だがDB Unique constraintはない。

---

## 24. 更新時Column全置換との関係

前文書 `01-target-crud.md` で確認した通り、Target更新時は:

```text
entity.clearColumns()
→ request.columnsを全件new BackupColumnへ変換
```

する。

そのためこのColumn Editor上の既存IDは、Backendで個別更新識別には使われない。

**実装事実**: order変更だけの保存でも、Column Entity全件が再作成される構造。

---

## 25. 現在のテスト保証

`BackupTargetValidatorTest` で確認できるケース:

### 25.1 物理Column不存在

`missingPhysicalColumnIsRejected()`

存在しないColumnを指定するとRuntimeException。

### 25.2 immutable field

`targetCodeAndTableCannotBeChanged()`

既存TargetのtargetCode変更を拒否。

### 25.3 fileNamePattern

`fileNamePatternRequiresTimestamp()`

`{timestamp}` 不在を拒否。

---

## 26. Column/Schemaで不足しているテスト

今回確認範囲で専用テストが不足しているもの:

- `BackupSchemaInspector` 実DB/JDBC metadataテスト
- table不存在
- identifier不正
- tenant_id検知
- 大文字小文字Column名
- columnName重複
- csvHeaderName重複
- orderNo重複
- exportFlag全false
- orderNo欠番
- dataType null
- DB型とdataType不一致
- `tenant_id` export可否
- Schema変更後の実行失敗
- Frontend add/remove/moveUp/moveDown
- resequence
- 一時負数ID
- Column Form validation

---

## 27. 今回見つかった既知事項

### A. dataTypeは実出力に使われていない

重要度: 中

設定値と実CSV変換が連動していない。

修正はしない。

### B. DB物理型とdataTypeを照合しない

重要度: 中

Schema InspectorはColumn名のみ確認。

修正はしない。

### C. tenant_id等のシステムColumnをexport可能

重要度: 要業務確認

禁止処理なし。

修正はしない。

### D. Schema保存後変更時はSQL実行時エラーになる

重要度: 中

実行前の保存定義↔現Schema明示照合はない。

修正はしない。

### E. Column EditorはDB Schema候補選択式ではなく手入力

重要度: UX観点

保存時に初めてBackendで実在照合する。

修正はしない。

---

## 28. 次に掘る範囲

次は `system/backup` の **履歴一覧・履歴ファイル再ダウンロード** に限定する。

```text
BackupHistoryTable.vue
→ useBackupHistoriesQuery.ts
→ useDownloadBackupHistoryFileMutation.ts
→ BackupController#findHistories / downloadHistoryFile
→ BackupHistoryService
→ BackupHistoryLookupService
→ BackupHistoryFileDownloadService
→ StorageService
→ BackupHistory Entity
```

ここで以下を確定する。

- 履歴200件制限
- 成功/失敗表示
- storedFileKey / storageType
- DOWNLOADのみ実行時の再DL可否
- Storage消失時の挙動
- Tenant境界
- ファイル名/Content-Type
- 履歴の削除・保持期間の有無
- テスト保証
