# Tests / Operations

## CIで保証されるもの

`.github/workflows/ci.yml` はpush/PRで以下を実行する。

### Backend

- Java 21
- `./gradlew test`
- `./gradlew integrationTest`
- `clean bootJar -x test`

main branch protectionのrequired contextに `Backend package and stable tests` が設定されている。

確認できた共通系テスト例:

- `app/config/CorsConfigTest`
- `app/security/jwt/filter/JwtAuthenticationFilterTest`
- `app/storage/service/LocalStorageServiceTest`
- `S3StorageServiceTest`
- `StorageServiceTest`
- `common/filter/TraceIdFilterTest`
- `common/sanitizer/HtmlSanitizerTest`
- `features/admin/business/service/BusinessSettingServiceTest`
- `ExternalSupportLinkSettingServiceTest`

### Frontend

- `npm ci`
- production dependency audit (high以上でfail)
- unit test command。ただし `--passWithNoTests`
- type-check: continue-on-error
- lint: continue-on-error
- production build: 必須

main required contextは `Frontend production build`。

### E2E

CIが `docker compose up --build --wait` でMySQL/MongoDB/Redis/Backend/runtime-schema/Frontendを起動しPlaywright smoke testを実行。失敗時compose logsを出力し、alwaysでvolume込みdown。

### Terraform

Terraform 1.15.5。`terraform fmt -check -recursive`、bootstrap/devの `init -backend=false` + `validate`。main required contextは `Terraform format and validation`。

## DEV deployment

`.github/workflows/deploy-dev.yml` は手動実行。`DEPLOY DEV` confirmationが必要。

verifyでは特定Backend stable testsとFrontend buildを再実行。deployではGitHub Environment `dev` のvars/secretsを検証し、OIDCでAWS roleをassume、ECRへimage push、SSMでruntime deploy、最後にCloudflare Access境界を検証する。

## 運用基盤

Terraform DEV:

- EC2 application host + Docker
- RDS MySQL
- ECR backend/frontend
- S3 document bucket
- CloudWatch logs/monitoring
- Secrets Manager runtime secret
- Cloudflare Tunnel/Access
- SES
- GitHub Actions deploy IAM/OIDC

## テストで保証不足の共通事項

- TenantFilterの `X-Tenant-ID` とJWT tenant不一致拒否。
- Tenant filterが全tenant entity/queryに適用されることの横断テスト。
- `AuditAspect` 成功/失敗とrollback時の監査永続性。
- `ApplicationTimeConfig` Clockを全時刻生成箇所が利用すること。
- Frontend auth guard + permission/roleのunit test網羅。
- Frontend API共通エラー/401/refresh flow。
- type-check/lintは現在blockingではない。
- DEV deploy後のBackend API health/DB migration/代表認証済みAPIまでのsmokeはCloudflare redirect確認ほど明示的ではない。

## 運用上の確認ポイント

障害調査はtraceIdを起点にFrontend表示/HTTP response → Backend structured log → CloudWatchを相関させる設計。Storage障害はLOCAL/S3 backend、DB障害はRDS、deploy障害はGitHub Actions → ECR → SSM → EC2 Dockerの順で切り分ける。

## V2候補

- frontend type/lint/unitをrequired gate化。
- tenant isolation security test suiteを独立追加。
- schema drift test。
- deploy後authenticated smoke + S3 read/write smoke。
- audit/trace/error schema contract test。
- Terraform plan policy/security scanのCI追加。