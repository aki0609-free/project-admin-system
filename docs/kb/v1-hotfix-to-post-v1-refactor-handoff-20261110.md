# V1緊急修正からPost-V1リファクタリングへの引継ぎ

## 目的

V1リリース後に本番運用で判明した修正を、`refactor/post-v1-stabilization` へ安全に取り込むための記録です。

V1側とPost-V1側ではDB・Policy・Batch周辺の構造が異なるため、ブランチ全体のマージではなく、修正意図とテストを確認しながら差分単位で移植します。

## 対象ブランチ

- V1基準: `v1.0.0` / `1000bc5`
- V1請求単価・距離・丸め修正: `fix/v1-customer-rate-distance-rounding-20261109`
- V1認証・セッション回復修正: `fix/v1-auth-session-recovery-20261110`
- 移植先: `refactor/post-v1-stabilization`

## V1修正1: 請求単価・現場距離・請求丸め

実装コミット: `3af8350` (`fix: support decimal distance and invoice ceiling`)

### 修正内容

- 請求単価の適用開始日に過去日を指定できるように修正
- 現場までの距離を小数第1位まで保存・表示可能に修正
- 請求金額を最終金額で1円単位切り上げに統一
- DBマイグレーション、バックエンド、フロント、テストを更新

### Post-V1取り込み時の注意

- Post-V1側のテーブル定義とFlyway履歴を先に比較し、V1用DDLをそのまま適用しない
- Policy基盤に請求丸めが移行済みの場合、二重丸めを発生させない
- 距離の型がAPI、Entity、View、帳票データ元で一貫しているか確認する

## V1修正2: ログインエラー・セッション自動回復

対象ブランチ: `fix/v1-auth-session-recovery-20261110`

### 障害根拠

- アクセストークンの有効期間は1時間
- フロントエンドが更新トークンを使用しておらず、1時間経過後の顧客APIが401になっていた
- 別PCで再ログインすると保存済みデータが見えたため、データ消失ではなくセッション切れによる取得失敗だった
- 認証情報不一致の例外が汎用例外処理へ流れ、500として返されていた
- バックエンドの更新処理がUUID形式の更新トークンをJWTとして解析しており、更新API自体も正常に動作しない状態だった

### 修正内容

- 認証情報不一致を `401 AUTH_INVALID_CREDENTIALS` で返す
- 更新トークンに保存された `userId` と `tenantId` から利用者を復元する
- 無効・期限切れの更新トークンを `401 AUTH_TOKEN_EXPIRED` で返す
- 無効化・削除済み利用者の更新を拒否する
- OpenAPIクライアント、Axios、直接ファイル取得を共通のセッション回復処理へ統一する
- 401時は更新後に元のリクエストを1回だけ再実行する
- 同時401時は更新APIを1回だけ実行する（single-flight）
- 更新失敗時は認証情報を削除し、ログイン画面へ遷移して期限切れを表示する
- ログイン失敗時に、認証情報不一致と通信・サーバー障害を分けて表示する

### 主な変更箇所

バックエンド:

- `GlobalExceptionHandler`
- `AuthService`
- `RefreshTokenService`
- `CustomUserDetailsService`
- `UserRepository`
- `InvalidRefreshTokenException`

フロントエンド:

- `shared/auth/session/authSession.ts`
- `app/plugins/apiClient.ts`
- `app/plugins/axiosApiClient.ts`
- `shared/auth/store/useAuthStore.ts`
- `shared/api/http.ts`
- `features/system/report/api/mutations/useTestPrintReportMutation.ts`
- `features/auth/composables/useLogin.ts`
- `main.ts`

### Post-V1取り込み時の注意

- Post-V1側に認証GatewayやHTTPクライアント抽象化がある場合、V1ファイルを丸ごと上書きせず、その契約へ移植する
- テナント解決方法が `default` 固定から変更されている場合、固定ヘッダーを廃止して現在のTenant契約を使用する
- 更新トークンのローテーションを追加する場合も、同時401のsingle-flightと再試行1回の制約を維持する
- 顧客入力内容の一時保存・画面遷移ガードは今回のV1修正対象外。別課題として扱う

## 移植手順（推奨）

1. `refactor/post-v1-stabilization` を最新化する
2. `3af8350` の変更をDB・Policy差分を確認しながら移植する
3. `fix/v1-auth-session-recovery-20261110` の認証修正をHTTP基盤の現行設計へ移植する
4. V1で追加したテストケースをPost-V1側へ移植する
5. ログイン、1時間経過相当の401回復、同時401、更新期限切れ、ファイル出力を確認する
6. AWS反映前にFlyway履歴と本番スキーマ差分を確認する

## 回帰テスト観点

- 正しい認証情報でログインできる
- 誤った認証情報は500ではなく401となる
- アクセストークン期限切れ後も操作が継続できる
- 複数APIが同時に401でも更新APIは1回だけ呼ばれる
- 更新トークン期限切れ時はログイン画面へ戻り、理由が表示される
- 帳票・Excel・PDF等の直接ダウンロードも自動回復する
- ログアウト後に更新処理が走らない
- 別利用者・別テナントの更新トークンとして扱われない
