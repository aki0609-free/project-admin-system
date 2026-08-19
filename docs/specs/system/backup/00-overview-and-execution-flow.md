# system/backup 詳細設計 00 — 概要・バックアップ実行フロー

## 1. 調査基準

- 対象: `system/backup`
- 基準コード: `main` / `12c91a72b409df16b9d4be0b416247a07a8f170a`
- 設計書作成ブランチ: `agent/v1-common-architecture-spec`
- アプリケーションコード変更: なし
- 今回の範囲: 画面の入口、主要構成、バックアップ実行処理、履歴保存、Storage保存、主要テスト
- 次回以降: バックアップ対象CRUD、列定義編集、履歴再ダウンロード、Validation/Schema判定を個別に掘る

## 2. 記載区分

- **確定仕様**: 現行コード、関連設定、テストから一貫して確認できる事項。
- **実装事実**: 現在コードが実際に行っている処理。業務要件として保証されているかは別。
- **推測**: 命名や構成から読み取れるがコードだけでは確定できない事項。
- **未決事項**: コードから意図を一意に決められない事項。
- **V2候補**: V1を修正せず、今後改善候補として記録する事項。

---

## 3. 機能目的

**実装事実**: `system/backup` は、管理者が登録したバックアップ対象テーブルについて、指定列をDBから読み出してCSVを生成し、複数対象の場合はZIPにまとめ、ブラウザへダウンロードさせる機能である。

また対象設定によっては、生成ファイルを共通 `StorageService` にも保存し、実行結果を `BackupHistory` としてDBへ記録する。

Frontend画面の説明文は `BackupPage.vue` に「対象テーブルのCSV/ZIP出力と実行履歴を管理します。」と定義されている。

---

## 4. 主要ファイル

### 4.1 Frontend

| 役割 | パス | 主要要素 |
|---|---|---|
| Page | `frontend/src/features/system/backup/page/BackupPage.vue` | `BackupPage` |
| Pageロジック | `frontend/src/features/system/backup/composables/useBackupPage.ts` | `useBackupPage()` |
| 実行API | `frontend/src/features/system/backup/api/useExecuteBackupMutation.ts` | `useExecuteBackupMutation()` |
| 対象一覧 | `frontend/src/features/system/backup/components/BackupTargetTable.vue` | `BackupTargetTable` |
| 対象編集 | `frontend/src/features/system/backup/components/BackupTargetEditDialog.vue` | `BackupTargetEditDialog` |
| 履歴一覧 | `frontend/src/features/system/backup/components/BackupHistoryTable.vue` | `BackupHistoryTable` |
| Blob保存 | `frontend/src/features/system/backup/utils/downloadBlob.ts` | `downloadBlob()` |
| 共通HTTP | `frontend/src/shared/api/http.ts` | `postBlobDownload()` |

### 4.2 Backend

| 役割 | パス | 主要クラス/関数 |
|---|---|---|
| Controller | `backend/src/main/java/com/project/backend/features/system/backup/controller/BackupController.java` | `BackupController#execute` |
| 実行オーケストレーション | `.../service/BackupExecutionService.java` | `execute()`, `buildFiles()`, `buildResult()` |
| 1対象CSV生成 | `.../service/builder/BackupSingleFileBuilder.java` | `build()` |
| SQL構築 | `.../service/builder/BackupSqlBuilder.java` | `buildSelectSql()`, `buildParameters()` |
| CSV生成 | `backend/src/main/java/com/project/backend/app/file/service/CsvFileWriter.java` | `write()` |
| 単体/ZIP結果構築 | `.../service/builder/BackupExecutionResultBuilder.java` | `buildSingle()`, `buildZip()` |
| ファイル保存 | `.../service/BackupFileStorageService.java` | `save()`, `delete()` |
| 共通Storage | `backend/src/main/java/com/project/backend/app/storage/service/StorageService.java` | `save()`, `delete()` 等 |
| 履歴保存・一覧 | `.../service/BackupHistoryService.java` | `saveSuccess()`, `saveFailure()`, `findAll()` |
| 履歴Entity | `.../entity/BackupHistory.java` | `BackupHistory` |
| 対象Entity | `.../entity/BackupTarget.java` | `BackupTarget` |
| 列Entity | `.../entity/BackupColumn.java` | `BackupColumn` |

---

## 5. 画面構成

`BackupPage.vue` は2タブ構成。

1. `targets`: バックアップ対象
2. `histories`: 履歴

### バックアップ対象タブ

`GenericToolbar` と `BackupTargetTable` を表示する。

Toolbarは `useBackupPage()` が以下を生成する。

- `新規追加`
- `全選択 / 全解除`
- `選択した N 件を出力`

