# 業務管理ドメイン仕様

## 対象画面

- 管理者メニュー → 業務管理
- 画面URL：`/admin/business-settings`
- 管理API：`/api/admin/business-settings`
- 利用権限：`SYS_ADMIN`

業務管理は単なる設定画面ではない。退職処理、給与締め期間、月次出力、年度帳票バックアップ、ヘッダーのJira・Confluenceリンクへ設定値を供給する。

## 資料

| 資料 | 内容 |
|---|---|
| [画面からDB・後続処理までの処理フロー](business-settings-screen-to-db-flow-v1.md) | 5タブの取得・保存と、退職・締め・帳票・バックアップ・外部リンクへの連携 |
| [入力項目の利用先・システム連携](business-settings-field-usage-and-integration-v1.md) | 各パラメーターのDB列、検証、利用先、初期値 |
| [未使用・未連携機能の調査](business-settings-unused-and-unintegrated-v1.md) | 表示と実装の不一致、汎用Core上の課題、テスト状況、V1判断 |
