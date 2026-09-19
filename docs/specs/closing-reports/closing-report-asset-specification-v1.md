# 締め処理帳票・資産対応仕様 V1

最終確認日: 2026年9月14日

## 1. 目的

この資料は、日次管理、月次締め、顧客請求締め、台帳で扱う帳票について、次の5種類の資産がどの処理で使われるかを一つにまとめたものです。

- DB View: 画面Previewまたは帳票生成の元データ
- Stored Procedure: 締め時のスナップショット確定、実行単位データ作成、後処理
- JRXML: JasperReportsで生成するPDFのレイアウト
- HTML: ブラウザPreview・ブラウザ印刷のレイアウト
- Spreadsheet / Excel: Syncfusion台帳JSON、またはExcelテンプレート帳票

帳票の見た目を調整するときに「どのファイルを直せばよいか」だけでなく、「その値が現在値か締め確定値か」「再出力時に同じ結果を再現できるか」まで判断できることを目的とします。

## 2. 最重要の区分

同じ帳票名でも、次の3系統は別のものです。

| 系統 | 用途 | データ | 主なレイアウト |
|---|---|---|---|
| 現在値Preview | 締め前の画面確認 | 現在のViewを毎回読む | HTML |
| 締め保存版 | 正式な発行・再発行 | Stored Procedureでhistoryへ確定した値 | JRXMLまたはExcel |
| 台帳 | 集計確認・業務上の追記 | 台帳用Viewと締めVersion | Syncfusion Spreadsheet JSON |

したがって、HTMLを直しても締め保存版PDFの見た目は変わりません。PDFはJRXML、Excel帳票は`.xlsx`、台帳はSpreadsheet RendererまたはSpreadsheetテンプレートを直します。

## 3. 全体フロー

### 3.1 現在値Preview

1. `OperationReportTab.vue`が対象日・対象月と`reportCode`を送る。
2. `OperationReportPreviewHtmlService`が`operation_report_preview`から定義を取得する。
3. `OperationReportPreviewRowReaderService`が定義されたViewをtenant・対象期間で絞って読む。
4. 専用HTMLがある帳票は専用HTML、ない帳票は`default-table.html`で描画する。
5. Previewは現在値であり、締め履歴そのものではない。

### 3.2 日次の正式帳票

1. 画面からBatch Jobを即時実行する。
2. `report_master`と`report_param`から帳票定義・引数を取得する。
3. 前処理Stored Procedureが実行ID単位の出力データを作る。
4. JasperReportsがJRXMLを使ってPDFを生成する。
5. 生成ファイルをStorageへ保存し、帳票履歴を残す。
6. 後処理Stored Procedureが実行用一時データを削除する。

日次管理は日報の読取結果を発行する画面であり、帳票側で日報を変更しません。修正が必要な場合は日報を直して再発行します。

### 3.3 月次締めの正式帳票

1. 月次締め開始時に締めVersionを決定する。
2. `monthly_closing_output_definition`を実行順に処理する。
3. `REPORT`はStored Procedureでhistoryへ確定し、JRXMLまたはExcelからファイルを生成する。
4. `LEDGER`は台帳用Viewを読み、Spreadsheet Rendererで締めVersion付きJSONを生成する。
5. 正式ファイルの参照情報を`monthly_closing_report_files`へ保存する。
6. 画面の本印刷・ダウンロードは保存済みファイルを読む。再度現在Viewから作り直さない。

### 3.4 顧客請求締め

1. 顧客ごとの締日から請求期間を算出する。
2. 既存の入金済み取引と同期可能か、外部Storageへ書き出す前に検証する。
3. 顧客の`invoiceType`を`MONTHLY_INVOICE_PATTERN_1/2/3`へ解決する。
4. 請求書を確定・生成する。
5. 確定済み請求書履歴を元に注文書を確定・生成する。
6. 生成成功後に取引管理へ同期する。

顧客ごとに期間が異なるため、顧客締めの正式生成では共通の対象月だけでなく、顧客別の`periodFrom`、`periodTo`、`customerId`を渡します。

## 4. 帳票一覧と資産対応

