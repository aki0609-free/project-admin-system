# system/backup 詳細設計 07 — 追加確認・未決事項・修正候補まとめ

## 1. この文書の位置づけ

この文書は `system/backup` の最終まとめである。

ここではコード・Terraform・CI/CD・DEV Runtime設定を追加確認し、以下だけを整理する。

- 追加確認で確定した事項
- 未決事項
- 既知不整合
- 修正候補
- 優先度
- 修正時の影響範囲

**この調査ではコード・Terraform・CI/CDを修正しない。**

基準コード:

- branch: `main`
- commit: `12c91a72b409df16b9d4be0b416247a07a8f170a`

設計書作成branch:

- `agent/v1-common-architecture-spec`

---

## 2. 追加確認で確定した事項

### 2.1 DEVのsystem/backup保存先はS3

DEV Runtime `infrastructure/runtime/dev/compose.yaml` ではBackendに:

```text
PROJECT_STORAGE_DEFAULT_TYPE=S3
PROJECT_STORAGE_S3_ENABLED=true
PROJECT_STORAGE_S3_BUCKET=...
```

が明示されている。

**確定仕様（現行DEV）**: DEVでStorage保存対象となるsystem/backupはS3へ保存される。

Local DockerだけがLOCAL Storageである。

---

### 2.2 S3 bucketは書類管理共通bucket

Terraform DEV:

```text
module document_bucket
```

を作成し、そのbucket ARNをBackend用IAM policyへ渡す。

system/backup固有bucketは存在せず、書類管理共通bucketの:

```text
documents/backups/system/{tenantId}/...
```

配下を利用する。

---

### 2.3 S3 bucketの保護設定

`document_bucket` moduleで確認できるもの:

- Public Access Block
- BucketOwnerEnforced
- Versioning Enabled
- SSE-S3 (`AES256`)
- insecure transport deny
- Terraform `prevent_destroy=true`

**確定仕様**: system/backupオブジェクトはversioning有効・暗号化済み・非公開bucketに保存される。

---

### 2.4 system/backup向けS3 retention ruleは無い

S3 lifecycleに存在する有効なexpiration ruleは:

```text
prefix = documents/backups/reports/
```

の年次帳票backup用7年保持のみ。

system/backupのprefix:

```text
documents/backups/system/
```

にはexpiration / noncurrent version expirationが設定されていない。

**確定仕様（Terraform上）**: system/backupファイルはS3 lifecycleによる期限削除対象ではない。

---

### 2.5 S3 VersioningとDeleteObject

`S3StorageService#delete()` はversionIdを指定せず通常の `DeleteObject` を実行する。

Versioning Enabled bucketでは通常、この操作は現在版を削除markerで隠し、過去versionを保持する。

system prefixにはnoncurrent version expirationが無いため、**cleanupや利用者から見えなくなったオブジェクトでも旧versionのデータが長期間残る可能性がある**。

これは以下に影響する。

- retention
- S3コスト
- データ削除方針
- 個人情報削除方針

**未決事項**: system/backupの旧versionを何日/何年保持すべきか。

---

### 2.6 Backend RuntimeのS3権限

Backend用document bucket policyは以下を許可する。

Bucket:

- GetBucketLocation
- ListBucket
- ListBucketMultipartUploads

Object:

- GetObject
- PutObject
- DeleteObject
- AbortMultipartUpload
- ListMultipartUploadParts

EC2 `application_host` のInstance Roleへこのpolicyがattachされる。

**確定仕様**: 現行DEVのBackend containerはEC2 Instance Profile経由でdocument bucketへアクセスする。

---

### 2.7 IAMはsystem backup prefix限定ではない

IAM Object resourceは:

```text
{document_bucket_arn}/*
```

であり、`documents/backups/system/*` のみに限定されていない。

**実装事実**: Backend Runtimeはbackupだけでなくdocument bucket内の全objectをGet/Put/Delete可能。

これは共通DocumentStorageを利用するシステム設計とは整合するが、system/backupだけのleast privilegeではない。

---

### 2.8 RDSには別の7日backupが存在する

Terraform DEV MySQL:

```text
backup_retention_period = 7
```

さらに:

- storage encryption
- deletion protection
- final snapshot
- automated backupを削除しない設定

がある。

**確定事項**: `system/backup` のCSV exportとは別に、RDS自身のDB backup機構が存在する。

したがって役割は区別する必要がある。

---

### 2.9 system/backupはDR backupというより論理Export

コードとInfraを合わせるとsystem/backupは:

- 選択table
- 選択Column
- CSV/ZIP
- 任意Storage保存
- Restore無し
- DDL/index/FK無し
- 同時snapshot保証無し

である。

一方、RDSに7日backupがある。

**実装事実からの分類**: system/backupは管理者向け「業務データ論理Export」に近く、RDS disaster recovery backupとは別物。

**仕様確認推奨**: UI/業務文書で「バックアップ」と呼ぶ際、この2種類を明確に区別する。

---

## 3. DB実DDL追加確認

### 3.1 Flywayは現行Schemaの正本ではない

`db/migration/V1__init.sql` は `test_table` のみ。

また:

```text
application.yml      Flyway default false
application-aws.yml  Flyway false
```

AWSでは:

```text
spring.jpa.hibernate.ddl-auto=validate
```

である。

**確定事項**: 現行AWS DB SchemaはFlyway migration群を正本として再現できる状態ではない。

Runtime schema upgrade script等で既存DBへDDLを適用する運用が別に存在する。

---

### 3.2 BackupTarget EntityのUnique

Entityは:

```text
UNIQUE(tenant_id, target_code)
```

を宣言する。

Application重複Validationは:

```text
tenant_id + target_code + deleted_at IS NULL
```

のみを重複扱いする。

### 未決

Repository内のDDL sourceから、DEV実DBのunique index定義を完全には確定できなかった。

AWSはHibernate `validate` だが、実DB index/constraintまでこの設定だけで完全保証されるとは扱わない。

したがって以下は**未決事項のまま**。

> soft delete後に同じtenantId + targetCodeを再登録できるか。

もし実DBもEntity通り `(tenant_id,target_code)` Uniqueなら、soft delete済みRowが残るため再登録時DB constraint violationとなる。

**確認方法候補（修正ではない）**:

DEV DBで `SHOW CREATE TABLE backup_target` または `information_schema.statistics` を確認する。

---

### 3.3 Runtime upgradeでbackup定義を運用検証している

`run_runtime_schema_upgrade_sql.sh` はdefault tenantについて以下4Targetを検証する。

- `BACKUP_CUSTOMERS`
- `BACKUP_CUSTOMER_TRANSACTIONS`
- `BACKUP_EMPLOYEES`
- `BACKUP_DAILY_REPORTS`

条件:

- output_mode = DOWNLOAD
- zip_required = TRUE
- backup_enabled = TRUE
- active_flag = TRUE
- deleted_at IS NULL

さらに対象tableの全物理Columnが `backup_column` のexport対象として定義されていることも検証する。

**実装事実**: 少なくともこれら4つはruntime schema upgrade運用の検証対象になっている。

---

## 4. CI/CD追加確認

### 4.1 通常CI

`.github/workflows/ci.yml` のBackend unit testは全testを実行するため、backup unit testも対象。

### 4.2 Deploy DEVのverify

`deploy-dev.yml` のdeploy前Backend testは対象packageを限定している。

対象:

- app.openai
- app.storage
- common.sanitizer
- system.mail
- system.report

**system.backupは含まれていない。**

さらにDeploy DEV workflowはmanual `workflow_dispatch` であり、コード上は通常CI成功を `needs` していない。

**実装事実**: deploy-dev workflow単体ではbackup unit testを再実行しない。

### 修正候補

- deploy verify対象へsystem.backupを追加
- またはdeploy workflowをCI成功commitに限定

今回は修正しない。

---

## 5. 最終 未決事項一覧

以下は現行コードから意図を一意に決められない。

### U-01 system/backupの正式な役割

候補:

- 管理者用CSV Export
- 長期保管用論理backup
- 障害復旧backup

コード上は1つ目に最も近い。

**確認質問**: RDS backupとは別の「業務データExport」と定義してよいか。

---

### U-02 SERVER_FILEの意味

UI:

```text
ストレージ保存のみ
```

現行経路:

- Storageへ保存
- HTTP bodyにもbyte[]返却
- FrontendもBlob download処理へ進む可能性

**確認質問**: 本当に端末DL禁止なのか。

---

### U-03 mixed OutputMode

複数Target:

```text
DOWNLOAD + BOTH
```

等を同時実行すると、最終ZIPがStorage保存され、DOWNLOAD TargetのデータもそのZIPへ含まれる。

**確認質問**: mixed modeを許可するか。

---

### U-04 system S3 retention

現状system prefixに期限削除なし。

**確認質問**:

- 何年保持か
- current version / noncurrent versionの双方を何年保持か
- 無期限か

---

### U-05 BackupHistory retention

DB履歴はTop200表示のみ。

DB purgeなし。

**確認質問**:

- 履歴DBを何年保持するか
- S3と同期間にするか

---

### U-06 soft delete済み業務Row

Dynamic SQLはtenant条件だけで:

```text
deleted_at IS NULL
```

を付けない。

**確認質問**: 削除済みデータもbackupへ含めるのが正しいか。

---

### U-07 Row順

SELECTにORDER BYなし。

**確認質問**: CSV行順不定でよいか。

---

### U-08 tenant_id export

物理Columnとして存在すればexport可能。

**確認質問**: tenant_idをCSVへ含めることを許可するか。

---

### U-09 BackupColumn.dataType

保存されるが:

- DB type照合なし
- CSV formatting利用なし

**確認質問**: 将来利用予定か、不要metadataか。

---

### U-10 BackupTarget再登録

soft delete後の同じtargetCode再登録可否がDB実constraint確認待ち。

---

### U-11 200件より古い履歴の再DL

一覧には出ないがhistoryIdが分かればAPI上取得できる可能性。

**確認質問**: 正式に許可するか。

---

### U-12 最大データ量

現在全件memory処理。

**確認質問**:

- 最大Row数
- 最大CSVサイズ
- 最大ZIPサイズ

運用上限が必要。

---

## 6. 既知不整合・修正候補

以下は「修正する」と決めたものではなく、**修正候補の一覧**。

### P0 — 仕様/セキュリティ上、先に判断推奨

#### C-01 SERVER_FILEと実download挙動

現象:

UI名称とコード挙動が一致していない可能性。

修正候補:

- SERVER_FILE時Frontendでdownloadしない
- Backendを204/metadata responseに変更
- または名称を「保存＋DL」に変更

影響:

- BackupPage
- execute mutation
- BackupController
- BackupExecutionResult
- History
- E2E

---

#### C-02 mixed OutputMode

現象:

DOWNLOAD TargetもStorage ZIPへ入る。

修正候補:

- mixed mode禁止
- Request単位OutputMode
- 個別fileだけStorage保存
- ZIPはdownload成果物専用

影響大。

---

#### C-03 S3 system backup retention

現象:

system prefixにexpiration無し。
Versioning旧版も残る。

修正候補:

Terraform lifecycleに:

```text
documents/backups/system/
```

用policy追加。

要件決定後に実施。

---

#### C-04 BackupTarget無効化後一覧から消える

現象:

管理一覧自体がenabled+activeのみ取得。

結果:

画面から再有効化困難。

修正候補:

- 管理一覧はdeletedAtのみ除外
- 実行対象一覧だけenabled/active filter

---

### P1 — 整合性・運用上優先

#### C-05 soft delete済みRowのbackup方針

候補:

- deleted_at IS NULLを自動付与
- Target単位でincludeDeleted設定
- 現状維持を明記

---

#### C-06 BackupTarget soft deleteとUnique

まずDEV実DDL確認。

問題が確定した場合候補:

- 物理削除
- targetCode再利用禁止を仕様化
- generated active key等を利用したunique設計
- targetCode変更不可を維持し別コード採番

DB設計変更なので慎重に扱う。

---

#### C-07 History/S3 retention同期

履歴だけ残りfileがない、またはfileだけ残る状態をどう扱うか定義。

候補:

- retention batch
- lifecycle + DB purgeを同期間
- fileAvailable status管理

---

#### C-08 Deploy DEVでbackup testをgate

