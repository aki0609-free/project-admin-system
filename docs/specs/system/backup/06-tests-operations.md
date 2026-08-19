# system/backup 詳細設計 06 — テスト・例外・監査・Clock・CI/運用総括

## 1. 対象範囲

この文書は `system/backup` の横断事項を総括する。

対象:

- テスト
- 例外処理
- 権限・Tenant
- 監査/履歴
- Clock
- Docker Local
- CI
- Storage運用
- AWS/Terraformとの接点
- 既知事項・未決事項

基準コードは `main` / `12c91a72b409df16b9d4be0b416247a07a8f170a`。

**この調査ではアプリケーションコードを修正しない。**

---

## 2. V1 backupの構成総括

`system/backup` は以下を内包する独立性の高いサブシステムである。

```text
管理画面
→ Target/Column定義CRUD
→ Schema照合
→ 実行Validation
→ Dynamic SELECT SQL
→ Tenant Row絞り込み
→ CSV生成
→ ZIP生成
→ Local/S3 Storage
→ 成功/失敗履歴
→ 再ダウンロード
→ SYS_ADMIN権限制御
```

単純なCSV utilityではなく、設定・実行・履歴・Storageまで一連のライフサイクルを持つ。

---

## 3. Backendテスト配置

現行コードで `features/system/backup` 専用テストは以下の構造。

```text
backend/src/test/java/com/project/backend/features/system/backup/
└─ service/
   ├─ BackupTargetCommandServiceTest.java
   ├─ builder/
   │  ├─ BackupFileKeyBuilderTest.java
   │  ├─ BackupFileNameBuilderTest.java
   │  ├─ BackupHistoryBuilderTest.java
   │  └─ BackupSqlBuilderTest.java
   └─ validation/
      └─ BackupTargetValidatorTest.java
```

**実装事実**: backup専用テストは主にunit testであり、Controller→DB→Storageまでを通すbackup専用integration/E2E testはこのディレクトリには存在しない。

---

## 4. 現在テストで保証されている事項

### 4.1 BackupTargetCommandServiceTest

保証:

- soft deleteでTargetへClock由来のInstantを設定
- 子Columnにも同じInstantを設定

### 4.2 BackupFileNameBuilderTest

保証:

- injected ClockのZoneをfilename timestampへ反映
- Asia/Tokyoの年跨ぎ
- CSV filename format
- ZIP filename format
- UUID先頭8文字を含む

### 4.3 BackupFileKeyBuilderTest

保証:

- `DocumentArea.BACKUPS` 配下へ保存
- `system/{tenantId}/{outputDir}/{fileName}` 構造
- filenameにpathを含む入力を拒否

### 4.4 BackupHistoryBuilderTest

保証対象:

- BackupHistory生成処理
- Clockを使ったexecutedAt
- success/failure履歴の組立
- Storage情報を含む成功履歴

詳細なassert内容に依存するため、変更時はテスト本体を同時確認する。

### 4.5 BackupSqlBuilderTest

保証対象:

- SELECT SQL構築
- Tenant scoped時の `WHERE tenant_id = :tenantId`
- tenant parameter
- identifier防御

### 4.6 BackupTargetValidatorTest

確認済み保証:

- 物理DBに存在しないColumnを拒否
- 作成後のtargetCode/tableName変更を拒否
- fileNamePatternの`{timestamp}`必須

---

## 5. テスト不足 — 優先度高

以下はコード上重要だが、backup専用テストで十分保証されていない。

### 実行全体

- `BackupExecutionService` 成功系
- 途中失敗→FAILED履歴
- Storage保存後の履歴保存失敗→cleanup
- cleanup自身失敗時に元例外を優先
- Validation失敗がFAILED履歴に残らない挙動

### Tenant/Security

- 別Tenant Target更新拒否
- 別Tenant History再DL拒否
- Dynamic SQLでtenant_id filterが実DBで効くこと
- SYS_ADMIN以外のController拒否