### 4.1 日次管理

| 帳票 | reportCode | 画面Previewデータ | Stored Procedure | 正式出力 | HTML |
|---|---|---|---|---|---|
| 日別労務費一覧 | `DAILY_LABOR_COST_PREVIEW` | `vw_daily_labor_cost_preview` | なし | HTML Preview/印刷 | `daily_labor_cost.html` |
| 給与支払表 | `DAILY_PAYMENT_PREPARATION` | `vw_daily_payment_preparation_preview` | なし | HTMLブラウザ印刷 | `daily_payment_preparation.html` |
| 日次給与明細 | `DAILY_PAY_SLIP` | `vw_daily_pay_slip_latest` | `sp_daily_pay_slip_prepare` / `sp_daily_pay_slip_cleanup` | `daily_pay_slip.jrxml`によるPDF | `daily_pay_slip.html` |

#### 日別労務費一覧

- 対象日は`target_date`で絞る。
- 支払サイクルと給与計算基準を分けて扱う。
- 発生労務費は勤務日に発生した給与原価、当日支払額はその日が支払日の場合の支払額である。
- 日給基準の日払いは、発生労務費と当日支払額が同日の値になる。
- 日給基準の週払い・月払いは、発生労務費は当日分、支払日は同一支払日までの対象日報を合算する。
- 週給基準・月給基準は現行V1の基準日数で日額換算する。

#### 給与支払表

- `payment_date = 対象日`の日報・従業員を支払サイクル別に表示する。
- 日払いは当日分、週払い・月払いは同一支払日までの支給・手当・控除を集計する。
- 金種内訳は、各従業員の支払額を個別に分解してから金種別枚数を合算する。総額だけを一括分解しない。
- 現行金種は1万円札、5千円札、千円札、500円玉、100円玉、50円玉。10円・5円・1円を運用対象にする場合はViewとHTMLを同時に拡張する。

#### 日次給与明細

- `vw_daily_pay_slip_latest`が日報の給与本体、勤務時間、手当、控除、貯蓄等を帳票形へ整える。
- 手当・控除欄はマスター駆動の明細を帳票用の動的項目へ展開する。
- HTMLは締め前確認、JRXML PDFは配布・保存用である。
- 日次は確定historyを持たず、実行用の`daily_pay_slip_output`を作り、PDF生成後にcleanupする。

### 4.2 翌日準備（締め処理の隣接帳票）

| 帳票 | reportCode | View | Stored Procedure | 正式出力 | HTML Preview |
|---|---|---|---|---|---|
| 作業証明伝票 | `DAILY_WORK_ORDER` | `vw_daily_work_order_render_source` | `sp_daily_work_order_prepare` / `sp_daily_work_order_cleanup` | `daily_work_order.jrxml` | 専用HTML未接続のため共通表 |

これは日次締めの確定帳票ではなく、翌日準備データから出す作業伝票です。`daily_work_order.html`はリソースとして存在しますが、現行のPreview定義・HTML初期化には接続されていません。

### 4.3 自社月次締め

| 帳票 | reportCode | 現在値View | Stored Procedure | history | 正式レイアウト |
|---|---|---|---|---|---|
| 月次給与明細 | `MONTHLY_PAY_SLIP` | `vw_monthly_pay_slip_operation_preview` / `vw_monthly_pay_slip_latest` | `sp_monthly_pay_slip_snapshot` / cleanup | `monthly_pay_slip_history` | `monthly_pay_slip.jrxml` |
| 労務費一覧表 | `MONTHLY_LABOR_COST_LIST` | `vw_monthly_labor_cost_list_latest` | `sp_monthly_labor_cost_list_snapshot` / cleanup | `monthly_labor_cost_list_history` | `monthly_labor_cost_list.xlsx` |

#### 月次給与明細

- Preview用Viewは、最新の締めVersionがあればその確定履歴を優先し、未締めなら現在値を表示する。
- 締め時には日報・月次計算結果を`monthly_pay_slip_history`へ固定する。
- JRXML描画時は`vw_monthly_pay_slip_render_flat`を実行IDで読む。
- 再締めは`closingVersion > 1`として`RECLOSE`を渡し、新しいVersionの履歴とファイルを作る。
- 手当・法定控除・その他控除は、帳票用の名称・値スロットへ展開した確定値を使う。

