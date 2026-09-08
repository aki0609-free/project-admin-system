# 台帳管理ドメイン仕様

## 対象画面

- 締め処理 → 台帳管理
- 画面URL：`/operation/book`
- API：`/api/operation/excel-books`
- マスター：`system/excelbook`

台帳はSpreadsheet形式のWorkbook JSONを生成・表示・必要に応じて編集保存する。通常帳票Previewとは異なる特殊画面として扱い、月次締め時には同じ生成Serviceを確定Version付きで呼び出す。

## 資料

| 資料 | 内容 |
|---|---|
| [画面からStorage・月次締めまでの処理フロー](book-operation-screen-to-storage-flow-v1.md) | マスター、選択対象、View、Renderer、保存、締め連携 |
| [設定項目の利用先・システム連携](book-operation-field-usage-and-integration-v1.md) | 台帳マスター、Catalog、変数、印刷・編集設定 |
| [未使用・未連携・不整合調査](book-operation-unused-and-unintegrated-v1.md) | 年度固定、固有Renderer、複数台帳Preview等の課題 |