### Storage

- LOCAL save/load/delete integration
- S3 save/load/delete integration
- Default Storage切替
- mixed OutputMode
- Storage file不存在時の再DL

### CSV/ZIP

- UTF-8 BOM
- quote/comma/newline
- LocalDate/LocalDateTime
- Timestamp/Boolean/Decimal
- NULL→空欄
- 1対象ZIP
- 複数対象ZIP
- Zip Slip防御

### DB/Schema

- `BackupSchemaInspector` JDBC metadata integration
- tenant_id判定
- Schema変更後実行
- soft delete済み業務Rowの出力
- ORDER BYなしによるrow順非保証

### Frontend

- Target CRUD
- Column add/remove/resequence
- 実行選択
- SERVER_FILE挙動
- 履歴再DL
- Storage消失時エラー表示

---

## 6. CIで実行されるテスト

`.github/workflows/ci.yml` は `push` と `pull_request` の両方で起動する。

Backend job:

```text
./gradlew test
./gradlew integrationTest
./gradlew clean bootJar -x test
```

Frontend job:

```text
npm ci
npm audit --omit=dev --audit-level=high
npm run test:unit -- --run --project unit --passWithNoTests
npm run type-check       # continue-on-error
npm run lint:check       # continue-on-error
npm run build-only
```

E2E job:

```text
docker compose up --build --wait
npm run test:e2e
```

Terraform job:

```text
terraform fmt -check -recursive
terraform validate bootstrap
terraform validate environments/dev
```

**確定仕様**: 設計書だけのpushでもCIは起動する。

---

## 7. CIでbackupをどこまで保証するか

CIはBackend unit/integration、Frontend build/unit、Playwright smokeを通す枠組みを持つ。

ただし `system/backup` 専用E2Eが確認できないため、CI greenでも以下は直接保証されない可能性がある。

- 実画面からbackup作成→実行→Storage保存→履歴→再DL
- S3環境での保存/再DL
- 大容量backup
- mixed OutputMode

**未決事項**: 汎用Playwright smoke testがbackup画面を間接的に通っているかはE2E spec全体の別途確認が必要。

---

## 8. Local Dockerでのbackup

`docker-compose.yml` のBackendは:

```text
PROJECT_STORAGE_DEFAULT_TYPE=LOCAL
PROJECT_STORAGE_LOCAL_BASE_PATH=/app/storage
PROJECT_STORAGE_S3_ENABLED=false
TZ=Asia/Tokyo
```

Backend storageはnamed volume:

```text
backend_storage:/app/storage
```

へ永続化する。

**確定仕様**: Local Dockerのbackup StorageはS3ではなくLOCAL。

`docker compose down --volumes` を行えばnamed volumeも削除されるため、CI E2E終了時のLocal backup fileは永続資産ではない。

---

## 9. Local DB

Local Docker:

- MySQL 8.4
- DB: `ADMIN`
- TZ: Asia/Tokyo
- `SPRING_JPA_HIBERNATE_DDL_AUTO=update`
- `SPRING_FLYWAY_ENABLED=false`

さらにBackend起動後 `runtime-schema` serviceが追加SQLを適用する。

**実装事実**: Local Schemaは「JPA update + runtime schema script」の組み合わせで形成される。

BackupSchemaInspectorはその実DB Schemaを直接見るため、Localでのruntime schema差異がbackup設定Validationへ直接影響する。

---

## 10. Clock

backup内でClock DIを明示利用する主要箇所:

- `BackupFileNameBuilder`
- `BackupHistoryBuilder`
- `BackupTargetCommandService#delete`

これらはテスト可能。

一方共通 `BaseEntity` は:

```java
Instant.now()
```

を直接利用する。

したがって同じDB rowでも:

```text
executedAt / deletedAt → injected Clock
createdAt / updatedAt  → system clock直接
```