#### 労務費一覧表

- `vw_monthly_labor_cost_list_item_total`で手当・控除等の明細を集計する。
- `vw_monthly_labor_cost_list_latest`が従業員単位の最新値を返す。
- 締め時に`monthly_labor_cost_list_history`へ固定し、実行用`monthly_labor_cost_list_output`を生成する。
- Excelテンプレート内の`${column}`は先頭行の共通値、`${row.column}`は繰返し明細である。
- `${target_month:yyyy年M月}`、`${row.net_amount:#,##0}`のように日付・数値書式を指定できる。
- 繰返し行はスタイル・数式・結合情報を複製し、明細数に応じてフッターを移動する。

### 4.4 顧客請求締め

| 帳票 | 論理reportCode | 実出力reportCode | View / render View | Stored Procedure | history | 正式レイアウト |
|---|---|---|---|---|---|---|
| 請求書 | `MONTHLY_INVOICE` | `MONTHLY_INVOICE_PATTERN_1` | `vw_monthly_invoice_pattern_1_render` | `sp_monthly_invoice_snapshot` / cleanup | `monthly_invoice_history` | `monthly_invoice_pattern_1.jrxml` |
| 請求書 | `MONTHLY_INVOICE` | `MONTHLY_INVOICE_PATTERN_2` | `vw_monthly_invoice_pattern_2_render` | 同上 | 同上 | `monthly_invoice_pattern_2.jrxml` |
| 請求書 | `MONTHLY_INVOICE` | `MONTHLY_INVOICE_PATTERN_3` | `vw_monthly_invoice_pattern_3_render` | 同上 | 同上 | `monthly_invoice_pattern_3.jrxml` |
| 注文書 | `MONTHLY_ORDER_FORM` | 同左 | `vw_monthly_order_form_render` | `sp_monthly_order_form_snapshot` / cleanup | `monthly_order_form_history` | `monthly_order_form.jrxml` |

#### 請求書

- 共通Previewは`vw_monthly_invoice_operation_preview`と`monthly_invoice.html`を使う。
- 正式生成時のパターンは顧客マスターの請求書形式で決まり、Javaの`InvoiceReportCodeResolver`が1〜3へ解決する。
- Pattern 1は職種別、Pattern 2は職種・現場役職別、Pattern 3は現場・職種・現場役職別である。
- `sp_monthly_invoice_snapshot`が顧客別期間、締めVersion、税額、請求明細をhistoryへ固定する。
- `vw_monthly_invoice_render_flat`が共通の確定値を平坦化し、各Pattern用ViewがJRXMLに必要な粒度へ変換する。
- 保存済みファイルは実reportCodeで記録されるが、画面は論理コード`MONTHLY_INVOICE`でPattern 1〜3をまとめて検索できる。

#### 注文書

- `sp_monthly_order_form_snapshot`は確定済み`monthly_invoice_history`を元に注文書履歴を作る。
- 請求書と注文書の金額・対象期間が別計算でずれないよう、注文書の基点は請求書履歴とする。
- 現在の画面Previewは専用HTMLが登録されていないため`default-table.html`へフォールバックする。
- 正式版は`monthly_order_form.jrxml`を使う。

請求書・注文書は自社月次締めから除外し、顧客請求締めだけで生成します。これは顧客ごとに締め期間が異なるためです。

### 4.5 月次締めSpreadsheet台帳

| 台帳 | bookCode | データView | rendererKey | 対象選択 | 正式保存 |
|---|---|---|---|---|---|
| 月間労務表 | `MONTHLY_LABOR` | `vw_monthly_labor_ledger` | `MONTHLY_LABOR_V1` | 従業員ごと | 締めVersionごと |
| 労務費支払一覧 | `LABOR_COST_PAYMENT` | `vw_labor_cost_payment_ledger` | `LABOR_COST_PAYMENT_V1` | なし | 締めVersionごと |
| 入金確認表 | `RECEIPT_CONFIRMATION` | `vw_receipt_confirmation_ledger` | `RECEIPT_CONFIRMATION_V1` | なし | 締めVersionごと |
| 月間集計表 | `MONTHLY_SUMMARY` | `vw_monthly_summary_ledger` | `MONTHLY_SUMMARY` | なし | 締めVersionごと |

