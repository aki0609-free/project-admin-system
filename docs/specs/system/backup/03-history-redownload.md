# system/backup 詳細設計 03 — 履歴一覧・履歴ファイル再ダウンロード

## 1. 対象範囲

この文書は `system/backup` のうち、**履歴一覧と履歴ファイル再ダウンロード** だけを対象とする。

対象経路:

```text
BackupPage.vue
→ useBackupHistoriesQuery.ts
→ BackupHistoryTable.vue
→ useDownloadBackupHistoryFileMutation.ts
→ BackupController#findHistories / downloadHistoryFile
→ BackupHistoryService
→ BackupHistoryLookupService
→ BackupHistoryFileDownloadService
→ BackupHistoryRepository
→ StorageService
→ BackupHistory Entity
```

基準コードは `main` / `12c91a72b409df16b9d4be0b416247a07a8f170a`。

**今回もアプリケーションコードは変更しない。** 不整合・改善候補は記録のみとする。

---

## 2. 履歴タブ

`BackupPage.vue` は `targets` / `histories` の2タブを持つ。

履歴タブでは:

```text
useBackupHistoriesQuery()
→ historiesQuery.histories
→ BackupHistoryTable
```

の順で表示する。

`useBackupHistoriesQuery()` は:

```http
GET /api/system/backup/histories
```

を実行する。

Query Keyは `queryKeys.backup.histories`。

バックアップ実行成功後、`useExecuteBackupMutation` がこのQueryをinvalidateするため、履歴の再取得対象になる。

---

## 3. Backend履歴一覧API

`BackupController#findHistories()`:

```text
GET /api/system/backup/histories
→ BackupHistoryService#findAll()
```

Controller全体に:

```java
@PreAuthorize("hasRole('SYS_ADMIN')")
```

があるため、履歴閲覧も `SYS_ADMIN` が必要。

---

## 4. 履歴取得件数

`BackupHistoryService#findAll()` は:

```text
BackupHistoryRepository
.findTop200ByTenantIdAndDeletedAtIsNullOrderByExecutedAtDesc(tenantId)
```

を使う。

**確定仕様**:

- 現在Tenantのみ
- `deletedAt IS NULL`
- `executedAt DESC`
- 最大200件

ページングAPIではなく固定上限200件。

---

## 5. Tenant境界

履歴一覧:

```text
TenantContext.getTenantId()
→ repository.findTop200ByTenantId...
```

履歴再DL:

```text
BackupHistoryLookupService#find(id)
→ repository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
```

**確定仕様**: 別TenantのhistoryIdを指定してもLookupできない。

履歴再DLはStorage keyだけで直接読まず、必ずTenant scopedな履歴Entityを取得してからStorageへアクセスする。

---

## 6. BackupHistory Entity

Table:

```text
backup_history
```

主要項目:

- `id`
- `tenant_id`
- `target_codes`
- `file_name`
- `content_type`
- `file_size`
- `zip_output`
- `storage_type`
- `stored_file_key`
- `stored_file_name`
- `status`
- `executed_by`
- `executed_at`
- `error_message`
- `created_at`
- `updated_at`
- `deleted_at`

Index:

```text
(tenant_id, status)
(tenant_id, executed_at)
storage_type
```

---

## 7. 成功履歴の生成

`BackupHistoryBuilder#buildSuccess()` が生成する。

記録項目:

### 常に記録

- targetCodes
- fileName
- contentType
- fileSize
- zipOutput
- status = SUCCESS
- executedBy
- executedAt
- errorMessage = null

### Storage保存された場合のみ

- storageType
- storedFileKey
- storedFileName

Storage保存情報が存在する場合、contentType / fileSizeはStoredFile側の値で上書きされる。

---

## 8. 失敗履歴

`BackupHistoryBuilder#buildFailure()`:

- targetCodes
- status = FAILED
- executedBy
- executedAt
- errorMessage

を保存する。

fileName / contentType / fileSize / Storage情報は通常設定されない。

### errorMessage

最大4000文字。

超過時は先頭4000文字へtruncate。