となりうる。

**既知事項**: Clock戦略が完全統一されていない。

今回は修正しない。

---

## 11. BaseEntityとTenant

`BackupTarget`, `BackupColumn`, `BackupHistory` は `BaseEntity` を継承する。

BaseEntityは:

- tenant_id
- created_at
- updated_at
- deleted_at

を持つ。

PrePersist/PreUpdate時、tenantIdがnullなら `TenantContext` から設定する。

さらにHibernate filter定義:

- `tenantFilter`
- `softDeleteFilter`

を持つ。

ただしbackupの重要Repositoryは明示的にtenantId/deletedAt条件をメソッド名へ含めている。

**実装事実**: backupでは安全境界をHibernate Filterだけへ依存せず、Repository queryにもtenant条件を明示する箇所が多い。

---

## 12. Dynamic SQLはHibernate Filter対象外

`BackupDataFetcher` はJPAではなく `NamedParameterJdbcTemplate` を使う。

したがってBaseEntityのHibernate `tenantFilter` / `softDeleteFilter` はバックアップ対象業務テーブルのSELECTへ自動適用されない。

Tenant境界は:

```text
BackupSchemaInspector
→ tenant_id存在判定
→ BackupSqlBuilder
→ WHERE tenant_id = :tenantId
```

で実現する。

一方 `deleted_at IS NULL` は自動付与されない。

**確定仕様（現行実装）**: tenant_idを持つテーブルではTenant Rowは絞るが、soft delete Rowは除外しない。

---

## 13. 権限

`BackupController` はクラス単位:

```text
hasRole('SYS_ADMIN')
```

対象:

- Target一覧/詳細
- Target作成/更新/削除
- backup実行
- History一覧
- History再DL

**確定仕様**: system/backup APIはSYS_ADMIN専用。

専用Controller security testは不足している。

---

## 14. 監査の意味を分離

backupには2種類の「履歴」がある。

### 14.1 BackupHistory

業務機能としてのバックアップ実行履歴。

記録:

- 対象
- 成否
- 実行者
- 実行時刻
- ファイル情報
- Storage情報
- errorMessage

### 14.2 Entity audit fields

BaseEntityの:

- createdAt
- updatedAt
- deletedAt
- tenantId

これは一般的な作成/更新/論理削除メタデータ。

**実装事実**: BackupHistory自体がバックアップ実行監査の中心であり、少なくとも今回確認範囲ではTarget変更内容のbefore/afterをBackupHistoryへ記録するものではない。

---

## 15. Target設定変更監査

Target/Column CRUDではBackupHistoryを作成しない。

したがってBackupHistoryから分かるのは「どのbackupをいつ実行したか」であり、

- 誰がTarget定義を変更したか
- どのColumnを変更したか
- outputModeを何から何へ変えたか

は分からない。

**未決事項**: 共通監査ログ機構で別途捕捉されているかは、共通audit機構詳細設計側で確認する。

---

## 16. 例外処理 — backup内部

backup内部では多くのValidation/Errorがplain `RuntimeException`。

例:

- Target不存在
- History不存在
- invalid identifier
- Schema不存在
- Column不存在
- Storage file不存在
- ZIP outputDir不一致
- 50件超過

**実装事実**: backupドメイン専用Exception hierarchyは確認できない。

影響:

- UIに返るErrorCode分類が共通例外ハンドラへ依存
- Validation ErrorとSystem Errorの区別が弱い

今回は修正しない。

---

## 17. FAILED履歴に入る例外 / 入らない例外

`BackupExecutionService#execute()` は:

```text
validator.validate(request)
```

をtry-catchより前に実行する。

したがってExecutionValidatorで失敗する:

- null request
- targetCodes empty
- >50件
- blank code
- >100文字
- joined >2000文字
- duplicate

はFAILED BackupHistoryに保存されない。

その後の:

