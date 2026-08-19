# system/backup 詳細設計 04 — ファイル生成・OutputMode・Storage key / filenameルール

## 1. 対象範囲

この文書は `system/backup` のうち、**ファイル生成・OutputMode・Storage key・filenameルール** だけを対象とする。

対象経路:

```text
BackupExecutionService
→ BackupOutputResolver
→ BackupExecutionResultBuilder
→ BackupZipService
→ BackupFileNameBuilder
→ BackupFileKeyBuilder
→ BackupFileStorageService
→ DocumentStorageKeyResolver
→ StorageService
→ LocalStorageService / S3StorageService
```

基準コードは `main` / `12c91a72b409df16b9d4be0b416247a07a8f170a`。

**今回もアプリケーションコードは変更しない。** 不整合・改善候補は記録のみとする。

---

## 2. 最終出力の種類

バックアップ実行の最終レスポンスは以下のどちらか。

- CSV
- ZIP

最終判定は `BackupOutputResolver#shouldZip()`。

---

## 3. ZIP判定

`BackupOutputResolver#shouldZip(request, files)` の判定順:

### 3.1 request.zipOutput == true

必ずZIP。

対象1件でもZIP。

### 3.2 request.zipOutput == false

```text
files.size() > 1
```

の場合のみZIP。

つまり明示falseでも複数ファイルを1レスポンスで返すためZIP化する。

### 3.3 request.zipOutput == null

```text
files.size() > 1
OR
いずれかのSingleBackupFile.zipRequired == true
```

ならZIP。

Controllerから通常呼ばれる `execute(List<String>)` は `zipOutput=null` を設定するため、通常画面操作ではこの3.3ルールが適用される。

**確定仕様**:

- 複数対象は必ずZIP
- 1対象でも `zipRequired=true` ならZIP
- 1対象かつ `zipRequired=false` ならCSV

---

## 4. OutputMode

`BackupOutputMode`:

- `DOWNLOAD`
- `SERVER_FILE`
- `BOTH`

`BackupOutputResolver#shouldSaveToServer()` は:

```text
SERVER_FILE → true
BOTH        → true
DOWNLOAD    → false
```

を返す。

---

## 5. OutputModeとHTTPレスポンスの関係

**重要な実装事実**: `SERVER_FILE` でも `BackupExecutionResult` 自体には `data` が入り、Controllerはそのbyte[]をHTTPレスポンスとして返す。

つまりBackend実装上は:

```text
DOWNLOAD    = HTTP返却 / Storage保存なし
SERVER_FILE = HTTP返却 / Storage保存あり
BOTH        = HTTP返却 / Storage保存あり
```

となる。

Frontendの項目名は `SERVER_FILE = ストレージ保存のみ` だが、現行Controller実装はレスポンスbodyを返している。

**未決事項**: Frontend側でSERVER_FILE時にダウンロードを抑止する別処理があるか、または名称と実装が不一致かは業務仕様確認が必要。

**V2候補**: OutputModeの意味を「保存先」ではなく「返却方式」まで含めて明文化する。

今回は修正しない。

---

## 6. 単体CSV生成結果

`BackupExecutionResultBuilder#buildSingle()`:

```text
1. saveSingleFileIfNeeded(file)
2. BackupExecutionResult生成
```

Content-Type:

```text
text/csv; charset=UTF-8
```

返却項目:

- fileName
- contentType
- data
- zipOutput=false
- storedFile

Storage保存対象でなければ `storedFile=null`。

---

## 7. ZIP生成

`BackupExecutionResultBuilder#buildZip()`:

```text
1. BackupZipService#createZip(files)
2. BackupFileNameBuilder#buildZipFileName()
3. saveZipIfNeeded(...)
4. BackupExecutionResult生成
```

Content-Type:

```text
application/zip
```

---

## 8. ZIP内部ファイル名

`BackupZipService#createZip()` は各 `SingleBackupFile.fileName()` をZipEntry名に使う。

ただし `safeFileName()` で:

```text
\  → _
/   → _
..  → _
trim
```

を実施する。

**確定仕様**: ZIP entryとしてディレクトリ階層を作らず、全ファイルをZIP root直下へ置く。

Zip Slip対策としてpath separatorと `..` を無害化している。

