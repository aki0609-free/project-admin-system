# Tenant境界 V1安定化

## 1. 目的

認証前と認証後でTenant IDの信頼元を明確に分離し、リクエストヘッダーだけで別Tenantのデータへアクセスできないようにする。

対応日：2026-08-22

## 2. 変更前の問題

変更前は次の2つのフィルターが、同じ`TenantContext`を個別に設定していた。

```text
TenantFilter
  X-Tenant-IDをTenantContextへ設定

JwtAuthenticationFilter
  JWTのtenantIdをTenantContextへ設定
```

問題点：

- `X-Tenant-ID`とJWTのTenantが異なる場合の照合がない
- 認証後もクライアント指定ヘッダーがTenant境界へ影響する
- 2つのフィルターの実行順が明確に固定されていない
- Spring Security外のServlet Filterとして二重登録される可能性がある
- JWTがなくてもヘッダーだけでTenantContextが設定される

## 3. V1の信頼境界

### 3.1 ログイン時

ログイン前はJWTが存在しないため、`X-Tenant-ID`をログイン先Tenantの指定として使用する。

```text
POST /auth/login
X-Tenant-ID: default
```

ログイン時は`X-Tenant-ID`を必須とする。

Tenant IDは次の文字だけを許可する。

- 英数字
- `.`
- `_`
- `-`
- 最大100文字

### 3.2 認証後

認証後はJWTから検索・検証された`SecurityUser.tenantId`だけを正とする。

```text
JWT tenantId
  ↓
CustomUserDetailsServiceで同一TenantのUserを取得
  ↓
SecurityUser.tenantId
  ↓
TenantFilter
  ↓
TenantContext / Hibernate Filter
```

認証後の`X-Tenant-ID`はTenant選択には使用しない。

- ヘッダーなし：JWT Tenantで処理
- JWTと同じヘッダー：互換性のため許可
- JWTと異なるヘッダー：HTTP 403
- 不正形式のヘッダー：HTTP 400

### 3.3 未認証の保護API

未認証状態で保護APIへ`X-Tenant-ID`だけを送っても、TenantContextは設定しない。

その後、Spring Securityが未認証リクエストとして拒否する。

## 4. フィルター順序

Spring Security内の順序を次へ固定した。

```text
JwtAuthenticationFilter
  ↓
TenantFilter
  ↓
UsernamePasswordAuthenticationFilter
  ↓
Controller / Service
```

ログインではJWT Filterをスキップするが、TenantFilterがログイン用ヘッダーを検証してTenantContextへ設定する。

認証済みAPIではJWT Filterが先にSecurityUserを確定し、TenantFilterがそのTenantを利用する。

## 5. Servlet Filter二重登録の防止

`JwtAuthenticationFilter`と`TenantFilter`はSpring Beanだが、通常Servlet Filterとしては無効化する。

実行経路は`SecurityFilterChain`だけに限定する。

これにより、`@Order`やServlet Container側の順序に依存しない。

## 6. Hibernate Filter

Tenant確定後、TenantFilterが次を有効化する。

- `tenantFilter`
- `softDeleteFilter`

処理終了時は必ず次を行う。

- `TenantContext.clear()`
- `tenantFilter`無効化
- `softDeleteFilter`無効化

スレッド再利用時にTenantが残らないよう、`finally`で解放する。

## 7. テスト保証

次を単体テストで確認した。

1. 認証後はヘッダーなしでもSecurityUserのTenantを使用する
2. JWT Tenantと異なるヘッダーを403で拒否する
3. ログイン時は検証済みヘッダーを使用する
4. ログイン時にTenantヘッダーがなければ400とする
5. 未認証の保護APIではTenantヘッダーを信頼しない
6. 無効JWTは従来どおり401とする
7. Spring Security設定変更後もアプリケーションContextが構築できる

## 8. フロントエンドとの互換性

現在のフロントエンドは全APIへ次を送信している。

```text
X-Tenant-ID: default
```

現在のFuyo V1 Tenantも`default`なので互換性は維持される。

認証後はこのヘッダーを削除しても動作する。将来複数Tenantへ対応する場合は、ログイン画面でTenantを選択し、認証後はJWTだけを使用する構成へ変更する。

## 9. Rule DataSourceとの関係

Rule DataSourceのtenant値は、Ruleパラメータではなく`TenantContext`から強制設定する。

今回の変更により、認証後の`TenantContext`はSecurityUserを信頼元とする。

2026-08-22に以下まで対応した。

1. CatalogなしRule DataSourceの登録・実行禁止
2. Tenant対象Catalogの`:tenantId`条件を基盤側で検査
3. Column Mappingなしの`SELECT *`禁止
4. TenantContextなしのTenant対象DataSourceをFail Closed
5. リクエストのtenantIdを無視し、TenantContextの値を強制設定
6. Rule本体の読込を`ruleName`単独検索から`TenantContext + ruleName`検索へ変更

6により、複数Tenantへ同名Ruleが登録されていても別TenantのRuleを取得せず、検索結果重複による実行失敗も防止する。TenantContextがないRule実行はFail Closedとする。
