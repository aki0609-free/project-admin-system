# 顧客管理 未使用・未連携機能の調査 V1

ドメイン：顧客管理

## 1. 目的

顧客情報・取引管理について、現行リポジトリ内で次の状態にあるものを区別して整理する。

- 定義されているが参照元がないコード
- バックエンドAPIはあるが、現在の画面から到達できない処理
- DBへ保存されるが、想定される業務機能へ連携されていない項目
- 選択・登録はできるが、後続計算が未完成の機能

本資料は削除指示ではない。V1では「削除」「利用停止」「将来用として維持」「連携を実装」のいずれにするかを判断するための棚卸しである。

## 2. 判定区分

| 区分 | 意味 |
|---|---|
| 未参照 | 定義ファイル以外から参照されていない |
| 画面未接続 | APIやServiceは存在するが、現行画面に呼出経路がない |
| 保存のみ | 登録・表示はできるが、計算・帳票・メール等では利用しない |
| 未連携 | 連携を意図した項目だが、対象機能へ値を渡す処理がない |
| 後続未対応 | 入力は可能だが、その値を扱う後続処理が未完成 |
| 現行仕様 | 未使用ではなく、意図的に画面入力を許可していない |

## 3. フロントエンドの未参照コード

リポジトリ内のimport・呼出を検索した結果、次の定義は現在の画面から使われていない。

| 対象 | 区分 | 現状 | V1での扱い候補 |
|---|---|---|---|
| `useCreateCustomerTransactionMutation.ts` | 未参照 | 取引新規APIを呼ぶMutationだが、取引管理画面は照会・入金確認のみ | 手入力取引を許可しないなら削除候補 |
| `useUpdateCustomerTransactionMutation.ts` | 未参照 | 取引全体更新用Mutation。現行画面は入金確認専用Mutationを使う | 同上 |
| `useDeleteCustomerTransactionMutation.ts` | 未参照 | 取引削除用Mutation。現行画面に削除操作なし | 同上 |
| `CustomerTransactionReviewDialog.vue` | 未参照 | 旧取引確認Dialogと考えられるが、画面へ配置されていない | 現行`PaymentConfirmDialog`へ統一後に削除候補 |
| `useCustomerTransactionReview.ts` | 未参照 | 上記Dialog用の状態管理。Dialogも未配置 | 同上 |
| `CustomerTransactionSavePayload` | 未参照 | `CustomerTransaction`の型別名のみ | 削除候補 |
| `useCreateCustomerSiteBillingRateMutation.ts` | 未参照 | 単価1件追加API用 | 現行一括保存へ統一するなら削除候補 |
| `useUpdateCustomerSiteBillingRateMutation.ts` | 未参照 | 単価1件更新API用 | 同上 |
| `useDeleteCustomerSiteBillingRateMutation.ts` | 未参照 | 単価1件削除API用 | 同上 |
| `useCustomerOptionsQuery.ts` | 未参照 | 顧客・現場候補Queryだが、現行は`useCustomerMasterStore`が候補を保持 | 候補取得方式をPiniaへ統一後に削除候補 |

注意：ファイル削除前にStorybook、テスト、外部利用コードも再検索すること。本調査は現在の本リポジトリ内を対象とする。

## 4. 取引管理の画面未接続API

### 4.1 取引CRUD

`CustomerTransactionController`には取引の作成・更新・削除APIがあり、`CustomerTransactionCommandService`にも処理がある。しかし、`CustomerTransactionPage.vue`が実際に使う更新処理は入金確認だけである。

```text
現在の画面経路
  GET 取引一覧
  PUT confirm-payment

画面から到達できない経路
  POST 取引作成
  PUT  取引全体更新
  DELETE 取引削除
```

取引は月次・顧客締めから生成し、利用者が取引管理画面から元データを作らない仕様であるため、これは現行仕様と整合する。ただし、未接続APIを公開したままにするかはV1のAPI面積縮小として判断が必要である。

### 4.2 月次締め取引生成API

`POST /api/customer-transactions/from-monthly-closing`はControllerに存在するが、本リポジトリ内の締め処理は`CustomerTransactionCommandService.upsertFromMonthlyClosing()`をServiceとして直接呼ぶ。HTTP APIの呼出元は確認できない。

- 外部バッチがHTTPで呼ぶ契約なら維持する。
- システム内部からしか生成しないなら、Controller公開は削減候補である。

### 4.3 請求単価の個別CRUD

バックエンドは単価1件の取得・作成・更新・削除APIを持つ。一方、現行の請求単価タブは`bulk-save`へ統一されている。

