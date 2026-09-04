# 手当マスター管理ドメイン仕様

## 対象画面

- マスター管理 → 手当
- API：`/api/master/allowances`

手当本体だけでなく、共通給与項目Policy、従業員別適用、Rule、日報、残高、月次給与・帳票までを追跡する。

## 資料

| 資料 | 内容 |
|---|---|
| [画面からDBまでの処理フロー](allowance-management-screen-to-db-flow-v1.md) | 一覧・詳細・登録・更新・削除と関連クラス・テーブル |
| [入力項目の利用先・システム連携](allowance-field-usage-and-integration-v1.md) | マスター・Policy・動的パラメーターを日報・月次まで追跡 |
| [未使用・未連携機能の調査](allowance-unused-and-unintegrated-v1.md) | 課税設定、月次候補、残高Policy等の現行到達状況 |