---

## 9. CSVファイル名

`BackupFileNameBuilder#buildCsvFileName(targetCode, fileNamePattern)`。

### fileNamePatternあり

使用可能placeholder:

- `{targetCode}`
- `{timestamp}`

保存前Validationで他placeholderは拒否される。

例:

```text
{targetCode}_{timestamp}.csv
```

### fileNamePatternなし

```text
{targetCode}_{timestamp}.csv
```

相当のdefault。

---

## 10. timestamp形式

`BackupFileNameBuilder`:

```text
yyyyMMdd_HHmmss_SSS_{UUID先頭8文字}
```

例:

```text
20270101_000001_000_a1b2c3d4
```

UUIDを付与するため、同一millisecond実行でも衝突を避ける。

---

## 11. timestamp timezone

`BackupFileNameBuilder` は:

```java
LocalDateTime.now(clock)
```

を使用する。

したがってtimezoneはDIされた `Clock` のZoneに依存する。

`BackupFileNameBuilderTest#buildCsvFileName_shouldUseTokyoDateAcrossYearBoundary()` では:

```text
Instant = 2026-12-31T15:00:01Z
Zone = Asia/Tokyo
```

から:

```text
20270101_000001_000_...
```

になることを保証している。

**確定仕様（テスト保証）**: Application ClockがAsia/Tokyoの場合、filename timestampも日本時間。

---

## 12. ZIPファイル名

`BackupFileNameBuilder#buildZipFileName()`:

```text
backup_{timestamp}.zip
```

CSVと同じunique timestampを使用する。

`BackupFileNameBuilderTest` でformatが保証されている。

---

## 13. fileName path防御

保存時 `BackupFileKeyBuilder#validateFileName()` は:

- blank不可
- `/` 不可
- `\` 不可

を確認する。

そのためfileNameを通じて任意フォルダへ保存することはできない。

`BackupFileKeyBuilderTest#rejectsPathInFileName()` が `../report.csv` を拒否することを保証する。

---

## 14. Storage key生成

`BackupFileKeyBuilder#build(outputDir, fileName)` が担当する。

概念構造:

```text
DocumentArea.BACKUPS root
  / system
  / {tenantId}
  / {outputDir optional}
  / {fileName}
```

Default設定を使うテストでは:

```text
documents/backups/system/tenant-a/master-data/REPORT_20260725.csv
```

となる。

`BackupFileKeyBuilderTest#buildsFileManagerVisibleSystemBackupKey()` がこのルールを保証する。

---

## 15. 書類管理との関係

`BackupFileKeyBuilder` は独自rootを直接組み立てず:

```text
DocumentStorageKeyResolver.resolve(
  DocumentArea.BACKUPS,
  relativePath
)
```

を使う。

**確定仕様**: バックアップStorageは書類管理共通の `BACKUPS` 領域配下に置かれる。

画面表示の「書類管理のバックアップ → system → テナント配下」という説明と実装が一致する。

---

## 16. Tenant key

`BackupFileKeyBuilder` は `TenantContext.getTenantId()` を必須とする。

relative path:

```text
system/{tenantId}/...
```

**確定仕様**: Storage keyにTenant IDが明示的に含まれる。

DB tenant filterだけではなく物理Storage namespaceもTenant分離される。

---

## 17. outputDir

`outputDir` あり:

```text
system/{tenantId}/{outputDir}/{fileName}
```

なし:

```text
system/{tenantId}/{fileName}
```

ただし `SERVER_FILE` / `BOTH` はTarget ValidationでoutputDir必須のため、通常Storage保存ではoutputDirありになる。

---

## 18. outputDir path traversal防御

Target保存時 `BackupTargetValidator#validateRelativeDirectory()` が:

- 最大500文字
- absolute path不可
- 空segment不可
- `.` / `..` 不可
- control char不可

を検証する。

さらに `DocumentStorageKeyResolver` でも全segmentに対して:

- `.` / `..` 不可
- ISO control char不可

を再検証する。

**確定仕様**: outputDirはValidation層とStorage Key Resolver層の二段階でpath traversal防御される。

---

## 19. DocumentStorageKeyResolver

Area rootは `StorageProperties.Document` から解決する。

```text
rootPath
+ area path
```