- 台帳マスターはシステムメニューと締めメニューで同じ`excel_book_master`を使う。
- データソースの許可列は`excel_book_data_source_catalog`で管理する。
- V1の4台帳は専用Rendererが固定レイアウトを組み立てるCODE方式であり、`${変数}`だけで全体レイアウトを作る方式ではない。
- Rendererが`requiresTemplate() = true`のときだけ、`ledgers/default/{bookCode}/template.json`が必須になる。
- CODE方式では空のテンプレートでも生成できる。管理画面のテンプレートは、そのRendererがテンプレートを読む設計の場合にだけ反映される。
- `RECEIPT_CONFIRMATION`は締め前の作業版で入金額・手数料・相殺・その他調整・備考を編集でき、編集内容を取引へ同期する専用Handlerを持つ。
- 締め時は`closing/v{version}`側へ確定版を保存し、本印刷・保管対象にする。

## 5. Viewの責務

Viewは、業務データを帳票が読める列名・粒度へ変換する層です。レイアウト上の位置や色は持ちません。

### 5.1 Viewを変更するケース

- 金額の計算根拠を変える。
- 集計単位を従業員別、顧客別、現場別などへ変える。
- 帳票へ新しい業務項目を供給する。
- 日本語表示用の日付ラベルや区分名を追加する。

### 5.2 Viewを変更しないケース

- 列幅、罫線、色、改ページだけを変える。
- 同じ値の表示位置だけを変える。

### 5.3 原則

- tenant条件を必ず維持する。
- 論理削除済みデータを除外する。
- 日次は対象日、月次は対象月、顧客締めは顧客別期間を明示する。
- 正式帳票の再現性が必要な値は、Viewだけに残さずStored Procedureでhistoryへ固定する。

## 6. Stored Procedureの責務

Stored Procedureは「帳票生成時点の値を確定する」ために使います。

| 種類 | 処理 |
|---|---|
| prepare | 日次など、実行ID単位の一時出力を作る |
| snapshot | 月次・顧客締めの値をhistoryへ固定し、実行用データを作る |
| cleanup | PDF/Excel生成後に実行用の一時データを削除する |

共通引数は、`executionId`に加え、帳票に応じて`targetMonth`、`paymentDate`、`customerId`、`periodFrom`、`periodTo`、`closingVersion`、`executionMode`を使います。

Stored Procedureを変更する場合は、既存Versionの履歴を上書きしないこと、同じbusiness keyの再実行が重複を作らないこと、失敗時に中途半端な確定行を残さないことを確認します。

## 7. JRXMLの仕様

JRXMLは正式PDFの表示専用です。業務計算はJRXMLの式へ持ち込まず、原則としてViewまたはhistoryで済ませます。

### 7.1 配置場所

`backend/src/main/resources/reports/`

### 7.2 V1対象

- `daily_pay_slip.jrxml`
- `daily_work_order.jrxml`
- `monthly_pay_slip.jrxml`
- `monthly_invoice_pattern_1.jrxml`
- `monthly_invoice_pattern_2.jrxml`
- `monthly_invoice_pattern_3.jrxml`
- `monthly_order_form.jrxml`

### 7.3 変更時の注意

- JRXMLのfield名は、`report_master.query_sql`が返す列名と一致させる。
- parameter名は`report_param`およびBatch実行引数と一致させる。
- 日本語フォント、PDF埋込、ページサイズ、余白、改ページを確認する。
- レイアウト調整だけならView・Stored Procedureを変えない。
- 本番反映時はclasspath資産がコンテナ内の帳票テンプレート配置へコピーされることを確認する。

## 8. HTMLの仕様

HTMLは画面Previewとブラウザ印刷用です。Thymeleafテンプレートとして、`definition`、`columns`、`rows`、`request`を受け取ります。

