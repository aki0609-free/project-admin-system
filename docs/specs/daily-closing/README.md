# 日次管理ドメイン仕様

## 対象画面

- 締め処理 → 日次管理
- 画面URL：`/operation/daily`
- API：`/api/operation/daily-payments`

V1確定仕様では、日次管理は日報から算出した日次前払い額と帳票を確認・発行する読取専用画面とする。日報を唯一の計算元とし、日次給与明細の生成ファイル履歴をその時点の支払証明として保持する。

画面・API・日次帳票View・月次前払いViewは日報基準へ統一済みである。既存DBとの互換のため`daily_payments`テーブルだけは残すが、V1の正本として使用しない。

## 資料

| 資料 | 内容 |
|---|---|
| [画面からDB・帳票までの処理フロー](daily-closing-screen-to-db-flow-v1.md) | 日報から対象者を組み立て、保存・日次帳票へ渡す流れ |
| [入力項目の利用先・システム連携](daily-closing-field-usage-and-integration-v1.md) | 支払日、予定額、実績額、状態、帳票項目の用途 |
| [未使用・未連携・不整合調査](daily-closing-unused-and-unintegrated-v1.md) | 状態固定、Snapshot欠落、未使用API等の棚卸し |
