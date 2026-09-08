# 帳票Preview共通基盤 設定項目・連携仕様 V1

## 1. `operation_report_preview`

| 項目 | 用途・連携先 |
|---|---|
| `operation_type` | 表示する業務タブ。画面からの一覧取得条件 |
| `report_code` | 帳票の一意な業務コード。Batch・保存ファイル検索にも使用 |
| `report_name` | 画面表示名。未設定時は帳票マスター名を補完可能 |
| `job_code` | 日次等で本出力するBatch Job。HTML系やPreviewのみでは不要 |
| `table_name` | HTML Preview元のView/表 |
| `template_name` | Jasper/帳票側のTemplate名として応答へ含める |
| `html_template_key` | S3/Local上のHTML Templateキー |
| `html_template_version/hash` | Templateの版・整合性管理用 |
| `filter_column_name` | 対象日/月を絞る列。未設定時は業務Typeから既定値を採用 |
| `target_param_name` | Batchへ渡す対象日のparameter名 |
| `order_by` | Preview行順。許可文字を検証して適用 |
| `display_order` | 帳票カード表示順 |
| `active_flag` | 一覧表示・実行対象 |
| `output_type` | Preview後の操作種別 |

## 2. `operation_report_preview_columns`

| 項目 | 用途 |
|---|---|
| `operation_report_preview_id` | 親Preview定義 |
| `column_name` | Viewの列名 |
| `preview_name` | HTML表の見出し |
| `display_order` | 表示順 |
| `active_flag` | 表示対象か |

列設定がない場合は自動推測するが、業務向けの日本語見出しと表示順を保証するには列設定を登録する。

## 3. `output_type`

| 値 | 意味 | 履歴 |
|---|---|---|
| `NONE` | 定義だけ表示し出力しない | なし |
| `HTML_PREVIEW` | HTML確認のみ | なし |
| `HTML_PRINT` | HTMLをブラウザ印刷 | なし |
| `PDF` | Jasper等でPDF生成、Viewer表示 | 日次はBatch履歴、月次は締めファイル履歴 |
| `CSV` | CSV生成・Download | 同上 |
| `EXCEL` | Excel Template出力・Download | 同上 |
| `EXCEL_BOOK` | 台帳更新を表す予約Type | 共通Tabでは現在使用しない |
| `CUSTOM` | Job固有出力 | Batch実装による |

## 4. Operation別の対象値

| Operation | 画面値 | Previewの既定列 | 本出力 |
|---|---|---|---|
| `PREPARATION` | `targetDate` | `target_date` | その場でBatch実行 |
| `DAILY` | `targetDate` | `payment_date` | その場でBatch実行 |
| `MONTHLY` | `targetMonth` | `target_month` | 締め時保存ファイルを取得 |
| `BOOK` | 定義上は存在 | `target_date`相当 | 専用台帳画面を使用 |

## 5. 関連基盤

- `system/report`：帳票マスター、Batch、Exporter、ファイル履歴
- `operation/monthly`：締めVersionと保存済み帳票ファイル
- `system/mail`：PDF Viewerからの添付メール送信
- `admin/document`：S3/LocalのTemplate・生成物管理
- 各業務View：HTML Previewの最新データ源