### 8.1 配置場所

`backend/src/main/resources/templates/operation/reportpreview/`

### 8.2 Storage上の版管理

専用HTMLは`documents/templates/reports/html/{reportCode}/v{version}/template.html`へ初期配置されます。`default`は正式なtenant IDとして利用可能です。一方、tenant Context自体が欠落した場合は別tenantへ誤接続しないよう失敗させます。

Bundled initializerが現在自動初期化するのは次の4帳票です。

- `DAILY_LABOR_COST_PREVIEW` v1
- `DAILY_PAYMENT_PREPARATION` v1
- `DAILY_PAY_SLIP` v2
- `MONTHLY_PAY_SLIP` v2

`monthly_invoice.html`はclasspath名としてPreview定義から参照します。専用キーがない帳票は共通`default-table.html`を使います。

`monthly_labor_cost.html`、`monthly_payment_confirm.html`、`daily_work_order.html`はファイルとして存在しますが、現行のPreview定義またはBundled initializerには接続されていません。削除前に採用・廃止を決める未接続資産です。

## 9. Excelテンプレート帳票の仕様

`monthly_labor_cost_list.xlsx`はApache POIで処理する正式Excel帳票です。Syncfusion Spreadsheet台帳のJSONテンプレートとは別物です。

### 9.1 プレースホルダー

| 書式 | 意味 |
|---|---|
| `${column_name}` | 先頭データ行を使う共通値 |
| `${row.column_name}` | 明細行。行数分複製する |
| `${column_name:yyyy年M月}` | 日付書式付き共通値 |
| `${row.amount:#,##0}` | 数値書式付き明細値 |

完全一致セルは数値・日付を型付きで設定します。文章内へ埋め込んだ場合は文字列化します。参照列が存在しない場合、またはテンプレート内にプレースホルダーが一つもない場合は生成を失敗させます。

## 10. Syncfusion Spreadsheet台帳の仕様

台帳はExcelファイルそのものではなく、Syncfusion Spreadsheetが読み込むWorkbook JSONをStorageへ保存します。画面のExcelダウンロード・印刷はそのWorkbookから行います。

### 10.1 生成方式

- `excel_book_master.renderer_key`でRendererを選ぶ。
- Rendererが全ソース列を使う場合は台帳用Viewをそのまま読む。
- 汎用マッピング方式では、台帳マスターの変数とデータソース列を対応させる。
- 月間労務表は従業員ごとに1ファイル、それ以外のV1台帳は月ごとに1ファイルを生成する。

### 10.2 Readiness

生成前に次を検証します。

- rendererKeyに対応するRendererがある。
- sourceNameに対応するデータソースカタログがある。
- 変数マッピングが必要な方式では、全参照列が許可されている。
- 選択型では選択データソース、値列、表示列が有効である。
- テンプレート必須方式ではWorkbook JSONが保存済みである。
- `monthly_closing_output_definition`に月次締め対象として登録されている。

なお、最後の「月次締め対象登録」は画面からの作業生成可否とは別情報として表示され、月次締めへの連動漏れを検知します。

## 11. 締めVersionと保存方針

- 初回締めはVersion 1、再締めはVersion 2以降とする。
- 初回は`executionMode=INITIAL`、再締めは`executionMode=RECLOSE`を渡す。
- 正式帳票は`monthly_closing_report_files`にStorage種別、キー、ファイル名、サイズ、生成日時、対象、締めVersionを保存する。
- 顧客請求書は実際に使ったPatternコードで保存する。
- 画面で過去Versionを指定した場合、そのVersionの保存済みファイルを返す。
- 失敗した締めは成功Versionとして確定せず、次の成功時に不要なVersion消費を起こさない。
- 年次バックアップは帳票ごとの保持年数を参照する。V1の月次給与明細、請求書、労務費一覧、注文書、4台帳は7年設定を基本とする。

## 12. 帳票を修正するときの判断表