**確定仕様**: Throwable stack trace全体は履歴DBへ保存せず、Exception messageのみを保存する。

---

## 9. executedBy

`BackupHistoryBuilder#currentUsername()` は:

```text
SecurityContextHolder
→ Authentication#getName()
```

を利用する。

Authenticationがない、usernameがnull/blankの場合:

```text
system
```

を記録する。

**実装事実**: 手動実行だけでなく、将来batch/scheduler等の非ユーザー実行にも対応できる形になっている。

---

## 10. executedAt

`BackupHistoryBuilder` はDIされた `Clock` を使用して:

```java
Instant.now(clock)
```

を設定する。

したがって履歴時刻はテスト可能なClock依存。

---

## 11. 履歴一覧の画面項目

`BackupHistoryTable.vue` で以下を表示する。

- 操作
- ID
- 対象
- ファイル名
- 状態
- 形式
- サイズ
- 保存先
- 保存名
- 保存キー
- 実行者
- 実行日時
- エラー

### 状態表示

- SUCCESS → success color
- FAILED → error color

### 形式

- `zipOutput=true` → ZIP
- false → CSV

### ファイルサイズ

- < 1024 → B
- < 1MB → KB 四捨五入
- それ以上 → MB 四捨五入

---

## 12. 再DLボタン表示条件

Frontendで:

```text
downloadable =
  status === 'SUCCESS'
  && !!storedFileKey
```

**確定仕様（画面上）**: 成功履歴でも `storedFileKey` がない場合、再DLボタンは表示しない。

---

## 13. DOWNLOADのみ実行した履歴

`outputMode=DOWNLOAD` の場合、`BackupExecutionResultBuilder` はStorage保存を行わない。

その結果成功履歴には:

```text
storageType = null
storedFileKey = null
storedFileName = null
```

となる。

したがって:

**確定仕様**: `DOWNLOAD` のみで実行した成功履歴は、履歴画面から再ダウンロードできない。

実行直後のHTTPレスポンスでのみ取得できる。

---

## 14. SERVER_FILE / BOTH

Storage保存が行われた場合、成功履歴に:

- StorageType
- fileKey
- storedFileName

が記録される。

この場合Frontendは再DLボタンを表示する。

---

## 15. 再DL Frontend処理

`BackupHistoryTable#downloadHistoryFile(row)`:

```text
if !downloadable → return
if mutation pending → return

useDownloadBackupHistoryFileMutation#mutateAsync(historyId)
→ getBlob('/api/system/backup/histories/{id}/file')
→ downloadBlob(blob, fileName)
```

失敗時:

```text
console.error(error)
alert('バックアップファイルの再ダウンロードに失敗しました。')
```

---

## 16. 再DL API

```http
GET /api/system/backup/histories/{historyId}/file
```

`BackupController#downloadHistoryFile()`:

```text
BackupHistoryFileDownloadService#download(historyId)
→ BackupHistoryFileDownloadResult
→ attachment response
```

Controller共通の `toDownloadResponse()` を使う。

Response:

- `200 OK`
- `Content-Disposition: attachment`
- `Content-Type`
- `byte[]`

---

## 17. Backend再DL Validation

`BackupHistoryFileDownloadService#validate()` が以下を確認する。

### 17.1 status

```text
SUCCESSのみ
```

FAILEDは拒否。

### 17.2 storageType

null不可。

### 17.3 storedFileKey

null / blank不可。

### 17.4 実ファイル存在

```text
storageService.exists(storageType, storedFileKey)
```

で確認。

存在しなければ:

```text
保存済みバックアップファイルが存在しません。 key=...
```

で失敗。

**確定仕様**: Frontend表示条件だけに依存せず、Backendでも再DL可能性を再検証する。

---

## 18. Storage読込

Validation後:

```text
StorageService.load(
  history.storageType,
  history.storedFileKey
)
```

を呼ぶ。

`StorageService` は `StorageType` から対応する `StorageBackend` を選ぶ。

したがって履歴作成時のDefault Storageが現在と変わっていても、履歴に記録された `storageType` を使う。

例:

```text
過去: LOCAL
現在Default: S3
```

でも履歴がLOCALならLOCAL backendから読む。

**確定仕様**: 再DLは「現在のdefault storage」ではなく「履歴に記録したstorageType」が基準。

---

## 19. Storage Backendが無い場合

`StorageService#backend(storageType)` は、該当BackendがDI登録されていなければ:

```text
IllegalStateException
```

を投げる。

したがって過去履歴のStorageTypeに対応するBackendを環境から削除すると再DL不能になる。

**V2候補**: Storage移行時のhistory migration / alias / compatibility policyを定義する。

今回は修正しない。

---

## 20. ファイル消失時

履歴DBにstoredFileKeyが残っていても、Storage実体が削除されている場合:

```text
storageService.exists() == false
```

となり再DLを拒否する。

### Frontendとの不一致

Frontendの `downloadable` は:

```text
SUCCESS && storedFileKeyあり
```

だけで判定するため、Storage実体消失は画面表示時には判定できない。

そのため再DLボタンは表示されるが、クリック後Backendで失敗するケースがある。

**実装事実**: Storage存在状態は履歴一覧APIでは確認していない。

**V2候補**: 一覧に `fileAvailable` を付けるか、Storage healthを別途点検する。

今回は修正しない。

---

## 21. 再DL時のメモリ使用

`BackupHistoryFileDownloadService` は:

```java
inputStream.readAllBytes()
```

で全ファイルをbyte[]化する。

Controllerも `ResponseEntity<byte[]>`。

**実装事実**: 再DL対象ファイル全体をJVM heapに載せる。

大容量バックアップではメモリ負荷が高い。

**V2候補**: StreamingResponseBody等のstreaming download。

今回は修正しない。

---

## 22. 再DLファイル名

Backend側:

優先順位:

```text
1. storedFileName
2. fileName
3. backup.dat
```

Content-Type:

```text
1. history.contentType
2. application/octet-stream
```

### Frontend fallback

画面側でも:

```text
storedFileName
→ fileName
→ backup_{id}.zip / backup_{id}.csv
```

というfallbackを持つ。

ただし `getBlob()` がContent-Disposition filenameを返す形ではなくBlobのみ返しているため、再DL時の最終ローカルファイル名はFrontend `resolveDownloadFileName()` に依存する。

**実装事実**: BackendがContent-Dispositionを設定していても、再DLFrontendはHTTP header由来のファイル名を利用していない。

---

## 23. 初回ダウンロードとの違い

初回実行:

`postBlobDownload()` がBlobとfilenameを扱い、Backend `Content-Disposition` のfilenameを利用する経路。

履歴再DL:

`getBlob()` はBlobのみ取得し、画面が履歴データからfilenameを再構築する。

**実装事実**: 初回DLと再DLでFrontendのdownload API abstractionが異なる。

**V2候補**: Blob download共通化。

今回は修正しない。

---

## 24. 履歴削除機能

今回確認したControllerには:

- 履歴一覧
- 履歴ファイル再DL

のみ存在し、履歴削除APIはない。

Frontendにも履歴削除操作はない。

**実装事実**: system/backup画面からBackupHistoryを削除する機能は存在しない。

---

## 25. 保持期間・Retention

RepositoryはTop200表示に制限するが、これは**表示件数制限**でありDB retentionではない。

今回 `BackupHistory` に対する:

- 定期削除
- retention days
- purge batch
- max rows cleanup

に該当する実装は確認できなかった。

**実装事実**: 少なくともsystem/backup機能内には履歴自動削除ロジックがない。

**未決事項**: DB外の運用SQL・別batch・AWS lifecycle等で消しているかは、この機能コードだけでは確定できない。

---

## 26. StorageファイルRetention

`BackupFileStorageService` は保存・失敗時cleanupを行うが、成功済みバックアップファイルの期限削除処理は持たない。

**未決事項**:

- S3 lifecycleで削除するか
- Local storage cleanupがあるか
- 永久保存か

は別途Infrastructure/運用設定確認が必要。

