# Windows 11 デスクトップショートカット

`create_project_admin_shortcut.bat` をWindows 11へコピーしてダブルクリックすると、
現在のユーザーのデスクトップに `ProjectAdmin` ショートカットを作成します。
クリックすると、既定のWebブラウザでAWS上のシステムを開きます。

## 設定箇所

BAT冒頭の次の値を変更できます。

- `APP_URL`: 開くシステムのURL
- `SHORTCUT_NAME`: デスクトップに表示する名前
- `ICON_URL`: 自動取得するICOファイルのURL

## 専用アイコンを使う場合

BATと同じフォルダに `project-admin.ico` を置いてから実行してください。
このファイルが最優先で使用されます。

Repository内から実行した場合は、既存の `frontend/public/favicon.ico` を使用します。
どちらのICOファイルも見つからない場合は、システムの `favicon.ico` を自動取得します。
取得できない場合のみWindows標準アイコンを使用します。

取得したアイコンは、ショートカットを別の場所へ移動しても消えないように、
`%LOCALAPPDATA%\ProjectAdmin\project-admin.ico` へ保存します。

Webサイトのブラウザタブに表示されるアイコンは、
`frontend/public/favicon.ico` で管理しています。
