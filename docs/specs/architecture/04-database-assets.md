# Database / Assets 共通設計

## データストア

### MySQL

**確定仕様**: 主RDB。ローカル `docker-compose.yml` はMySQL 8.4 / database `ADMIN`。DEV Terraform `modules/mysql_database` はRDS MySQL、`environments/dev/main.tf` ではengine 8.0.46、DB `ADMIN`、backup retention 7日。

JPA Entity/Repositoryが業務永続化の中心。`BaseEntity` が `tenant_id`, `created_at`, `updated_at`, `deleted_at` を共通化しHibernate filterでtenant/soft-deleteを提供する。

### MongoDB / Redis

**実装事実**: ローカルComposeはMongoDB 7とRedis 7.4を起動し、Backendへ接続情報を渡す。`app/config/RedisConfig.java` も存在する。

**未決事項**: DEV Terraformの主要モジュール一覧にはMongoDB/Redisのmanaged resourceが確認できない。DEVで外部サービスを使うか、未使用/移行途中かはruntime secret・運用環境確認が必要。

## Schema管理

`backend/src/main/resources/db/migration/V1__init.sql` は存在するが非常に小さい。ローカルComposeでは `SPRING_FLYWAY_ENABLED=false`, `SPRING_JPA_HIBERNATE_DDL_AUTO=update` とし、backend health後に `runtime-schema` サービスが `infrastructure/scripts/database/apply_local_runtime_schema.sh` を実行する。

**実装事実**: DB schemaの正本がFlywayだけではない。JPA ddl-auto + runtime schema scripts/resourcesが関与する。

**V2候補**: 本番/DEV/ローカルのschema適用方式を一つのversioned migration体系へ寄せ、差分検出をCI化する。

## Storage assets

`app/storage` はLOCAL/S3を `StorageBackend` で抽象化。`StorageService` が上位窓口、`LocalStorageService` / `S3StorageService` が実装。

DEV Terraform `document_bucket` は業務文書用S3 bucketを作成し、`backend_runtime_iam` がbucket access policyをアプリ実行roleへ付与する。GitHub Actions deploy roleにもdeployment bucket ARNが渡される。

## 帳票・静的資産

Backend resourcesに `fonts/`, `image/`, JasperReports設定、`businessview/*.SQL` が存在する。帳票生成/バックアップはStorageとDB双方へ依存する可能性があるため、個別帳票詳細設計でテンプレート・SQL・保存先・retentionを追跡する。

## データ境界

**確定仕様**: tenant対応Entityでは `tenant_id` を保持し、request時にHibernate filterで絞る設計。

**既知事項**: filter有効化が `X-Tenant-ID` の有無に依存するため、JWT tenantだけで常にDB filterが有効になるとはコード上保証されない。Repositoryの個別tenant条件と合わせた精査が必要。

## バックアップ

- RDS: Terraformでbackup retention 7日。
- 業務帳票: `AnnualReportBackupSettingService` と管理画面から年度バックアップを実行可能。
- S3 lifecycle/versioning等の詳細は `modules/document_bucket/main.tf` を個別運用設計で確認対象とする。

## 未決事項

- MySQL以外の永続ストアのDEV実体。
- schema正本の正式な運用ルール。
- tenant filterを適用しないEntity一覧と理由。
- S3 object keyのtenant境界・削除/retention規則の全機能横断保証。