BACKUPSなら `document.backupsPath`。

relativePathはslash正規化後、segment単位でvalidateしてjoinする。

先頭/末尾slashは除去される。

---

## 20. ZIP Storage保存条件

ZIP化した場合、すべてのTargetがStorage保存対象とは限らない。

`saveZipIfNeeded()` は:

```text
files
→ outputModeがSERVER_FILE/BOTHのものだけ抽出
```

する。

1件以上あればZIP全体をStorageへ保存する。

### 例

```text
Target A = DOWNLOAD
Target B = BOTH
```

複数対象なのでZIP。

Storage保存対象がBに存在するため、最終ZIP全体がStorageへ保存される。

**実装事実**: Storage保存対象ではないAのCSVもZIP内に含まれるため、結果としてAデータも保存ZIP内へ入る。

**未決事項**: mixed OutputMode時にこの挙動を正式仕様とするか要確認。

今回は修正しない。

---

## 21. ZIP outputDir

Storage保存対象ファイルについて:

```text
outputDir Setを作成
```

### 0種類

error:

```text
ZIP保存先 outputDir が設定されていません。
```

### 1種類

そのoutputDirを採用。

### 2種類以上

error:

```text
同時にZIP保存する対象の outputDir は統一してください。
```

**確定仕様**: 1回のZIP保存ではStorage保存対象TargetのoutputDirは統一必須。

---

## 22. Local Storage

`LocalStorageService` は `project.storage.localBasePath` を基準にkeyをPath resolveする。

save:

```text
resolvePath(key)
→ parent directory create
→ Files.copy(..., REPLACE_EXISTING)
```

### path traversal防御

```text
basePath.resolve(key).normalize()
```

後:

```text
resolvedPath.startsWith(basePath)
```

を確認する。

外へ出るkeyは拒否。

**確定仕様**: Local backendでもStorage root外へのescapeを防止する。

---

## 23. S3 Storage

`S3StorageService` は:

```java
@ConditionalOnProperty(
  prefix="project.storage.s3",
  name="enabled",
  havingValue="true"
)
```

で有効化される。

save:

```text
PutObjectRequest
bucket = configured bucket
key    = Storage key
contentType = 指定値
```

を使用する。

**確定仕様**: Backup機能側はS3 bucket名を知らない。共通Storage設定がbucketを決める。

---

## 24. LocalとS3のkey差

Backup機能から渡すkeyは同じ。

例:

```text
documents/backups/system/tenant-a/master-data/xxx.csv
```

Local:

```text
{localBasePath}/documents/backups/system/tenant-a/...
```

S3:

```text
s3://{bucket}/documents/backups/system/tenant-a/...
```

**確定仕様**: 論理Storage keyはBackend種類に依存しない。

---

## 25. Storage type選択

`BackupFileStorageService#save()` はStorageTypeを明示指定せず:

```text
storageService.save(key, ...)
```

を呼ぶ。

したがって保存先は:

```text
StorageProperties.resolveDefaultType()
```

で決まる。

保存後 `BackupStoredFile.storageType` にその時点のdefault typeを記録する。

再DL時は履歴に保存されたtypeを使うため、後でDefault typeが変わっても元Backendから読む。

---

## 26. 保存時上書き

### Local

`Files.copy(..., REPLACE_EXISTING)`。

### S3

同じkeyへのPutObjectは上書き。

ただしfilenameにmillisecond + UUID 8文字が入るため通常衝突しにくい。

**確定仕様**: 一意filenameで衝突回避するが、Storage backend自体は同一key上書きを許容する。

---

## 27. cleanup

`BackupExecutionService` で実行途中に例外が起き、`BackupExecutionResult.storedFile` が存在する場合:

```text
BackupFileStorageService#delete(storedFile)
→ StorageService.delete(storageType, fileKey)
```

を呼ぶ。

### Local

`Files.deleteIfExists()`。

### S3

`DeleteObject`。

cleanup失敗は握りつぶし、元例外を優先する。

**確定仕様**: cleanup対象は最終結果として保存済みの単一CSVまたはZIP 1ファイル。

---

## 28. ZIP生成中のメモリ

各CSVは `byte[]`。

ZIPも `ByteArrayOutputStream` で全体を `byte[]` 化する。

