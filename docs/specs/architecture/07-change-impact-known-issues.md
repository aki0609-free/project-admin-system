# Change Impact / Known Issues

本書は今回見つけた不整合を**修正せず**記録する。

## KI-01 Tenant情報が二経路

**実装事実**: `JwtAuthenticationFilter` はJWT claim tenantIdを `TenantContext` に設定。`TenantFilter` は `X-Tenant-ID` をTenantContextとHibernate filter parameterへ設定する。

**影響**: request headerとtokenが不一致の場合、認証主体tenantとDB filter tenantがずれる可能性を否定できない。tenant分離に直結するため高優先度。

**修正候補**: JWT tenantを正としheaderを廃止、または両者一致をfilterで必須検証。Repository横断security testを追加。

## KI-02 Tenant filterがheader依存

`TenantFilter` は `X-Tenant-ID != null` のときだけtenant/soft-delete filterをenableする。

**影響**: headerなし認証済みrequestでBaseEntity queryが自動tenant絞込されない経路があり得る。

**修正候補**: SecurityContext/JWTからtenantを必ず解決してfilterを有効化し、tenant必須APIでは欠落をreject。

## KI-03 Clockの利用不統一

`ApplicationTimeConfig` はAsia/Tokyo Clockを提供するが `BaseEntity` は `Instant.now()` を直接使用。

**影響**: 時刻固定テスト、business timezone統一、将来の時刻シミュレーションが難しい。

**修正候補**: entity listener/time provider等へ集約。Instant保存とbusiness local date計算の責務を分離。

## KI-04 認証エラー形式が共通Advice外

JWT不正時 `JwtAuthenticationFilter#rejectAuthentication` が直接 `{message: ...}` を返す一方、通常例外は `ErrorResponse(code,message,traceId,...)`。

**影響**: Frontend共通error handlerがstatusによって別schema対応を要する。

**修正候補**: AuthenticationEntryPoint/AccessDeniedHandlerでErrorResponseへ統一。

## KI-05 Frontend localStorage全消去

`useAuthStore#clearAuth` は `localStorage.clear()`。

**影響**: 認証以外の将来設定・UI preference・feature stateを巻き込む。

**修正候補**: accessToken/refreshToken等の所有キーのみremove。

## KI-06 Frontend品質gateが一部non-blocking

CIのtype-check/lintは `continue-on-error: true`。unit testは `--passWithNoTests`。

**影響**: build可能でも型/lint debtやunit test欠落がmainへ入る。

**修正候補**: baseline解消後に段階的blocking化。

## KI-07 Schema管理方式が複数

Flyway migrationは存在するがlocalではdisabled、JPA ddl-auto=updateとruntime-schema scriptを併用。

**影響**: 環境差・再現性・rollback/upgrade手順の複雑化。

**修正候補**: versioned migrationを唯一のschema正本へ。

## KI-08 DEV runtimeとECS IAM命名の混在

Terraformに `ecs_task_execution_iam` があるが現行 `application_host` はEC2 Docker Runtime。

**推測**: ECS案の残存、または将来用資産の可能性。

**影響**: 新規担当者がruntimeをECSと誤認しやすい。

**修正候補**: 未使用確認後削除、またはREADMEで用途を明示。

## KI-09 ローカルとDEVのデータサービス差

local ComposeはMySQL/MongoDB/Redis。DEV Terraformで明確なのはRDS MySQL等で、MongoDB/Redis runtimeはIaCから確定できない。

**影響**: local-only依存がDEVで発覚する可能性、運用責任境界が不明瞭。

**確認事項**: DEV runtime secretと実環境composeを照合する。

## 変更影響マップ

- 認証/JWT変更: Backend security filters/services、Frontend auth store/api client/router guard、E2E login。
- tenant変更: filters、BaseEntity、全tenant Repository、JWT claim、Frontend tenant header有無、integration tests。
- Clock変更: BaseEntity、日付計算Service、batch/schedule、給与/締め処理テスト。
- Storage変更: StorageService/backends、document/report機能、S3 IAM/Terraform、deploy smoke。
- Error schema変更: GlobalExceptionHandler、security handlers、Frontend HTTP wrappers/UI。
- Infra runtime変更: Terraform modules/env、runtime compose、deploy script、GitHub Actions、Cloudflare origin。

## 今回の結論

V1の共通構造は「Vue feature分割 + Spring feature分割 + JWT/role + tenant filter + audit + Storage abstraction + EC2 Docker/RDS/S3 + GitHub Actions/Terraform」で一貫している。一方、tenant境界、Clock、schema運用、Frontend品質gateはV2またはV1安定化フェーズで優先確認すべき。