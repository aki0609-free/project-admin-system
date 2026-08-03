# 作業証明伝票 Jasper帳票基盤 V1

## 1. 目的

「翌日準備」で登録した従業員配置・現場配車を基に、顧客・現場単位の作業証明伝票をPDF生成する。

- JasperReportsでPDF生成する
- 生成PDFは通常の帳票履歴へ保存する
- PDF生成後に画面でプレビューし、ブラウザ印刷できる
- 帳票行のクリック時は、PDF生成前でも簡易HTMLプレビューを表示する
- 帳票固有のデータ整形は専用View／ストアドへ閉じ込める
- 帳票レイアウト変更は原則として専用JRXMLだけを変更する

## 2. 確定仕様

| 項目 | 仕様 |
|---|---|
| 帳票コード | `DAILY_WORK_ORDER` |
| 帳票名 | 作業証明伝票 |
| 生成元 | 翌日準備 |
| 生成単位 | 対象日＋顧客＋現場 |
| 改ページ | 1ページ最大10名。11名以上は同じ顧客・現場の次ページ |
| ページ表示 | 全n枚中m枚目 |
| 出力形式 | JasperReports PDF |
| 履歴 | 帳票履歴およびS3へ保存 |
| 画面 | 翌日準備 → 帳票 → 作業証明伝票 |

帳票形式とプレビュー形式は分離する。作業証明伝票の本出力はPDFだが、
一覧行クリック時にはViewの主要項目を共通HTMLテーブルで簡易表示する。

## 3. 自動出力項目

- 作業日・曜日
- 得意先
- 現場名
- 作業員名
- 作業内容
- 会社から現場までの距離
- 配車台数
- 自社名
- 自社電話番号
- 自社FAX番号

## 4. 手書き項目

- 工事番号
- 職種
- 始業・終業
- 作業時間
- 時間外
- 深夜
- 自家用車使用
- 同乗者人数
- 通勤費
- 使用車両の詳細
- 機材・資材・納入品
- 通勤経路
- 所属長承認欄
- Memo

## 5. データフロー

```text
daily_preparations
  + daily_preparation_assignments
  + daily_preparation_dispatches
  + company_profile
        ↓
vw_daily_work_order_render_source
        ├─ 帳票クリック → 簡易HTMLプレビュー
        ↓ sp_daily_work_order_prepare(executionId)
daily_work_order_render_output
        ↓ daily_work_order.jrxml
PDF生成
        ↓
帳票履歴 + S3保存
        ↓
PDFプレビュー → ブラウザ印刷
```

## 6. 実装資産

| 区分 | パス |
|---|---|
| DDL・View・ストアド・マスター | `backend/src/main/resources/sql/system/report/daily_work_order/daily_work_order_foundation_v1.sql` |
| Jasperテンプレート | `backend/src/main/resources/reports/daily_work_order.jrxml` |
| Jasper描画テスト | `backend/src/test/java/com/project/backend/features/system/report/template/DailyWorkOrderJasperTemplateTest.java` |
| 帳票入力共通テスト | `backend/src/test/java/com/project/backend/features/system/report/service/builder/ReportInputBindBuilderTest.java` |
| 帳票一覧・出力画面 | `frontend/src/features/operation/reportpreview/components/OperationReportTab.vue` |
| PDFプレビュー・印刷 | `frontend/src/shared/components/pdf/PdfPreviewDialog.vue` |

## 7. 帳票固有変更の境界

### データ取得・項目追加

`vw_daily_work_order_render_source` と `sp_daily_work_order_prepare` を変更する。

### レイアウト変更

`daily_work_order.jrxml` を変更する。

### 共通基盤

通常のレイアウト変更では、帳票実行、S3保存、履歴、PDFプレビュー、印刷の共通コードは変更しない。

## 8. プレビュー対応状況

| 出力種別 | 画面上の確認方法 | 状況 |
|---|---|---|
| HTML_PREVIEW | iframeで即時表示 | 対応済み |
| HTML_PRINT | iframeで表示後、ブラウザ印刷 | 対応済み |
| PDF | クリック時は簡易表示。生成後はPDF専用ダイアログで表示・印刷 | 対応済み |
| EXCEL_BOOK | Spreadsheet台帳画面で表示・編集 | 対応済み |
| EXCEL | クリック時は簡易表示。本出力時に`.xlsx`をダウンロード | 対応済み |
| CSV | クリック時は簡易表示。本出力時に`.csv`をダウンロード | 対応済み |

## 9. 適用手順

### ローカル

1. DBへ `daily_work_order_foundation_v1.sql` を適用する。
2. BackendとFrontendを再ビルドする。
3. 翌日準備で対象日、従業員配置、現場配車を保存する。
4. 「帳票」タブを開く。
5. 「作業証明伝票」を選択する。
6. 「印刷」を押して、JRXMLから生成されたPDFをプレビュアーで開く。
7. PDF内容を確認し、プレビュアーの「印刷」を押して本印刷する。

### AWS DEV

1. `infrastructure/scripts/database/apply_runtime_schema_upgrade.sh` を実行する。
2. Backendイメージをビルド・ECRへPushする。
3. Backendランタイムを更新する。
4. Frontendイメージを更新する。
5. Cloudflare Access経由で翌日準備画面を確認する。

## 10. 確認項目

- 顧客・現場ごとにページが分かれること
- 10名までは1ページ、11名以上は次ページになること
- 「全n枚中m枚目」が正しいこと
- 得意先、現場名、従業員名、作業内容が表示されること
- 距離、配車台数、会社情報が表示されること
- PDFが帳票履歴とS3へ保存されること
- PDFダイアログから印刷できること
- 生成後に一時input/outputデータが削除されること

## 11. V1で意図的に対象外とする項目

- 工事番号等の手書き欄をシステム入力へ変更すること
- 承認フローとの連携
- 顧客別レイアウト切替
- 会社ロゴの自動配置

これらは業務仕様が確定した時点で、View、マスター、または帳票テンプレート単位で拡張する。
