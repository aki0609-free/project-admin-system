# 顧客取引管理 V1 リファクタリング

## 1. 目的

月次締めで生成された顧客別請求に対し、入金状況を確認・確定する。
取引レコードの画面からの新規作成は行わず、月次締めを正規の生成経路とする。

## 2. 金額と入金状態

| 項目 | 内容 |
| --- | --- |
| 取引金額 | 月次締めで確定した請求額 |
| 入金額 | 銀行等で実際に確認した金額 |
| 手数料 | 顧客負担として決済済みに含める振込手数料 |
| 相殺額 | 別債権・債務との相殺額。0以上 |
| その他調整額 | 上記で表現できない差額。正負の円単位整数 |
| 入金合計 | 入金額＋手数料＋相殺額＋その他調整額 |
| 残額 | 取引金額－入金合計 |

入金状態は次の規則で自動判定する。

- 入金合計が0以下：`UNPAID`
- 残額が正：`PARTIAL`
- 残額が0：`PAID`
- 残額が負：`OVERPAID`

その他調整額が0以外の場合、備考への調整理由入力を必須とする。
画面とサーバーの両方で検証し、APIの直接実行でも理由なしの調整を拒否する。

## 3. データ生成・更新経路

```text
月次締め
  -> CustomerTransactionCommandService.upsertFromMonthlyClosing
  -> customer_transactions
  -> 取引管理／入金確認表で入金情報を更新
```

- 顧客・対象月の組み合わせは一意。
- 月次締め由来の取引は取引管理から削除しない。
- 入金済・過入金の取引は再締めで請求額を上書きしない。
- 入金確認表Spreadsheetからの更新にも同じ入金判定サービスを使用する。

## 4. 主要コード

| 層 | ファイル・役割 |
| --- | --- |
| Entity | `CustomerTransaction.java`：請求・入金最新状態 |
| API DTO | `CustomerPaymentConfirmRequest.java`、`CustomerTransactionResponse.java` |
| Service | `CustomerTransactionCommandService.java`：検証・入金状態判定 |
| Mapper | `CustomerTransactionMapper.java` |
| 画面 | `CustomerTransactionPage.vue` |
| 入金Dialog | `PaymentConfirmDialog.vue` |
| 一覧定義 | `useCustomerTransactionColumns.ts` |
| DB差分 | `customer_transaction_adjustment_v1.sql` |
| ローカルfixture | `demo_customer_transaction_fixture.sql` |

## 5. ローカルテストデータ

`E2E 月間集計検証顧客`へ次の4パターンを再実行可能なSQLで投入する。

- 未入金
- 一部入金
- 入金済（入金額＋手数料）
- 入金済（その他調整額を含む）

本番のruntime schemaにはfixtureを含めない。

## 6. テスト

- `CustomerTransactionCommandServiceTest`
  - 手数料・相殺・調整額を含む判定
  - 一部入金
  - 調整理由必須
  - 再締め・重複・削除制約
- `ReceiptConfirmationSpreadsheetRendererTest`
- `ReceiptConfirmationSpreadsheetEditHandlerTest`
- `customer-transaction-ui.spec.ts`
  - 4状態の表示
  - その他調整額の数値計算
  - 調整理由未入力時の保存抑止
