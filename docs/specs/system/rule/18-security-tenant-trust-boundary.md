# system/rule 詳細設計 18 — Security / Tenant 信頼境界

## 1. 調査方針

実装コードは変更しない。前章 `17-tenant-boundary.md` で未決だった、認証済みUser・JWT tenantId・`X-Tenant-ID`・`TenantContext`・Rule Controller権限の接続を現行mainコードから確認する。

---

## 2. 結論

Rule Controller自体は `SYS_ADMIN` に制限されている。一方、Tenantの情報源がHTTP request内で2系統存在する。

```text
Authorization: Bearer JWT
  └─ JWT tenantId
      └─ JwtAuthenticationFilter
          └─ TenantContext.setTenantId(jwtTenantId)

X-Tenant-ID header
  └─ TenantFilter
      ├─ TenantContext.setTenantId(headerTenantId)
      └─ Hibernate tenantFilter = headerTenantId
```

ここで重要なのは、JWT認証では `username + JWT tenantId` でUserを再取得するため、JWT tenantIdとUser tenantの整合は一定程度確認されること。

しかしTenantFilterは `X-Tenant-ID` を独立して信用し、認証Principalのtenantとの一致確認を行わない。

さらにSecurityConfigではJwtAuthenticationFilterとTenantFilterの両方を `UsernamePasswordAuthenticationFilter` より前に登録しているが、両者間の明示的な順序契約はコード上読み取りにくい。TenantFilterには `@Order(HIGHEST_PRECEDENCE)` があるものの、SecurityFilterChainへ明示追加されたFilterの最終順序をTenant信頼境界の根拠にしない方が安全。

したがってV2ではTenant source of truthを1つに統一する。

推奨:

```text
認証済みSecurityUser / signed JWT tenant claim
          ↓
TrustedTenantResolver
          ↓
TenantExecutionScope
          ↓
JPA + JDBC + Rule
```

`X-Tenant-ID` を使う場合も、単独の信頼値にはせず、認証Userがアクセス可能なTenantかを検証した後にだけeffective tenantへ昇格させる。

---

## 3. Rule Controllerの権限

`RuleMasterController`:

```java
@RequestMapping("/api/system/rules")
@PreAuthorize("hasRole('SYS_ADMIN')")
```

`RuleExecutionController`:

```java
@RequestMapping("/api/system/rules/execution")
@PreAuthorize("hasRole('SYS_ADMIN')")
```

### 確定事項

Rule管理CRUDと手動Rule実行APIは、現状 `ROLE_SYS_ADMIN` が必要。

SecurityConfigでは `@EnableMethodSecurity` が有効なので、Controllerの`@PreAuthorize`は有効化されている。

### 注意

これは「誰がRule APIを操作できるか」の制御であり、「どのtenantデータへアクセスできるか」の保証とは別。

Role authorizationとTenant isolationを混同しない。

---

## 4. JWTのtenantId

`JwtService#generateToken(SecurityUser user)` はJWT claimへ以下を格納する。

```text
userId
 tenantId
roles
subject = username
```

`extractTenantId(token)` はsigned JWT claimからtenantIdを読む。

### 意味

JWT署名が正常なら、クライアントがtenant claimだけを任意改ざんすることはできない。

したがってJWT tenantIdは、単なるHTTP headerより強い信頼源。

---

## 5. JwtAuthenticationFilter

主要処理:

```text
Bearer token
 ↓
extract username
extract tenantId
 ↓
TenantContext.setTenantId(jwtTenantId)
 ↓
loadUserByUsernameAndTenantId(username, jwtTenantId)
 ↓
JWT validation
 ↓
SecurityContext Authentication設定
```

### 良い点

`CustomUserDetailsService#loadUserByUsernameAndTenantId()` は:

```text
UserRepository.findByUsernameAndTenantId(username, tenantId)
```

を使う。

したがってJWTに入っているusernameとtenantIdの組み合わせに対応するUserがDBに存在しなければ認証Principalを作れない。

### ただし

`JwtService#isTokenValid()` 自体はusernameとexpirationのみを確認する。

tenant整合の実質的な保証は、その前段の `loadUserByUsernameAndTenantId(username, tenantId)` に依存している。

---

## 6. SecurityUser

`SecurityUser#getTenantId()` は内部Userの `user.getTenantId()` を返す。

Role/Permissionも内部UserのrolesからSpring Security Authorityへ変換する。

したがって認証完了後はPrincipal側にtrusted tenant情報が存在する。

### V2候補

Rule/Tenant infrastructureはrequest headerを直接参照せず、原則として:

```text
SecurityContext
  → SecurityUser
      → tenantId
```

からeffective tenantを解決する方が一貫する。

---

## 7. TenantFilterとの競合

`TenantFilter` はJWTを見ない。

```text
String tenantId = request.getHeader("X-Tenant-ID");

if (tenantId != null) {
    TenantContext.setTenantId(tenantId);
    session.enableFilter("tenantFilter")
           .setParameter("tenantId", tenantId);
}
```

