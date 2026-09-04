# 控除マスター管理ドメイン仕様

## 対象画面

- マスター管理 → 控除
- API：`/api/master/deductions`

本資料は、控除マスター単体だけでなく、共通給与項目Policy、従業員別適用、Rule、日報、残高、月次給与・帳票までを追跡対象とする。

## 資料

| 資料 | 内容 |
|---|---|
| [画面からDBまでの処理フロー](deduction-management-screen-to-db-flow-v1.md) | 一覧・詳細・登録・更新・削除と関連クラス・テーブル |
| [入力項目の利用先・システム連携](deduction-field-usage-and-integration-v1.md) | マスター・Policy・動的パラメーターを日報・月次まで追跡 |
| [未使用・未連携機能の調査](deduction-unused-and-unintegrated-v1.md) | 保存のみ、現行処理で未参照、UI未公開、旧仕様との不整合 |
