# 翌日準備ドメイン仕様

## 対象画面

- 締め処理 → 翌日準備
- 画面URL：`/operation/daily/preparation`
- API：`/api/operation/daily-preparations`

本資料は対象日ごとの従業員配置・現場配車だけでなく、顧客・現場マスターのスナップショット、作業証明伝票のView・ストアド・Jasper PDFまでを追跡対象とする。

## 資料

| 資料 | 内容 |
|---|---|
| [画面からDBまでの処理フロー](daily-preparation-screen-to-db-flow-v1.md) | 対象日、配置、配車、一括保存、作業伝票の処理フロー |
| [入力項目の利用先・システム連携](daily-preparation-field-usage-and-integration-v1.md) | 各項目をマスター・作業伝票・日報との関係まで追跡 |
| [未使用・未連携機能の調査](daily-preparation-unused-and-unintegrated-v1.md) | 状態・備考、日報未連携、整合性検証、不要コード等の棚卸し |