対象行クリック時は `openEditDialog(target)` を呼び、対象詳細を取得して編集ダイアログを開く。

### 履歴タブ

`useBackupHistoriesQuery()` で取得した履歴を `BackupHistoryTable` に渡す。

**実装事実**: バックアップ実行成功後、`useBackupPage#executeBackup()` は選択を解除し、`activeTab` が渡されている場合は自動的に `histories` タブへ切り替える。

---

## 6. バックアップ実行シーケンス

### 6.1 Frontend

```text
BackupPage.vue
  ↓ Toolbar「選択した N 件を出力」
useBackupPage#executeBackup()
  ↓
useExecuteBackupMutation#mutateAsync()
  ↓
postBlobDownload('/api/system/backup/execute', request)
  ↓
POST /api/system/backup/execute
```

Requestは `BackupExecuteRequest` で、Frontendから選択済み `targetCodes` を送信する。

対象未選択の場合はAPIを呼ばず、Frontendで `alert('バックアップ対象を1件以上選択してください。')` を表示する。

成功時はHTTP `Content-Disposition` のファイル名を優先し、取得できない場合のみFrontendでfallback名を作る。

fallback:

- 1対象: `{targetCode}_{timestamp}.csv`
- 複数対象: `backup_{timestamp}.zip`

その後 `downloadBlob()` でブラウザダウンロードを開始する。

### 6.2 Controller

`BackupController` はクラス単位で次の権限制御を持つ。

```java
@PreAuthorize("hasRole('SYS_ADMIN')")
```

したがって `/api/system/backup/**` はSpring Method Security上 `SYS_ADMIN` Roleが必要。

`BackupController#execute()` は `request.targetCodes()` を `BackupExecutionService#execute()` へ渡す。

Service結果の以下3項目をHTTPダウンロードレスポンスへ変換する。

- `fileName`
- `contentType`
- `data`

レスポンスは `200 OK` + `Content-Disposition: attachment`。

---

## 7. Backend実行処理

### 7.1 `BackupExecutionService#execute(List<String>)`

Controller経由ではまず以下の `BackupRequest` に変換される。

- `targetCodes`: Frontend指定値
- `encoding`: `UTF-8`
- `zipOutput`: `null`

続いて `execute(BackupRequest)` へ委譲。

### 7.2 `BackupExecutionService#execute(BackupRequest)`

処理順は以下。

```text
1. BackupExecutionValidator.validate(request)
2. targetCodesごとに BackupSingleFileBuilder.build(targetCode)
3. CSVファイル群を生成
4. BackupOutputResolverで単体CSV/ZIPを決定
5. BackupExecutionResultBuilderで最終ファイルを生成
6. 必要な場合Storageへ保存
7. BackupHistoryService.saveSuccess()
8. 生成byte[]をControllerへ返却
```

例外発生時は以下。

```text
catch Exception
  ├─ cleanupStoredFile(result)
  │    └─ Storageへ保存済みなら削除を試行
  ├─ saveFailureHistory(request, e)
  │    └─ 失敗履歴保存を試行
  └─ 元例外を再throw
```

**確定仕様**: cleanupまたは失敗履歴保存自身が失敗しても、その例外は握りつぶし、元のバックアップ例外を優先する。

コードコメント上、Storage削除失敗による孤立ファイルは「S3運用点検で検知する」前提になっている。

---

## 8. 1対象分のCSV生成

中心は `BackupSingleFileBuilder#build(String targetCode)`。

### 8.1 定義取得

`BackupDefinitionService#getBackupTargetDefinition(targetCode)` からバックアップ対象設定を取得する。

対象定義には少なくとも以下が含まれる。

- targetCode
- targetName
- tableName
- fileNamePattern
- includeHeader
- zipRequired
- outputMode
- outputDir
- columns

### 8.2 出力列決定

`BackupExportColumnResolver#resolve(target)` により実際にCSVへ出力する `BackupColumnDefinition` 一覧を確定する。

### 8.3 DBスキーマ判定

`BackupSchemaInspector#inspect(target.tableName())` が対象テーブルのスキーマ情報を確認し、`BackupSourceSchema#tenantScoped()` を返す。

**重要**: テナント条件を付けるかどうかは、BackupTarget設定だけではなく実テーブルのSchema検査結果を用いて決める。

### 8.4 SELECT SQL生成

`BackupSqlBuilder#buildSelectSql()` がSQLを組み立てる。

形式:

```sql
SELECT col1, col2, ... FROM table_name
```

テナント対象テーブルの場合:

```sql
SELECT col1, col2, ... FROM table_name WHERE tenant_id = :tenantId
```

テーブル名・列名は `^[a-zA-Z0-9_]+$` で検証される。