つまりHeader値とPrincipal tenantの比較がない。

### 問題となる組み合わせ

```text
JWT tenantId = tenant-A
X-Tenant-ID = tenant-B
```

現コードでは2つのFilterがそれぞれTenantContextへ値を設定する。

またHibernate filter値はTenantFilterがheaderから設定する。

このため最終的なTenantContext値・JPA filter値がどちらを向くかを、Filter実行順序の暗黙挙動へ依存させるべきではない。

### 判定

**P0設計修正候補。**

JWTとX-Tenant-IDが異なる場合はfail closedするか、X-Tenant-ID方式自体を廃止しPrincipal tenantへ一本化する。

---

## 8. なぜRuleで特に重要か

RuleはJPAだけでなく `NamedParameterJdbcTemplate` も使う。

JPA:

```text
Hibernate tenantFilter
```

JDBC Rule DataFetcher:

```text
TenantContext.getTenantId()
```

を参照する。

Tenant sourceが不一致になると、同一request内で:

```text
JPAがtenant-B
JDBCがtenant-A
```

のような異なるscopeを向く可能性を構造的に排除できない。

V2ではJPA/JDBC双方が同じ`TenantExecutionScope`のresolved tenantを使用する必要がある。

---

## 9. LoginのTenant決定

`LoginRequest` は:

```text
username
password
```

のみでtenantIdを持たない。

`AuthService#login()` は通常の `AuthenticationManager.authenticate(username,password)` を呼ぶ。

`CustomUserDetailsService#loadUserByUsername()` は:

```text
TenantContext.getTenantId()
```

を取得し、

```text
findByUsernameAndTenantId(username, tenantId)
```

を実行する。

一方 `JwtAuthenticationFilter#shouldNotFilter()` は `/auth/login` を除外する。

TenantFilterはloginを除外していないため、login時のTenantContextは `X-Tenant-ID` に依存する構成になっている。

### 意味

現行Loginではtenant headerが事実上tenant選択に使われる。

これは「ログイン前なのでPrincipalからtenantを決められない」という事情と整合するが、login後requestまで同じheaderをtrusted tenantとして使い続ける必要はない。

---

## 10. Login後の推奨境界

候補:

```text
LOGIN
username/password + tenant selector
 ↓
UserRepository(username, selectedTenant)
 ↓
auth success
 ↓
signed JWT tenantId

AUTHENTICATED REQUEST
signed JWT
 ↓
SecurityUser tenantId
 ↓
TrustedTenantResolver
 ↓
TenantExecutionScope
```

Single-tenant user前提なら、login後の`X-Tenant-ID`は不要にできる。

Multi-tenant membershipを将来許可するなら:

```text
requestedTenant
 ↓
Membership check
 ↓
resolvedTenant
```

を必須にする。

---

## 11. Rule APIとSYS_ADMIN

Rule ControllerはSYS_ADMIN限定なので、通常UserがRule定義を編集・手動実行することは防がれている。

ただしSYS_ADMINであってもtenant境界を越えてよいとは限らない。

### 推奨原則

```text
Authorization:
  この機能を使えるか

Tenant isolation:
  どのデータ領域を使えるか
```

を独立させる。

`SYS_ADMIN`だからtenant filterを無効化する、という暗黙仕様にはしない。

Cross-tenant管理機能が必要なら、別の明示的なsystem scopeを設計する。

---

## 12. HeaderなしAuthenticated Request

JWT filterはJWT tenantIdをTenantContextへ設定する。

しかしTenantFilterは`X-Tenant-ID`がないとHibernate tenantFilterをenableしない。

したがって:

```text
Authorization Bearerあり
X-Tenant-IDなし
```

の場合、JWT側TenantContextは一時的に設定されても、JPA Hibernate tenantFilterが有効になる保証はTenantFilterのheader条件からは得られない。

これは重要。

### 判定

**P0修正候補。**

Authenticated requestのtenant isolationを`X-Tenant-ID`有無へ依存させない。

TenantExecutionScopeがresolved tenantを受け取り、JPA filterを必ずenableする構成にする。

---

## 13. Soft Deleteも同じ問題を持つ

TenantFilterではsoftDeleteFilterも:

```text
X-Tenant-ID != null
```

のブロック内でenableされる。

そのためheaderなしrequestではsoft delete filterもenableされない。

Tenantとsoft deleteは別関心事なので、V2ではライフサイクルを分離した方が安全。

---

## 14. CORS

CorsConfigはallowedHeaders `*`。

したがってBrowser clientから `X-Tenant-ID` を送ること自体はCORS設定上許容される。

これは脆弱性そのものではないが、headerをtrusted tenant sourceにしてはいけない理由を補強する。

---

## 15. UserRepository

認証で使う安全側method:

```text
findByUsernameAndTenantId(username, tenantId)
```

が存在する。

一方UserRepository全体にはtenant条件なしmethodも複数ある。