- Target定義不存在
- Schema/SQL/DB取得失敗
- CSV/ZIP生成失敗
- Storage失敗
- success履歴保存失敗

等はcatch対象となり、FAILED履歴保存を試みる。

**確定仕様（実装上）**: 入力Validation失敗と実行途中失敗では履歴の残り方が異なる。

---

## 18. 履歴保存Transaction

成功/失敗履歴とも:

```text
Propagation.REQUIRES_NEW
```

で保存。

バックアップ本体にTransactionがなくても、履歴DB insertは独立Transaction。

**意図として読み取れること**: 実行本体の失敗から履歴保存を独立させる。

---

## 19. DB snapshot整合性

`BackupExecutionService` 自体に `@Transactional` はない。

複数Targetはstream順で1つずつ読み出す。

そのため:

- Target A取得
- 業務DB更新
- Target B取得

が起こりうる。

**実装事実**: 複数テーブルを「完全に同一時点」のDB snapshotとして保証しない。

これはバックアップの用途が「論理export」なのか「災害復旧backup」なのかを決める上で重要。

---

## 20. StorageとDBの原子性

StorageはDB Transaction資源ではない。

概念:

```text
Storage save
→ success history DB insert
```

success history insertに失敗すると、catchでStorage deleteを試みる。

ただしdelete失敗時は孤立fileが残る。

**確定仕様**: 厳密なatomic transactionではなく補償処理方式。

---

## 21. Storage運用

Local:

- `/app/storage`
- Docker named volume

S3:

- 共通StorageService経由
- S3 backend enabled時のみ登録
- bucketはStoragePropertiesが決定

Backup自身はbucket名を直接保持しない。

Historyには:

- storageType
- storedFileKey

を保存するため、再DLは過去のStorage backendを指定して読む。

---

## 22. AWS/Terraformとの関係

共通アーキテクチャ調査で、DEVはEC2上Docker Runtime、RDS MySQL、S3 document storage、ECR、CloudWatch等をTerraform管理する構成を確認済み。

system/backupから見たInfra境界は:

```text
BackupFileStorageService
→ StorageService
→ S3StorageService
→ configured S3 bucket
```

backup機能自身は:

- Terraform resource名
- EC2
- IAM Role
- bucket ARN

を知らない。

**確定設計意図**: Infrastructure依存を共通Storage層で隔離している。

---

## 23. CI/CDとbackup

CIでは:

- unit/integration
- Local Docker E2E
- Terraform validate

を行う。

Deployment workflowは別責務であり、backupコードはDeploymentを直接制御しない。

Local E2EではS3無効のため、**通常CI smokeだけでは本物のS3 backup経路は通らない**。

**重要な未保証領域**: AWS DEVでのS3 IAM/permissionまで含めたbackup動作。

---

## 24. 大容量運用リスク

現在以下が全件メモリ方式。

```text
queryForList → List<Map<String,Object>>
CSV → byte[]
ZIP → byte[]
Storage upload → byte[]由来InputStream
HTTP response → byte[]
再DL → readAllBytes()
```

複数Target ZIPでは個別CSV + ZIPを同時に保持する可能性がある。

**V2候補**:

- JDBC streaming
- CSV streaming
- ZIP streaming
- S3 multipart/stream upload
- StreamingResponseBody

今回は修正しない。

---

## 25. Retention

BackupHistory一覧はTop200だがDB削除ではない。

system/backup内で:

- history retention days
- purge job
- storage lifecycle cleanup

は確認できない。

**未決事項**:

- S3 lifecycle rule
- 運用手順によるcleanup
- DB purge batch

をInfra/運用側で別途確認する必要がある。

---

## 26. 復旧用バックアップとしての評価

**実装事実からの評価**:

現在のsystem/backupは、DBの物理backupよりも「管理者が指定したテーブル/ColumnをCSVとして論理exportする仕組み」に近い。

根拠:

- 全Table自動対象ではない
- Column選択式
- soft delete Rowも含みうる
- Foreign Key/DDL/index/schemaを保存しない
- 複数Table snapshot保証なし
- Restore処理がない

したがってRDS snapshot / mysqldump等のInfra disaster recovery backupとは役割を分けるべき構造。

**未決事項**: 業務上「正式バックアップ」と呼ぶ範囲を確認する。

---

## 27. V1で優先確認すべき仕様質問

1. `system/backup` は災害復旧用ではなく、管理者向けCSV論理exportとして扱ってよいか。
2. `SERVER_FILE` は本当に「保存のみ」か。現行Frontend/Backendではdownloadも起こる可能性がある。
3. mixed OutputModeを許可するか。
4. soft delete済みRowを含めるのが正しいか。
5. CSV Row順は不定でよいか。
6. `tenant_id` をexport可能でよいか。
7. `dataType` は将来何に利用する項目か。
8. BackupHistory / Storage fileの保持期間は何日/何年か。
9. 200件より古い履歴の再DLを許可するか。
10. 大容量Targetの想定最大Row数/ファイルサイズはいくらか。

---

## 28. 修正時の影響範囲一覧

### Tenant判定を変更する場合

- BackupSchemaInspector
- BackupSqlBuilder
- BackupSingleFileBuilder
- BackupTargetValidator
- TenantContext
- security test

### OutputModeを変更する場合

- BackupOutputResolver
- BackupExecutionResultBuilder
- useBackupPage
- BackupTarget UI
- History/再DL
- Storage test

### Column更新方式を変更する場合

- BackupTargetMapper
- BackupColumn Entity
- orphanRemoval
- Request ID handling
- migration/DB reference

### CSV formatを変更する場合

- BackupCsvDataBuilder
- CsvFileWriter
- BackupDataType
- consumer側仕様

### Retentionを追加する場合

- BackupHistory
- BackupHistoryRepository
- scheduler/batch
- StorageService
- S3 lifecycle/Terraform
- Local cleanup

---

## 29. 現時点のV1既知事項一覧

### 高

- SERVER_FILE名称とdownload挙動の不一致可能性
- mixed OutputModeでDOWNLOAD対象データもStorage ZIPへ入る
- 無効化したBackupTargetが管理一覧から消える
- soft deleteとBackupTarget unique constraintの再登録問題要確認

### 中

- Column IDは更新時維持されない
- dataTypeがCSV処理へ使われない
- DB物理型とdataType不一致を許容
- soft delete Rowをbackup対象から除外しない
- Row順非保証
- 複数Table snapshot非保証
- Storage/DB非atomic
- 大容量時heap負荷
- History retention不明
- Validationがplain RuntimeException中心

### 仕様確認

- tenant_id export可否
- DOWNLOAD履歴の再DL不可
- 200件超履歴の扱い
- system/backupを正式DR backupと呼ぶか

---

## 30. system/backup V1詳細設計の読み順

推奨:

```text
00-overview-and-execution-flow.md
→ 01-target-crud.md
→ 02-column-schema-validation.md
→ 05-execution-sql-csv.md
→ 04-output-storage-rules.md
→ 03-history-redownload.md
→ 06-tests-operations.md
```

最初に実行全体を理解し、その後設定、SQL/CSV、Storage、履歴、運用の順で読む。

---

## 31. 次の段階

`system/backup` のコードベース詳細設計は、主要経路について一巡した。

次に行うなら以下のどちらか。

### A. backupのDB実DDL・Terraform/S3 lifecycleだけを追加確認

今回「未決事項」とした以下を確定する。

- backup_target unique index
- backup_history index
- S3 lifecycle
- IAM権限
- DEV Storage設定

### B. 次のsystem機能へ移る

backupと同じ粒度で次機能を調査する。

いずれの場合も、この調査チャットでは**コード修正を行わない**。