`CustomerSiteBillingRateQueryService.findByCustomerSiteId()`も現在の画面・Serviceからの呼出を確認できない。個別APIを残す場合だけ必要となる可能性がある。

## 5. 保存のみ・未連携の入力項目

### 5.1 顧客基本情報

| 項目 | 状態 | 影響 |
|---|---|---|
| `jobType` | 保存のみ | 請求単価の`jobCode`、日報の職種とは連動しない |
| `contractFlag` | 連携済み | `ACTIVE`だけを顧客・現場候補および顧客締め対象にする |
| `representativeName` | 画面内利用 | 現行の請求書・注文書SQLでは利用先を確認できない |
| `phone` | 画面内利用 | 帳票・メール宛先には連携しない |

`jobType`をマスター選択と誤認しないこと。現状は顧客のメモ的属性であり、請求単価の職種体系とは別物である。

### 5.2 現場情報

| 項目 | 状態 | 影響 |
|---|---|---|
| `contactPersonName` | 保存のみ | 顧客管理画面以外から参照しない |
| `contactPersonPhone` | 保存のみ | 電話・通知機能へ連携しない |
| `contactPersonEmail` | 未連携 | メール宛先グループへ登録されない |

### 5.3 顧客社員

| 項目 | 状態 | V1の連携 |
|---|---|---|
| `email` | 連携済み | 顧客別の`mail_recipient_group` / `mail_recipient`へ同期する |
| `invoiceToFlag` | 連携済み | ONの場合、請求書送信先TOとして有効化する |
| `invoiceCcFlag` | 連携済み | ONの場合、請求書送信先CCとして有効化する |

グループキーは`CUSTOMER_INVOICE_{customerId}`、グループ名は
`請求書送付先：{顧客名}`で自動生成する。メールアドレスがありTO/CC未指定の担当者も、
誤送信防止のため無効な宛先として同期する。メール管理から手動追加した宛先は同期時も保持する。

### 5.4 請求単価

| 項目 | 状態 | 影響 |
|---|---|---|
| `note` | 保存のみ | 請求計算・帳票には使用しない |
| `MONTHLY` | 対応済み | 顧客締め期間内で同じ請求単価につき基本料金を1回計上する |
| `FIXED` | V1廃止 | 選択肢・型から除外し、既存データは`MONTHLY`へ移行する |

`MONTHLY`の残業・深夜・休日・通勤費は、基本料金とは別に各日報の実績を合算する。

## 6. 状態値はあるが遷移処理がないもの

`CustomerPaymentStatus`には次の6状態がある。

- `UNPAID`
- `PARTIAL`
- `PAID`
- `OVERPAID`
- `CANCELED`
- `WRITE_OFF`

現在の入金確認処理が金額から設定するのは最初の4状態だけである。`CANCELED`と`WRITE_OFF`へ変更する画面操作・専用Commandは確認できない。

既存DBに値が入っている可能性や将来利用を否定するものではないが、現行画面だけでは請求取消・貸倒へ遷移できない。

## 7. 削除対象ではない現行仕様

次は「使われていない」のではなく、意図的な設計である。

- 取引管理から取引元データを新規入力しない。
- 取引管理では、入金日・入金額・手数料・相殺額・調整額・理由だけを更新する。
- 請求単価は行単位APIではなく一括保存を使う。
- 顧客名・現場名・単価は日報等へスナップショットし、過去データをマスター変更で自動更新しない。

## 8. V1判断一覧

| 優先度 | 課題 | 推奨判断 |
|---|---|---|
| 完了 | `MONTHLY` / `FIXED`単価 | `MONTHLY`へ統一し顧客締め期間単位の計算を実装 |
| 完了 | 顧客社員TO/CCがメールへ未連携 | 顧客別グループの自動生成・同期を実装 |
| 中 | 取引CRUD APIとMutationが画面未接続 | 外部利用の有無を確認し、なければV1後に削減 |
| 中 | 単価個別CRUDと一括保存が併存 | 一括保存を正式経路として整理 |
| 低 | 顧客・現場の保存のみ項目 | 必要性を業務確認し、不要ならV2前に削減 |
| 低 | `CANCELED` / `WRITE_OFF`の遷移なし | V1対象外なら画面に説明せず内部予約値として維持 |

現場・顧客担当者のIDとメール宛先グループの数値IDはDB内部の自動採番値として維持し、
V1画面では表示・入力させない。

## 9. 調査した主な実装

```text
frontend/src/features/customer/
backend/src/main/java/com/project/backend/features/customer/
backend/src/main/resources/sql/system/report/invoice/monthly_invoice_foundation_v1.sql
backend/src/main/java/com/project/backend/features/operation/monthly/
backend/src/main/java/com/project/backend/features/system/mail/
```