Rule調査の範囲では、JWT authentication pathがusername+tenantIdを使うことを確認した。

User管理全体のtenant isolationは別system/security調査対象とする。

---

## 16. Trusted Tenantの定義

V2では以下を区別する。

```text
requestedTenant
  クライアントが要求したtenant
  未検証

principalTenant
  認証済みPrincipalに結びつくtenant

resolvedTenant
  Authorization/Membership確認後に確定したtenant
  JPA/JDBCが唯一使ってよいtenant
```

Ruleへ渡すのは`resolvedTenant`だけ。

Rule ParameterとしてtenantIdを公開しない。

---

## 17. TenantExecutionScope案

概念:

```java
TenantExecutionScope.run(resolvedTenant, () -> {
    // Rule / JPA / JDBC
});
```

責務:

```text
1. resolvedTenant必須確認
2. TenantContext set
3. Hibernate tenantFilter enable
4. soft-delete scopeは別途適用
5. JDBC Rule Queryへ同tenantを供給
6. finallyで必ずclear/disable
7. nested scopeの扱いを定義
8. tenant switchは明示許可なし禁止
```

### 重要

このAPIを業務ServiceがtenantIdを自由入力するためのAPIにはしない。

HTTPではTrustedTenantResolver、batch/schedulerではtrusted job metadataからresolved tenantを作る。

---

## 18. Rule ParameterからtenantIdを消す

前章の結論をSecurity調査でも維持する。

Rule作者が設定するParameter:

```text
employeeId
targetDate
hours
amount
```

Rule作者が設定しないもの:

```text
tenantId
userId for security
role
raw tenant predicate
```

TenantはRuleの業務入力ではなくInfrastructure scope。

---

## 19. Fail Closed条件

V2では少なくとも次をfail closedにする。

```text
resolvedTenantなしでTENANT scoped Rule実行
JWT tenantとrequested tenant不一致
membership未確認tenantへのswitch
TENANT Catalogでtenant scope生成不能
JPA tenant filterを有効化できない
JDBC tenant predicateを生成できない
background jobのtenant metadata欠落
```

---

## 20. 必要なIntegration Test

```text
1. JWT tenant-A + header tenant-B → reject
2. JWT tenant-A + headerなし → tenant-AでJPA/JDBC両方scope
3. JWT tenant-A + header tenant-A → tenant-A
4. tenant-A SYS_ADMINでもtenant-B Ruleを取得不可
5. tenant-A SYS_ADMINでもtenant-B Catalogを取得不可
6. tenant-A Rule JDBCからtenant-B row取得不可
7. forged/invalid JWT tenant claim → 401
8. JWT tenantにUserが存在しない → authentication不可
9. login時tenant選択と発行JWT tenant一致
10. headerなしでもsoft deleteが期待通り適用
11. RuleExecutionServiceをbatchから呼ぶ場合もtenant scope必須
12. TenantContextがrequest/job終了後に残らない
```

---

## 21. 優先度

### P0

- Tenant source of truth統一
- JWT tenantとX-Tenant-ID競合排除
- authenticated requestでheaderなしでもJPA tenant filterを保証
- JPA/JDBCで同一resolved tenantを保証
- TENANT scoped Ruleのfail closed

### P1

- Login時requested tenantと認証後principal tenantの概念整理
- TenantExecutionScope導入
- softDelete filter lifecycleをTenant headerから分離
- SYS_ADMINとcross-tenant権限を分離

### P2

- Multi-tenant membershipを将来導入する場合のTenant switch API
- system/global scopeの明示モデル

---

## 22. 最終判定

### 現行で確認できた安全側仕様

- JWTにtenantIdが署名付きclaimとして入る
- JWT authentication時は `username + tenantId` でUserを再取得する
- Rule ControllerはSYS_ADMIN限定
- SecurityUserはDB UserのtenantIdを保持する

### 現行で残るTenant gap

- TenantFilterが`X-Tenant-ID`をPrincipal照合なしで使用
- JWT tenantとheader tenantという2つのsource of truthが存在
- headerなしではHibernate tenantFilterがenableされない
- softDelete filterもheader有無へ結合
- Rule JDBCとJPAが同じresolved tenantを使うことを単一Infrastructureで保証していない

### V2確定候補

```text
Rule tenantId Parameter廃止
       +
TrustedTenantResolver
       +
TenantExecutionScope
       +
JPA/JDBC自動tenant scope
       +
TENANT scopeはContextなしFail Closed
```

---

## 23. 次段

Security/Tenant信頼境界の確認はここで一旦閉じる。

次は `system/rule` 全体の総まとめを作成し、これまでの各調査章を統合して以下をV2設計案として整理する。

```text
Rule architecture
Rule lifecycle / execution
Fact / Parameter / Catalog
Base Calculator
Annotation catalog
RuleFunctions / RuleUtils
BigDecimal / scale / RoundingMode / MathContext
Version / Revision
Audit / Snapshot
Tenant完全自動化
Security trust boundary
Migration順序
P0/P1/P2
```
