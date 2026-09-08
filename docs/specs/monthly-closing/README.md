# 月次締め仕様

## 対象画面

- 締め処理 → 月次管理
- 画面URL：`/operation/monthly`
- API：`/api/operation/monthly`

自社の給与締日を基準に、帳票・台帳と法定預り金返金をVersion付きで確定する。請求書・注文書は顧客請求締めの対象であり、自社月次締めから除外する。

## 資料

| 資料 | 内容 |
|---|---|
| [画面から確定履歴・帳票までの処理フロー](monthly-closing-screen-to-db-flow-v1.md) | 期間解決、実行履歴、帳票・台帳、再締め |
| [項目の利用先・システム連携](monthly-closing-field-usage-and-integration-v1.md) | 締め本体、実行、項目、出力定義、保存ファイル |
| [未使用・未連携・不整合調査](monthly-closing-unused-and-unintegrated-v1.md) | 概要期間差、項目一括完了、未接続API等 |
| [月次給与明細の計算・表示・履歴確定フロー](monthly-pay-slip-item-flow-v1.md) | 月次給与明細の可変項目と確定経路 |
