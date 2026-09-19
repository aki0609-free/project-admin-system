# Jasper Report 編集領域

Jaspersoft Studioでは `working/` 配下の `.jrxml` を開いて編集してください。アプリが利用する正式版は `backend/src/main/resources/reports/` ですが、手作業でコピーする必要はありません。

反映方法、競合防止、Local／AWSのコマンドは [帳票編集ワークスペース](../README.md) を参照してください。

- `working/`: 現在の編集用ファイル
- `baseline-2026-09-13/`: 2026年9月13日時点の比較・復旧用スナップショット
- `archive/`: ワークスペース導入時などに退避した旧working

`baseline` と `archive` は直接編集しません。
