# 共通シーケンス

## 1. ログイン

1. Login Page/Composable（個別画面詳細は別紙）→ `POST /auth/login`。
2. `AuthController#login` → `AuthService#login`。
3. `AuthenticationManager.authenticate` → `CustomUserDetailsService` + BCrypt password encoder。
4. 認証成功後 `SecurityUser` を取得。
5. `JwtService#generateToken` でaccess token、`RefreshTokenService#createToken` でrefresh tokenを生成/保存。
6. `AuthResponse(user, accessToken, refreshToken)` を返す。
7. Frontend `useAuthStore#setAuth` がPinia state + localStorageへ保存。

副作用: refresh token永続化、localStorage更新。

## 2. 認証済みAPI

1. feature API → `shared/api/http.ts` → `apiClient`。
2. `apiClient.onRequest` がBearer access tokenを付与。
3. `JwtAuthenticationFilter#doFilterInternal` がtokenからusername/tenantIdを抽出し、user再ロード、token検証、SecurityContext設定。
4. `TenantFilter#doFilterInternal` は `X-Tenant-ID` があればTenantContext + Hibernate tenant/softDelete filterを有効化。
5. Controller `@PreAuthorize` でrole/permission判定。
6. Service → Repository → DB。
7. finallyでTenantContext/filterをclear/disable。

既知事項: JWT tenantIdとX-Tenant-IDの一致確認が見当たらない。

## 3. 管理設定の初期表示

`BusinessSettingsPage.vue` → `useBusinessSettingsPage#load` → `Promise.all` で以下を並列呼出:

- resignation message
- resignation checklist
- closing setting
- closing outputs
- dormitory fees
- annual report backup setting
- external support links

各APIは `businessSettingApi.ts` → `BusinessSettingController` → 対応Service → Repository → MySQL。全完了後Composable stateへ一括反映する。1 APIでもrejectすると `run()` がalertして全体loadを失敗扱いにする。

## 4. 更新APIと監査

一般形:

1. Page event → Composable `run(action)`。
2. feature API PUT/POST/DELETE。
3. Controller validation (`@Valid`) + `@PreAuthorize`。
4. Serviceの更新Transaction。
5. Repository save/delete、必要に応じStorage/mail等副作用。
6. 対象methodに `@Auditable` があれば正常return後 `AuditAspect#audit`、例外時 `#auditError`。
7. `AuditLogService#recordAuditLog` → `AuditLogRepository#save`。
8. Frontendはresponse反映または再GET。

**注意**: auditのTransaction独立性は共通Serviceでは明示されていない。rollback時に失敗監査が残る保証は個別確認が必要。

## 5. 例外

Controller/Service/Repositoryで例外 → `GlobalExceptionHandler`。

- validation → VALIDATION_ERROR
- entity not found → RESOURCE_NOT_FOUND
- BusinessException → domain ErrorCode
- その他 → INTERNAL_ERROR

MDC traceIdをログと `ErrorResponse` に付与。JWT filter内の無効tokenはAdviceを経由せずfilter自身がHTTP 401 JSONを書き込むため、error schemaは別経路。

## 6. Storage

Service → `StorageService` → 設定された `StorageBackend` → LOCALまたはS3。

LOCAL: filesystem `/app/storage`（Compose volume）。
S3: AWS SDK + `S3StorageService`、DEVではEC2 instance roleにdocument bucket権限をTerraform付与。

## 7. DEV deploy

1. 手動 `workflow_dispatch`、入力値 `DEPLOY DEV` 必須。
2. verify job: Backend stable tests + Frontend build。
3. GitHub Actions OIDCでAWS deploy roleをassume。
4. Backend/Frontend Docker imageをimmutable tagでbuild。
5. ECRへpush。
6. `infrastructure/scripts/deployment/deploy_runtime_ci.sh` がSSM経由でEC2 runtimeを更新。
7. Cloudflare Accessへの302 redirectをcurlで確認。

Terraformはruntime資源を事前構築し、deploy workflowはアプリimage更新を担当する。