候補:

- `features.system.backup.*` をdeploy verifyへ追加
- CI workflow成功をdeploy前提にする

---

#### C-09 Domain Error化

現状RuntimeException中心。

候補:

- BackupValidationException
- BackupDefinitionException
- BackupStorageException
- 共通BusinessException/ErrorCode

UIで400系/500系を明確に分離。

---

### P2 — 品質・拡張性

#### C-10 Column ID全置換

更新時Column全再作成。

候補:

- ID維持差分merge
- 「Column IDは内部一時IDで非保証」と仕様化

---

#### C-11 dataTypeの責務整理

候補:

- DB型互換Validationへ使う
- CSV formatterへ使う
- 不要なら削除

---

#### C-12 CSV Row順

候補:

- orderByColumn設定追加
- PK自動ORDER BY
- 行順不定を仕様化

---

#### C-13 実行前Schema再Validation

現状Schema変更後はSQL execution errorになりうる。

候補:

保存済みColumnと現Schemaを実行前明示比較。

---

#### C-14 Clock統一

backup Builder等はClock使用。
BaseEntityは `Instant.now()`。

候補:

共通Entity audit時刻もApplication Clockへ統一。

---

### P3 — 大容量対応

#### C-15 Streaming化

現状:

```text
queryForList
→ List<Map>
→ CSV byte[]
→ ZIP byte[]
→ S3 / HTTP
```

再DLも `readAllBytes()`。

候補:

- JDBC streaming
- streaming CSV
- streaming ZIP
- S3 multipart upload
- StreamingResponseBody

最大データ量要件確定後に判断。

---

## 7. テスト追加候補まとめ

優先順。

### 最優先

- BackupExecutionService success/failure/cleanup
- SERVER_FILE E2E
- mixed OutputMode
- Tenant別Target/Historyアクセス拒否
- S3 Storage integration相当

### 次点

- BackupSchemaInspector integration
- soft delete Row inclusion
- history 200件境界
- S3 file missing再DL
- Target無効化→再有効化画面
- soft delete後targetCode再登録

### 品質

- CSV quote/newline/BOM/type
- Zip Slip
- Column editor resequence
- outputDir traversal
- dataType mismatch

---

## 8. Infra運用上の確認事項

### RDS

Terraform DEV:

- automated backup 7日
- backup window 08:30-09:00 UTC設定値
- deletion protection
- encrypted
- final snapshot

system/backupとは別系統。

### S3

- versioning enabled
- AES256
- public block
- system prefix retentionなし

### DEV Backend

- Default Storage=S3
- mem_limit=2800m

大容量backupはこのmemory上限との関係を考える必要がある。

---

## 9. 現在の評価

### 良い点

- SYS_ADMIN制御
- Tenant aware repository
- Dynamic SQL identifier防御
- Storage abstraction
- TenantをStorage keyへ含める
- S3 versioning/encryption/public block
- success/failure history
- Clock導入箇所あり
- cleanup補償処理
- Local/S3共通interface

### 注意点

- backupという名称に対しDR機能ではない
- OutputModeの意味が曖昧
- lifecycle未定義
- memory全件処理
- DB/Storage非atomic
- Schema migration正本が弱い
- backup専用integration/E2Eが不足

---

## 10. メインチャットで修正検討する場合の推奨順

この調査チャットでは修正しない。

修正する場合は、まず仕様判断を先に行う。

```text
1. system/backupの役割を確定
2. SERVER_FILE / BOTH / DOWNLOADの意味を確定
3. mixed OutputMode方針を確定
4. retention期間を確定
5. soft delete Row方針を確定
6. DB Unique実態を確認
7. その後コード修正
8. backup integration/E2E追加
```

仕様を決める前にコードだけ直すと、別の不整合を作る可能性が高い。

---

## 11. backup詳細設計 全ファイル

```text
00-overview-and-execution-flow.md
01-target-crud.md
02-column-schema-validation.md
03-history-redownload.md
04-output-storage-rules.md
05-execution-sql-csv.md
06-tests-operations.md
07-summary-open-issues.md
```

これで `system/backup` のV1コードベース詳細設計は主要範囲を一巡した。
