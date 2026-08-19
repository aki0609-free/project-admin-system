# API / Backend 共通アーキテクチャ

## レイヤ

**実装事実**: Spring Bootコードは横断基盤 `app/`、業務 `features/`、共通 `common/` に分離。業務機能は概ね Controller → Service → Repository → Entity/DB。

代表経路:
`BusinessSettingsPage.vue` → `useBusinessSettingsPage()` → `businessSettingApi.ts` → `BusinessSettingController` → `BusinessSettingService` / `AnnualReportBackupSettingService` / `ExternalSupportLinkSettingService` → Repository → MySQL。

## 認証

主要ファイル:

- `app/config/SecurityConfig.java#filterChain`
- `app/security/auth/controller/AuthController.java#login/#refresh/#me`
- `app/security/auth/services/AuthService.java#login/#refresh/#me`
- `app/security/jwt/filter/JwtAuthenticationFilter.java#doFilterInternal`
- `app/security/jwt/services/JwtService.java`
- `app/security/jwt/services/RefreshTokenService.java`

`SecurityConfig` はSTATELESS、CSRF disabled。`/auth/login`, `/auth/refresh`, `/actuator/health`, Swagger/OpenAPIをpermitAll、それ以外は認証必須。

`AuthService#login` はAuthenticationManager → `SecurityUser` → access token生成 + refresh token作成。`#refresh` はrefresh token検証後、username + tenantIdでuserを再ロードしaccess tokenを再発行する。

## 権限

`@EnableMethodSecurity` を有効化。業務Controllerでは `@PreAuthorize` を使用。例: `BusinessSettingController` はクラス単位で `hasRole('SYS_ADMIN')`。

権限モデルは `app/security/permission/entity/Role.java`, `Permission.java`、Repository、`PermissionCacheService` に存在。Frontendにもpermissions定数と `usePermission()` があるが、最終防御はBackend。

## テナント

主要ファイル:

- `app/tenant/context/TenantContext.java`
- `app/tenant/filter/TenantFilter.java#doFilterInternal`
- `app/base/entity/BaseEntity.java`
- `app/security/jwt/filter/JwtAuthenticationFilter.java`

`BaseEntity` は `tenant_id`, created/updated/deleted timestampを共通化し、Hibernate `tenantFilter` と `softDeleteFilter` を定義する。

**実装事実**: `TenantFilter` は `X-Tenant-ID` が存在する場合のみHibernate filterを有効化する。JWT filterもtoken claimのtenantIdを `TenantContext` へ設定する。ヘッダtenantとJWT tenantの一致検証は確認できないため、セキュリティ上の既知事項として扱う。

## 監査

`@Auditable` → `AuditAspect` の `@AfterReturning/@AfterThrowing` → `AuditLogService#recordAuditLog` → `AuditLogRepository#save`。SecurityContextの `SecurityUser` からuserId/tenantIdを取得し、MDC `traceId`、action、target、成功可否を記録する。

**実装事実**: 監査保存自体の独立Transaction指定は `AuditLogService` では確認できない。呼出元Transactionとの関係は対象Serviceごとに確認が必要。

## 例外・Trace

`common/handlers/GlobalExceptionHandler` が以下を変換する。

- `BusinessException` → 保持する `ErrorCode`
- `MethodArgumentNotValidException` → VALIDATION_ERROR + field messages
- `EntityNotFoundException` → RESOURCE_NOT_FOUND
- その他 `Exception` → COMMON_INTERNAL_ERROR

全てMDCのtraceIdを `ErrorResponse` とstructured logへ含める。入口は `common/filter/TraceIdFilter.java`。

## Clock

`app/config/ApplicationTimeConfig#applicationClock` は `Clock.system(Asia/Tokyo)` をBean化。

**不整合**: `BaseEntity#prePersist/#preUpdate` は `Instant.now()` を直接利用しておりClock非依存。テスト可能性・時刻統一の観点でV2候補。

## Storage

主要ファイル:

- `app/storage/service/StorageBackend.java`
- `StorageService.java`
- `LocalStorageService.java`
- `S3StorageService.java`
- `app/storage/properties/StorageProperties.java`
- `app/storage/config/StorageConfig.java`, `S3StorageConfig.java`

LOCAL/S3を抽象化。ローカルComposeは `PROJECT_STORAGE_DEFAULT_TYPE=LOCAL`, `/app/storage` volume。DEV Terraformはdocument S3 bucketとbackend IAM policyを作成する。

## Transaction

**実装事実**: Transaction境界はService層に置かれる設計が中心だが、共通アーキテクチャ調査では全Serviceの `@Transactional` を網羅確認していない。個別機能詳細設計では各更新APIについてreadOnly/更新Transaction、副作用（Storage、mail、audit、batch）を必ず追跡する。

## V2候補

- JWT tenantとrequest tenantの単一ソース化・一致強制。
- Clock利用規約の統一。
- 監査ログの失敗時永続化を `REQUIRES_NEW` 等で保証するか検討。
- ErrorResponseとSecurity filter直接401応答の形式統一。
- architecture testでController→Service→Repository依存方向を保証。