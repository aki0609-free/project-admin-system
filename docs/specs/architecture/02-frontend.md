# Frontend 共通アーキテクチャ

## 構造

**確定仕様**: Vue 3/TypeScriptの機能分割。`frontend/src/main.ts` が起動点、`frontend/src/app/router/index.ts` がRouter、`frontend/src/app/router/guard.ts` が認証・権限ガードを登録する。

- `src/app/`: layout (`AppLayout.vue`, `AppHeader.vue`, `AppSidebar.vue`)、router、menu、plugins。
- `src/features/`: Page/Composable/API/types/componentsを業務機能単位に配置。
- `src/shared/`: 共通API、auth、共通UI。代表: `shared/api/http.ts`, `shared/auth/store/useAuthStore.ts`, `shared/auth/composables/usePermission.ts`。
- `src/stories/`: Storybook資産。

## Page → Composable → API

代表例: 管理 > 業務設定。

1. Page: `features/admin/business/pages/BusinessSettingsPage.vue`。
2. Composable: `useBusinessSettingsPage()`。`onMounted(load)`、loading制御、confirm/alert、編集状態を集約。
3. API: `features/admin/business/api/businessSettingApi.ts`。`/api/admin/business-settings` 配下を `get/post/put/del` で呼ぶ。
4. 共通HTTP: `shared/api/http.ts`。
5. OpenAPI client: `app/plugins/apiClient.ts`。`createClient<paths>` を利用。

**実装事実**: `useBusinessSettingsPage#load` は7 APIを `Promise.all` で並列取得する。保存後に再取得する処理（チェックリスト）と、返却値をそのままstateへ反映する処理が混在する。

## 認証状態

`shared/auth/store/useAuthStore.ts` がPinia store。

- `setAuth`: user/accessToken/refreshTokenをstateとlocalStorageへ保存。
- `loadAuth`: localStorageからtoken復元。
- `initAuth`: accessTokenがあればAxiosで `/auth/me` を呼びuserを復元。失敗時 `clearAuth`。
- `clearAuth`: stateをnull化し `localStorage.clear()`。

**既知事項**: access/refresh tokenはlocalStorage保管。XSS耐性の観点ではHttpOnly Cookie方式との差をV2で再評価候補。

## Router権限

`setupAuthGuard(router)` (`app/router/guard.ts`) は以下を順に判定する。

1. `authReady` でなければ `initAuth()`。
2. `meta.requiresAuth` かつ未認証 → `/login`。
3. `meta.resource` + `meta.action` → `usePermission().can(...)`。不可なら `/forbidden`。
4. `meta.roles` → user.rolesとの包含判定。不可なら `/forbidden`。

**確定仕様**: Frontend権限制御はUX上の表示/遷移制御であり、Backendの `@PreAuthorize` がセキュリティ境界である。

## APIクライアント

`app/plugins/apiClient.ts` は `VITE_API_BASE_URL` をbaseUrlとし、request middlewareでlocalStorageの `accessToken` をBearer付与する。`shared/api/http.ts` はGET/POST/PUT/PATCH/DELETE、FormData、Blob downloadをラップする。

**実装事実**: `getBlob()` はopenapi-fetchを通さずnative `fetch` を使い、Bearerを手動付与する。通常APIとファイル取得でエラー整形が統一されていない。

## 共通UI

`shared/components/` に `BaseDialog.vue`, `ErrorPage.vue`, form base群, `GenericChart.vue`, `SearchBar.vue`, `DslEditor.vue` 等を配置。Storybook storyが一部共通コンポーネントに存在する。

## テスト

CIでは `npm run test:unit -- --run --project unit --passWithNoTests` を実行するが、`--passWithNoTests` のためテスト不存在でもCI成功可能。type-checkとlintは `continue-on-error: true` で観測のみ。production buildは必須。

## 未決事項 / V2候補

- API errorを共通型へ正規化し、traceIdをUIから提示する仕組み。
- token refreshの自動再試行/401 interceptorの統一。
- `localStorage.clear()` を認証キー限定削除へ変更するか。
- Page/Composable/APIの責務ルールをlint/architecture testで保証するか。
- type-check/lintをblocking gateへ昇格する時期。