**確定仕様**: SQLのテーブル名・列名はbind parameter化できないためidentifier検証を行い、tenantIdだけNamed Parameterとして渡す。

### 8.5 テナント値

テナント対象テーブルでは `TenantContext.getTenantId()` を `:tenantId` にbindする。

tenantIdがnull/blankなら `BackupSqlBuilder#buildParameters()` はRuntimeExceptionを投げる。

### 8.6 データ取得

`BackupDataFetcher#fetch(sql, parameters)` がSELECTを実行し、`List<Map<String,Object>>` を返す。

### 8.7 CSV化

取得データを `BackupCsvDataBuilder` でCSV向けデータへ変換し、共通 `CsvFileWriter#write()` へ渡す。

列順は `BackupColumnDefinition` 一覧の順。

- `columnName` → データキー
- `csvHeaderName` → CSVヘッダ名
- `target.includeHeader()` → ヘッダ有無

### 8.8 SingleBackupFile生成

最後に以下を保持する `SingleBackupFile` を作る。

- targetCode
- targetName
- fileName
- data
- zipRequired
- outputMode
- outputDir

---

## 9. CSVかZIPか

`BackupExecutionService#buildResult()` → `BackupOutputResolver#shouldZip()` が判断する。

- ZIP不要 → `BackupExecutionResultBuilder#buildSingle()`
- ZIP必要 → `BackupExecutionResultBuilder#buildZip()`

**実装事実**: 最終的なZIP判定は単純な「対象件数 > 1」だけではなく `BackupOutputResolver` に集約されている。個々の対象定義が持つ `zipRequired` とRequest指定も考慮される構造である。

この判定ロジックの詳細は次回の個別設計で記載する。

---

## 10. Storage保存

`BackupExecutionResultBuilder` は、返却用byte[]を作るだけでなく `outputMode` に応じてサーバー側Storage保存も行う。

### 単体CSV

`BackupOutputResolver#shouldSaveToServer(file.outputMode())` がtrueの場合のみ:

```text
BackupExecutionResultBuilder
  → BackupFileStorageService.save()
  → BackupFileKeyBuilder.build()
  → StorageService.save()
  → LocalStorageService または S3StorageService
```

### ZIP

複数ファイル中、サーバー保存対象が1件でも存在するとZIP自体をStorageへ保存する。

保存先 `outputDir` は `BackupOutputResolver#resolveZipOutputDir(serverFileTargets)` で決定する。

### Storage抽象化

**確定仕様**: `system/backup` はS3 SDKを直接呼ばない。`BackupFileStorageService` → 共通 `StorageService` を通す。

共通Storageは環境設定によりLOCAL/S3を切り替える。

ローカルDockerでは `PROJECT_STORAGE_DEFAULT_TYPE=LOCAL`。
DEV/AWSでは共通アーキテクチャ調査でS3 document bucketとBackend IAMアクセスを確認済み。

---

## 11. 履歴保存とTransaction

`BackupHistoryService` が履歴永続化を担当する。

### 成功

`BackupHistoryService#saveSuccess()`

```java
@Transactional(propagation = Propagation.REQUIRES_NEW)
```

### 失敗

`BackupHistoryService#saveFailure()`

```java
@Transactional(propagation = Propagation.REQUIRES_NEW)
```

**確定仕様**: 成功・失敗履歴はどちらも呼出元とは独立した新規Transactionで保存される。

これにより、バックアップ本体側で例外が発生した場合でも、失敗履歴を独立Transactionで残そうとする設計になっている。

### 一覧

`BackupHistoryService#findAll()` はreadOnly Transactionで、

`BackupHistoryRepository#findTop200ByTenantIdAndDeletedAtIsNullOrderByExecutedAtDesc()`

を利用する。

**確定仕様**: 画面へ返す履歴は現在テナントの削除されていない直近200件まで。

---

## 12. テナント境界

バックアップデータ本体と履歴でテナント制御方法が異なる。

### バックアップ対象テーブル読取

`BackupSchemaInspector` がテナント対象と判断したテーブルのみ、SQLへ明示的に

```sql
WHERE tenant_id = :tenantId
```

を付加する。

### BackupHistory

履歴一覧ではRepositoryメソッド引数として `TenantContext` のtenantIdを明示指定する。

### 重要な注意

**実装事実**: system/backupはDynamic SQLで対象テーブルを直接読むため、通常JPA Entityに適用されるHibernate `tenantFilter` に依存していない。バックアップ本体のテナント境界は `BackupSchemaInspector` + `BackupSqlBuilder` + `TenantContext` が担う。

このため、この3点はセキュリティ上の重要変更影響ポイントである。

---

## 13. 副作用

バックアップ実行には以下の副作用がある。

