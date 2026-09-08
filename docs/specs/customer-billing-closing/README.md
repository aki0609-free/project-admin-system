# 顧客請求締めドメイン仕様

## 対象画面

- 締め処理 → 顧客請求締め
- 画面URL：`/operation/customer-billing`
- API：`/api/operation/customer-billing`

顧客ごとの締日・請求期間で請求額を確定し、請求書・注文書を保存するとともに顧客管理の取引へ請求額を同期する。自社給与の月次締めとは別の締め単位である。

## 資料

| 資料 | 内容 |
|---|---|
| [画面から請求履歴・取引までの処理フロー](customer-billing-closing-screen-to-db-flow-v1.md) | 対象抽出、個別/一括締め、帳票、再締め、取引同期 |
| [項目の利用先・システム連携](customer-billing-closing-field-usage-and-integration-v1.md) | 顧客締日、請求単価、Version、帳票ファイル、取引項目 |
| [未使用・未連携・不整合調査](customer-billing-closing-unused-and-unintegrated-v1.md) | 税率固定、Preview期間、失敗履歴等の課題 |
