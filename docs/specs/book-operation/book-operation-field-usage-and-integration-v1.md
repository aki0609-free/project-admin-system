# 台帳管理 設定項目・システム連携 V1

## 1. `excel_book_master`

| 項目 | 用途 |
|---|---|
| `book_code/name` | 台帳識別・画面表示・Storageパス・月次出力コード |
| `template_file_path` | Template管理上のパス情報。実WorkbookはTemplate Serviceから取得 |
| `output_file_path` | 出力先設定値。現行生成ServiceはDocument storage resolverを使用するため実利用を要確認 |
| `source_type` | V1は`SNAPSHOT`のみ対応 |
| `layout_type` | `renderer_key`未設定時の互換Fallback |
| `renderer_key` | Renderer Registryの実装選択 |
| `source_name` | 行データを取得するCatalogコード |
| `template_sheet_name` | Template内の元Sheet名 |
| `active_flag` | 台帳一覧・生成対象 |
| `print_*` | 用紙、方向、1ページFit。画面/Rendererへ渡す印刷設定 |

## 2. 対象選択設定

| 項目 | 用途 |
|---|---|
| `selection_mode` | 対象選択の有無・方式 |
| `selection_source_name` | 候補取得Catalog |
| `selection_value_column` | employee_id等の保存・検索値 |
| `selection_display_columns` | 候補Dialogへ表示する列 |
| `allow_select_all` | 全件選択の許可 |
| `generation_unit` | V1は選択なし=`ONE_FILE`、対象選択あり=`FILE_PER_SELECTION` |

## 3. データソースCatalog

Catalogは`physical_name`、tenant scope、where template、maxRowsと、許可列一覧を持つ。台帳マスターから任意SQLを直接実行せず、事前登録したViewと列へ限定する安全境界である。

## 4. 変数Mapping

汎用Template Rendererでは、Template内の`${変数名}`とViewの`source_column`を対応付ける。固定Rendererは全行・全列を受け取り、帳票固有の複雑なセル配置をコードで行う場合がある。

## 5. WorkbookとTemplate

- Template：管理画面で保存したSpreadsheet Workbook JSON
- 生成物：Templateへ対象月データを展開したWorkbook JSON
- 作業版：締め前に閲覧・編集可能な安定パス
- 確定版：月次締めVersion配下の変更不可Snapshot

台帳はxlsxそのものではなく、ブラウザSpreadsheetが扱うJSONをStorageへ保存する。Excel Download/PDF変換は表示ComponentのExport能力・実装状況に依存する。

## 6. 台帳とDB同期

通常の台帳はDB/Viewからの一方向生成である。Renderer固有Edit Handlerが登録された台帳だけ、画面編集を業務DBへ反映する。現在の代表例は入金確認表の入金確定処理である。

## 7. 月次締め出力定義

`monthly_closing_output_definition`の`output_type=LEDGER`、`output_code=bookCode`により、月次締め対象へ追加する。台帳マスターに存在するだけでは自動的に月次締め対象にはならない。締め処理の台帳一覧は同じ`excel_book_master`から作られ、上記定義との一致状態も返す。

## 8. 年度開始月

台帳画面の年度・月候補は、業務管理の年度帳票バックアップ設定にある`fiscal_year_start_month`を共通の年度開始月として参照する。画面内の固定値は持たない。
