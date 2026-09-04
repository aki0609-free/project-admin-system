# 書類管理ドメイン仕様

## 対象画面

- 管理者メニュー → 書類管理
- 画面URL：`/admin/document`
- API：`/api/admin/documents`
- 利用権限：`SYS_ADMIN`
- UI：Syncfusion Essential JS 2 FileManager

## 資料

| 資料 | 内容 |
|---|---|
| [画面からLOCAL/S3までの処理フロー](document-management-screen-to-storage-flow-v1.md) | FileManager操作、領域権限、LOCAL/S3、ZIPダウンロード、下流機能との境界 |
| [領域・パラメーターの利用先](document-management-field-usage-and-integration-v1.md) | 5領域、保存キー、入力検証、帳票・バックアップ・取込との連携 |
| [未使用・未連携機能の調査](document-management-unused-and-unintegrated-v1.md) | テナント分離、監査、上書き、検索性能、スクリプト実行等のV1課題 |