その後Storage保存とHTTP返却の両方で同じbyte[]を利用する。

**実装事実**: 複数大容量CSVでは、個別CSV byte[] + ZIP byte[]を同時にheap上へ保持する時間帯がある。

**V2候補**: stream zip + stream upload + stream response。

今回は修正しない。

---

## 29. SERVER_FILEの名称と実装

画面ラベル:

```text
ストレージ保存のみ
```

しかしBackend Controllerは常に生成データをHTTP bodyへ返す。

さらに `useBackupPage#executeBackup()` はMutation結果をBlobとして `downloadBlob()` する。

このためコード経路だけを見ると `SERVER_FILE` でも利用者端末へダウンロードされる可能性が高い。

**既知事項**: UIラベルとコード挙動が不一致の可能性。

重要度: 高

今回は修正しない。

---

## 30. mixed OutputMode

複数Targetを一括出力できるため、TargetごとにOutputModeが異なるケースが存在する。

例:

```text
A = DOWNLOAD
B = SERVER_FILE
C = BOTH
```

現行構造では最終ZIPは1つ。

B/Cが1つでもあれば最終ZIPをStorage保存する。

そのZIPにはA/B/Cすべてのデータが入る。

**未決事項**: OutputModeをTarget単位で持つのに、複数Target実行時の最終成果物単位で保存されるため、概念上の粒度に差がある。

**V2候補**:

- 実行Request単位でOutputModeを持つ
- mixed mode禁止
- 各Target個別Storage保存 + ZIPはdownload用のみ

などの整理候補。

今回は修正しない。

---

## 31. 現在のテスト保証

### BackupFileNameBuilderTest

保証:

- Asia/Tokyo Clockで年跨ぎfilenameが日本時間になる
- CSV filename format
- ZIP filename format
- UUID先頭8文字付き

### BackupFileKeyBuilderTest

保証:

- BACKUPS領域 + `system/{tenantId}/{outputDir}/{fileName}`
- 書類管理から見えるkey構造
- filenameにpathを含む場合拒否

---

## 32. 不足しているテスト

今回確認範囲で不足:

- `BackupOutputResolver#shouldZip` 全分岐
- zipOutput true/false/null
- single zipRequired
- mixed OutputMode
- ZIP outputDir不一致
- `BackupExecutionResultBuilder` Storage保存分岐
- `BackupZipService` Zip Slip対策
- ZIP内entry名
- `DocumentStorageKeyResolver`とのintegration
- outputDir path traversal
- Tenant ID欠落
- LocalStorage root escape
- S3 save/delete integration
- Default Storage切替
- cleanup成功/失敗
- SERVER_FILEでFrontend downloadされるかのE2E

---

## 33. 今回見つかった既知事項

### A. SERVER_FILEでもHTTPダウンロードされる可能性

重要度: 高

画面名称「ストレージ保存のみ」とコード挙動が不一致の可能性。

修正しない。

### B. mixed OutputModeでDOWNLOAD対象もStorage ZIPへ入る

重要度: 高

一括ZIPが保存単位になるため。

修正しない。

### C. OutputModeの粒度がTarget、成果物はRequest単位

重要度: 設計課題

複数Target一括出力時に意味が曖昧になる。

修正しない。

### D. ZIP/CSV全体をheap保持

重要度: 中

大容量時メモリ問題。

修正しない。

### E. Storage keyのTenant分離は明確

重要度: セキュリティ上の確認済み事項

`BACKUPS/system/{tenantId}/...`。

---

## 34. 次に掘る範囲

次は `system/backup` の **実行Validation・SQL生成・データ取得・CSVフォーマット** に限定する。

```text
BackupExecutionValidator
→ BackupDefinitionService
→ BackupExportColumnResolver
→ BackupSchemaInspector
→ BackupSqlBuilder
→ BackupDataFetcher
→ BackupCsvDataBuilder
→ CsvFileWriter
```

ここで以下を確定する。

- targetCodes null/empty/duplicate
- 無効Target
- SQL identifier防御
- tenant_id WHERE条件
- SELECT順序
- ORDER BY有無
- null / date / datetime / decimal / boolean CSV表現
- BOM・quote・改行
- DBデータ量とTransaction
- テスト保証