1. 対象DBテーブルをSELECT
2. CSV/ZIPをメモリ上で生成
3. outputModeによってLOCAL/S3 Storageへファイル保存
4. `backup_history` 相当Entityへ成功または失敗履歴をINSERT
5. HTTPレスポンスとして同じ生成データをブラウザへ返す
6. Frontendがローカル端末へダウンロード開始
7. 成功後、履歴Queryをinvalidate
8. Frontend選択状態をクリアし履歴タブへ遷移

**実装事実**: DBデータの更新・削除はバックアップ実行そのものでは行わない。ただし履歴DBとStorageは更新する。

---

## 14. 現在存在するテスト

`backend/src/test/java/com/project/backend/features/system/backup/service/` 配下で確認できたテスト:

- `BackupTargetCommandServiceTest`
- `BackupFileKeyBuilderTest`
- `BackupFileNameBuilderTest`
- `BackupHistoryBuilderTest`
- `BackupSqlBuilderTest`
- `BackupTargetValidatorTest`

### 現在テストで保証されている主な層

- BackupTarget更新系Serviceの一部
- Storage file key生成
- 出力ファイル名生成
- 履歴Entity構築
- Dynamic SELECT SQL生成とidentifier/tenant parameter
- BackupTarget Validation

### 今回確認範囲で不足しているテスト

以下の専用テストは `features/system/backup` のテストツリーでは確認できなかった。

- `BackupController` のRole/API contractテスト
- `BackupExecutionService` の成功/失敗/cleanup補償テスト
- `BackupSingleFileBuilder` の統合的テスト
- `BackupExecutionResultBuilder` のCSV/ZIP/Storage分岐テスト
- `BackupHistoryService` の `REQUIRES_NEW` 動作保証テスト
- `BackupHistoryFileDownloadService` テスト
- `BackupSchemaInspector` のtenantScoped判定テスト
- Frontend `useBackupPage` / `useExecuteBackupMutation` のunit test
- ブラウザ実ダウンロードを含むsystem/backup E2Eテスト

**未決事項**: 別ディレクトリの汎用E2Eがこの画面を間接的に通る可能性はあるため、全E2E仕様の精査時に再確認する。

---

## 15. 現時点の重要な確認事項・既知リスク

### 15.1 メモリ使用量

**実装事実**: `BackupDataFetcher` の結果を `List<Map<String,Object>>` として保持し、CSVも `byte[]`、ZIPも `byte[]` で生成し、Controllerも `ResponseEntity<byte[]>` で返す。

したがって大規模テーブルではデータ量に比例してJVM heapを消費する。

**V2候補**: Streaming ResultSet + StreamingResponseBody / multipart upload等への変更検討。

### 15.2 TransactionとStorageは原子的ではない

Storage書込と履歴DB書込は同一Transaction資源ではない。

成功履歴保存に失敗した場合は `BackupExecutionService` のcatchへ入り、保存済みStorage削除を試みる。

ただしStorage削除にも失敗した場合は孤立ファイルが残る。

**確定仕様**: cleanup失敗は元例外を優先して握りつぶす。

### 15.3 Dynamic SQL

identifierはregexで制限されており、tenantIdはbind parameterである。

一方、対象テーブル・列定義自体を管理画面から変更できるため、ValidationとSchema Inspectorは重要な安全境界。

### 15.4 Clock

ファイル名・履歴時刻生成クラスの一部はClock利用有無を次回確認する。
共通アーキテクチャでは `ApplicationTimeConfig` のClockと `Instant.now()` 直呼びが混在していることを確認済み。

---

## 16. この段階で確認したい質問

次に詳細化する前に、業務仕様として以下を確認したい。

1. 「バックアップ対象」は **SYS_ADMINだけが設定・実行する運用**で確定か。
2. `SERVER`系outputModeで保存されるファイルは、障害復旧用の正式バックアップか、それとも管理者が後から再ダウンロードするための履歴保存か。
3. バックアップ対象テーブルは将来的にも管理画面から自由追加する想定か、それとも運用上は事前承認されたテーブルだけ登録するか。
4. 1対象でも `zipRequired=true` の場合ZIPにする挙動を業務仕様として維持するか。

---

## 17. 次に読むコード

次回は範囲を広げず、**「バックアップ対象設定の登録・編集・削除」**だけを詳細化する。

追跡順:

```text
BackupTargetEditDialog.vue
→ useBackupTargetEditDialog.ts
→ useCreate/Update/DeleteBackupTargetMutation.ts
→ BackupController#create/update/delete
→ BackupTargetCommandService
→ BackupTargetValidator
→ BackupTargetMapper
→ BackupTargetRepository / BackupColumnRepository
→ BackupTarget / BackupColumn
```
