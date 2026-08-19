# ProjectAdminSystem V1 共通アーキテクチャ概要

## 調査基準

- 基準リポジトリ: `aki0609-free/project-admin-system`
- 基準ブランチ: `main`
- 基準コミット: `12c91a72b409df16b9d4be0b416247a07a8f170a` (`test: keep net pay assertion configuration-independent`)
- 調査日: 2026-08-19
- **未コミット変更**: GitHub APIで取得したリモートツリーを調査したため、利用者PC上のworking treeは確認不能。今回の成果物自体は `agent/v1-common-architecture-spec` にのみ追加し、アプリコードは変更していない。

## 記載区分

- **確定仕様**: コード・設定・テスト・IaCが複数箇所で整合して保証している事項。
- **実装事実**: 現在のコードに存在する挙動。意図や将来保証までは断定しない。
- **推測**: 命名・残存資産等から推定した事項。
- **未決事項**: コードだけでは決定できない事項。
- **V2候補**: V1を変更せず将来整理する候補。

## 全体構成

**確定仕様**: 単一リポジトリ内に `backend/`, `frontend/`, `infrastructure/`, `openapi/` を持つ。ローカルはルート `docker-compose.yml`、DEVはTerraform＋GitHub Actionsで構築・配備する。

### Backend

Spring Boot / Java 21。エントリポイントは `backend/src/main/java/com/project/backend/BackendApplication.java`。大分類は以下。

- `app/`: 横断基盤。`config`, `security`, `tenant`, `audit`, `storage`, `persistence`, `job`, `rate` 等。
- `features/`: 業務機能。Controller/DTO/Entity/Repository/Serviceを機能単位で配置。
- `common/`: 例外、ErrorCode、TraceId、日付規則、sanitizer、utility等。
- `batch/`, `schedule/`: バッチ・スケジュール処理。

### Frontend

Vue 3 + TypeScript。`frontend/src/main.ts` → `App.vue` → Vue Router。構成は `app/`（layout/router/plugins/menu）、`features/`（画面単位）、`shared/`（API/auth/UI）、`toolbox/`。

基本処理経路は **Page → Composable → feature API → shared HTTP → Backend Controller → Service → Repository → DB**。例として管理設定は `BusinessSettingsPage.vue` → `useBusinessSettingsPage()` → `businessSettingApi.ts` → `BusinessSettingController` → `BusinessSettingService` → 各Repository → MySQL。

### Infrastructure

**実装事実**: DEVの `infrastructure/environments/dev/main.tf` はEC2 Docker Runtime、RDS MySQL、ECR、S3、CloudWatch、Secrets Manager、Cloudflare Zero Trust、SES、GitHub Actions OIDCを組み合わせる。`ecs_task_execution_iam` モジュールは存在するが、現行DEVのアプリ実行先は `application_host` (EC2) である。

## 横断リクエスト処理

1. Frontend `apiClient.ts` がlocalStorageのaccess tokenをBearerヘッダへ設定。
2. Backend `JwtAuthenticationFilter#doFilterInternal` がJWTを検証し、username/tenantIdを取得してSecurityContextを構築。
3. `TenantFilter#doFilterInternal` は `X-Tenant-ID` がある場合Hibernateの `tenantFilter` と `softDeleteFilter` を有効化。
4. Controllerの `@PreAuthorize` 等で権限判定。
5. ServiceがTransaction境界・業務処理を担当しRepositoryへ委譲。
6. 例外は `GlobalExceptionHandler` が共通 `ErrorResponse` に変換。`TraceIdFilter`/MDCのtraceIdを応答・ログ相関に利用。
7. `@Auditable` 対象は `AuditAspect` が成功/失敗を `AuditLogService` 経由で永続化。

## 重要な既知事項

- **実装事実**: JWT内tenantIdと `X-Tenant-ID` の双方がTenantContextへ書き込む経路がある。両者一致を強制するコードは今回確認範囲では見つからない。
- **実装事実**: `BaseEntity#prePersist/preUpdate` は `Instant.now()` を直接使用し、DIされた `Clock` を使わない。一方 `ApplicationTimeConfig#applicationClock` はAsia/TokyoのClockを提供する。
- **実装事実**: Frontend auth storeの `clearAuth()` は `localStorage.clear()` を呼び、認証以外のlocalStorage項目も削除する。
- **実装事実**: ローカルはMySQL + MongoDB + RedisをComposeで起動するが、DEV Terraformの主要DBはRDS MySQL。MongoDB/RedisのDEV実体はIaCだけからは確定できない。

## このセットの範囲

今回は共通アーキテクチャのみ。個別業務機能の完全な画面/API/テーブル仕様は別フェーズとする。ただし共通構造を説明するため代表経路を追跡する。