もしStorage側だけ期限削除され、履歴DBが残れば、前述の「再DLボタンはあるが実ファイルなし」が発生する。

---

## 27. 履歴のsoft delete

`BackupHistory` は `BaseEntity` を継承し `deletedAt` を持つ。

Repositoryも `DeletedAtIsNull` 条件を使う。

ただしsystem/backup内にはBackupHistoryへdeletedAtを設定するService/APIは確認できない。

**実装事実**: soft delete対応のEntity/Query構造はあるが、この画面機能から削除する経路はない。

---

## 28. 履歴200件制限の意味

200件を超えた古い履歴もDBから削除されるわけではない。

画面から見えなくなるだけ。

したがって古い履歴IDを直接知っていれば、`findByIdAndTenantIdAndDeletedAtIsNull()` で取得できるため、API上は再DLできる可能性がある。

**実装事実**: 「一覧に表示される200件」と「再DL可能な履歴範囲」は厳密には同一ではない。

**未決事項**: 200件より古い履歴をAPIから再DLできることを正式仕様とするか要確認。

---

## 29. errorMessage表示

失敗履歴は `errorMessage` を画面にそのまま表示する。

BackendはException messageを保存するため、内部情報を含むmessageが生成された場合、SYS_ADMIN画面に表示される。

権限はSYS_ADMIN限定だが、SQL/Storage path等の内部詳細が見える可能性がある。

**V2候補**: user-facing error messageとinternal diagnostic messageの分離。

今回は修正しない。

---

## 30. 現在のテスト

### BackupHistoryBuilderTest

存在する。

このBuilderの:

- 成功履歴構築
- 失敗履歴構築
- Clock/実行者/Storage情報等

の一部を保証していると考えられるため、詳細ケースは次のテスト総括時に精査する。

### 今回確認範囲で不足しているもの

- `BackupHistoryService#findAll` Top200 + tenant integration
- `BackupHistoryLookupService` 別Tenant拒否
- `BackupHistoryFileDownloadService` SUCCESS validation
- FAILED再DL拒否
- storageType null
- storedFileKey null
- Storage exists=false
- Storage load失敗
- LOCAL/S3 backend選択
- 再DL fileName fallback
- Content-Type fallback
- Controller Role/API contract
- Frontend downloadable判定
- Frontend再DL成功/失敗
- 200件より古い履歴の再DL挙動
- retention / purge policy

---

## 31. 今回見つかった既知事項

### A. DOWNLOADのみの成功履歴は再DL不可

重要度: 仕様確認

Storage保存情報がないため再DLボタンが出ない。

修正しない。

### B. Storage実体消失を一覧では判定しない

重要度: 中

ボタン表示後にBackendで404相当ではなくRuntimeExceptionとなる可能性。

修正しない。

### C. 履歴Retentionがsystem/backup内にない

重要度: 中〜高

DB/Storage双方の長期容量に影響。

修正しない。

### D. Top200は表示制限だけ

重要度: 中

古い履歴がDBに残り続ける可能性。

修正しない。

### E. 再DLはreadAllBytes

重要度: 中

大容量時heap負荷。

修正しない。

### F. 初回DLと再DLでfilename取得方式が異なる

重要度: 低〜中

初回はHTTP Header、再DLは履歴項目ベース。

修正しない。

---

## 32. 次に掘る範囲

次は `system/backup` の **ファイル生成・OutputMode・Storage key/filenameルール** に限定する。

```text
BackupOutputResolver
→ BackupExecutionResultBuilder
→ BackupZipService
→ BackupFileNameBuilder
→ BackupFileKeyBuilder
→ BackupFileStorageService
→ StorageService
→ LocalStorage / S3Storage
```

ここで以下を確定する。

- 1件CSV / 1件ZIP / 複数ZIPの条件
- SERVER_FILE / BOTH / DOWNLOAD
- ZIP時のoutputDir決定
- fileNamePattern placeholder
- timestamp timezone
- Storage keyにtenantがどう入るか
- path traversal防御
- LocalとS3の差
- 保存先と書類管理の関係
- cleanup時削除対象
- テスト保証
