# 帳票Preview共通基盤仕様

## 対象

- 日次・翌日準備・月次・顧客締めの「帳票」タブ
- 共通Component：`OperationReportTab.vue`
- API：`/api/operation/report-previews`

本資料では、画面でデータを確認するHTML Previewと、PDF・CSV・Excel等の本出力を分けて説明する。月次系の本出力は締め時に保存したファイルを参照し、HTML Previewは原則として最新Viewを参照する。

## 資料

| 資料 | 内容 |
|---|---|
| [画面からPreview・本出力までの処理フロー](report-preview-screen-to-output-flow-v1.md) | 定義取得、HTML描画、日次出力、月次保存ファイル参照 |
| [設定項目の利用先・システム連携](report-preview-field-usage-and-integration-v1.md) | Preview定義、列定義、出力Type、対象日の使われ方 |
| [未使用・未連携・不整合調査](report-preview-unused-and-unintegrated-v1.md) | 最新Viewと確定履歴の差、BOOK未接続、設定管理等の課題 |