| 変更内容 | 主に直す場所 | 同時確認 |
|---|---|---|
| 計算式・集計条件 | View | 日報/月次履歴、境界日、tenant |
| 締め時点の保存項目 | Stored Procedure・history | 再締め、再実行、過去Version |
| PDFの位置・色・罫線 | JRXML | field名、フォント、改ページ |
| ブラウザPreviewの位置・色 | HTML | 画面と印刷CSS、Storage上のtemplate version |
| Excel正式帳票の配置 | `.xlsx` | `${...}`列名、明細行、数式範囲 |
| 台帳の固定配置・性能 | Spreadsheet Renderer | データView、結合、罫線、印刷範囲 |
| 台帳の汎用差込 | Spreadsheet template + variable mapping | catalog許可列、Readiness |
| 帳票の追加 | SQLマスター + View/SP + template + Batch | Preview定義、締め出力定義、保存履歴 |

## 13. 新しい帳票を追加する場合

### 13.1 HTML Previewだけを追加

1. Preview用Viewを作る。
2. `operation_report_preview`を登録する。
3. 表示列を`operation_report_preview_column`へ登録する。
4. 専用HTMLが必要ならHTMLファイルとStorage初期化定義を追加する。
5. Readiness対象にする場合は必須コードへ追加する。

Javaをまったく変更せず追加できるのは、既存の汎用Preview入力形式で足りる場合です。専用のパラメータ解釈、複雑な集約、独自のテンプレート配布が必要ならJavaまたはSQLの追加が必要です。

### 13.2 正式PDFを追加

1. 最新値Viewと、必要ならhistory・実行用テーブルを作る。
2. prepare/snapshot/cleanup Stored Procedureを作る。
3. JRXMLを追加する。
4. `report_master`と`report_param`を登録する。
5. `batch_job`を登録する。
6. `operation_report_preview`へ画面表示定義を登録する。
7. 月次締め対象なら`monthly_closing_output_definition`へ登録する。
8. ファイル生成・履歴・再締め・失敗時ロールバックをテストする。

### 13.3 Spreadsheet台帳を追加

1. 台帳用Viewを作る。
2. データソースカタログと許可列を登録する。
3. 固定配置ならRenderer、汎用差込なら変数マッピングとSpreadsheetテンプレートを用意する。
4. `excel_book_master`を登録する。
5. 月次締め対象なら`monthly_closing_output_definition`へ登録する。
6. Readiness、生成、再生成、印刷、ダウンロードを確認する。

## 14. V1確認観点

- Preview値と正式帳票値の差が、未締め/締め済みという意図した差だけである。
- 月次・顧客締め後に元データを変更しても、過去Versionの正式帳票が変わらない。
- 再締め後も旧Versionを参照できる。
- 顧客別締日、当月・翌月・翌々月の支払日が正しく反映される。
- 動的手当・控除・貯蓄・法定準備金が日次明細と月次確定値へ反映される。
- 請求書Patternごとに件数、単価、税額、合計が一致する。
- 注文書が同じVersionの請求書履歴を参照する。
- 取引管理と入金確認表に顧客締め結果が同期される。
- PDF、ブラウザ印刷、Excel、Spreadsheet印刷で罫線・日本語・日付・金額書式が崩れない。
- tenant Context欠落時は失敗し、正式tenant ID `default`では正常に生成できる。

## 15. 実装資産の場所

| 資産 | 場所 |
|---|---|
| JRXML / Excel | `backend/src/main/resources/reports/` |
| HTML | `backend/src/main/resources/templates/operation/reportpreview/` |
| 帳票SQL | `backend/src/main/resources/sql/system/report/` |
| 台帳SQL | `backend/src/main/resources/sql/system/excelbook/` |
| 月次締め定義SQL | `backend/src/main/resources/sql/operation/monthly/closing_output_foundation_v1.sql` |
| 共通Preview Java | `backend/src/main/java/com/project/backend/features/operation/reportpreview/` |
| 月次・顧客締めJava | `backend/src/main/java/com/project/backend/features/operation/monthly/` |
| Spreadsheet Renderer | `backend/src/main/java/com/project/backend/features/operation/book/service/` |
| Jasper/Excel実行 | `backend/src/main/java/com/project/backend/features/system/report/` |
