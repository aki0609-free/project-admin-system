# 入金確認表 Spreadsheet台帳 V1

## 1. 目的

月次締めで確定した顧客別請求に対し、入金予定と実際の入金結果を継続管理する。

- 請求情報の正本は`customer_transactions`とする。
- Spreadsheet JSONは閲覧・入力・印刷用の台帳とする。
- 月次締め後も、入金額・手数料・相殺・備考は更新できる。
- 通常の帳票履歴基盤には載せず、台帳基盤で管理する。
- S3上の月次JSONは再生成・保存時に同一パスへ更新する。

## 2. 画面操作

1. 「締め処理 → 台帳」を開く。
2. 対象となる請求月を選択する。
3. 「入金確認表」を生成または開く。
4. 入金結果を入力する。
5. 保存する。
6. 必要に応じてSpreadsheetの印刷機能で印刷する。

対象月は「入金月」ではなく「請求対象月」である。たとえば2月請求で入金予定日が3月・4月の場合も、2月の入金確認表に表示する。

## 3. 正本と保存先

| データ | 役割 | 保存先 |
|---|---|---|
| 請求・入金状態 | 業務上の正本 | MySQL `customer_transactions` |
| 最新の台帳 | 画面表示・入力・印刷 | S3 Spreadsheet JSON |
| 台帳テンプレート | 外観・列幅・基本書式 | S3 Spreadsheet JSON |

生成台帳：

```text
documents/generated-reports/
  ledgers/{tenantId}/RECEIPT_CONFIRMATION/{yyyy-MM}/
    RECEIPT_CONFIRMATION-{yyyy-MM}.json
```

テンプレート：

```text
documents/templates/
  ledgers/default/RECEIPT_CONFIRMATION/template.json
```

初回起動時にテンプレートが存在しない場合だけ、デプロイ資産の初期テンプレートをS3またはLOCALへ登録する。管理画面で編集済みのテンプレートは上書きしない。

## 4. 入力可能項目

| 項目 | 編集 | DB反映 |
|---|---:|---|
| 業者名 | 不可 | 顧客マスタ・請求確定値を表示 |
| 締め日 | 不可 | 請求確定値を表示 |
| 支払日 | 不可 | 請求確定値を表示 |
| 請求金額 | 不可 | 請求確定値を表示 |
| 入金予定日 | 不可 | 請求確定値を表示 |
| 入金額 | 可 | `paid_amount` |
| 手数料 | 可 | `fee` |
| 相殺 | 可 | `offset_amount` |
| 合計金額 | 不可 | 入金額＋手数料＋相殺 |
| 備考 | 可 | `note` |

画面では入力可能セルだけロックを解除する。API側でも非表示の取引ID・顧客ID・対象月・請求金額を検証し、不整合な保存を拒否する。

## 5. 入金状態の計算

```text
決済済み金額 = 入金額 + 手数料 + 相殺
残額         = 請求金額 - 決済済み金額
```

| 条件 | payment_status |
|---|---|
| 決済済み金額が0以下 | `UNPAID` |
| 残額が正 | `PARTIAL` |
| 残額が0 | `PAID` |
| 残額が負 | `OVERPAID` |

入金額・手数料・相殺のいずれかが入力され、入金確認日が未設定の場合、保存日を`confirmed_payment_date`へ設定する。3項目がすべて0の場合は入金確認日を未設定へ戻す。

## 6. 締め処理との関係

- 月次締めは請求金額・入金予定日などを`customer_transactions`へ確定する。
- 入金確認表は、その確定済み取引をViewから読み取って生成する。
- 月次締め後も入金処理は続くため、この台帳だけは締め後編集を許可する。
- 再締め時、`PAID`の取引は月次締めから上書きしない。
- 入金確認表から請求金額を変更することはできない。

## 7. DB・実装資産

```text
backend/src/main/resources/sql/system/excelbook/receipt_confirmation_v1.sql
backend/src/main/java/com/project/backend/features/operation/book/service/ReceiptConfirmationSpreadsheetRenderer.java
backend/src/main/java/com/project/backend/features/operation/book/service/ReceiptConfirmationSpreadsheetEditHandler.java
backend/src/main/resources/spreadsheet/receipt_confirmation_template.json
```

設定値：

| 設定 | 値 |
|---|---|
| bookCode | `RECEIPT_CONFIRMATION` |
| rendererKey | `RECEIPT_CONFIRMATION_V1` |
| dataSourceCode | `RECEIPT_CONFIRMATION_LEDGER` |
| source View | `vw_receipt_confirmation_ledger` |
| selectionMode | `NONE` |
| generationUnit | `ONE_FILE` |
| paperSize | `A4` |
| orientation | `LANDSCAPE` |
| fitToOnePage | `true` |

## 8. 環境反映

DB資産は既存のランタイムスキーマ更新スクリプトへ組み込み済みである。

```bash
AWS_PROFILE=project-admin-terraform \
infrastructure/scripts/database/apply_runtime_schema_upgrade.sh
```

アプリケーションを再ビルド・再デプロイすると、初回起動時に未登録のテンプレートが配置される。

## 9. 本番適用前の確認

- 代表顧客で請求金額・締め日・支払日・入金予定日を原本と照合する。
- 入金額＋手数料＋相殺が請求金額と一致したとき`PAID`になることを確認する。
- 一部入金が`PARTIAL`、過入金が`OVERPAID`になることを確認する。
- 締め後にも入金欄を保存でき、請求金額は変更できないことを確認する。
- 3月・4月など入金予定月ごとの小計と総合計を確認する。
- A4横・1ページ幅の印刷結果を確認する。

## 10. V1制約・将来拡張

- V1では入金確認日をSpreadsheetの列として直接編集せず、初回の入金入力日を自動設定する。
- 入金日を明示入力する要件が追加された場合は、入力列と編集ハンドラーを追加する。
- 複数回の分割入金履歴そのものは保持せず、`customer_transactions`に最新累計を保持する。入金明細監査が必要になった場合は、別の入金明細テーブルを追加する。
- 入金合計は`入金額＋手数料＋相殺額＋その他調整額`とする。
- `その他調整額`は正負を許可し、0以外の場合は備考への調整理由入力を必須とする。
