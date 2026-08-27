# 帳票管理 Excelテンプレート差し込み基盤 V1

## 1. 目的

帳票コードごとのJava Rendererを作らず、帳票の出力テーブルが返した列をExcelテンプレートの任意セルへ差し込む。

対象は帳票管理の `output_format = EXCEL` かつ `template_file_name` が `.xlsx` の帳票である。PDF、CSV、HTMLプレビュー、Spreadsheet台帳の処理は変更しない。

## 2. 処理フロー

```text
View
  ↓ 締め・再締め
historyTable
  ↓ ストアド
outputTable
  ↓ List<Map<columnName, value>>
ExcelReportExporter
  ↓
S3から.xlsxテンプレート取得
  ↓
GenericExcelTemplateReportRenderer
  ↓ プレースホルダー置換・明細行拡張
完成.xlsx
  ↓
帳票履歴・S3保存・年次バックアップ
```

正データは締めVersion付きの履歴テーブルであり、Excel内の値や数式を正データにはしない。

## 3. テンプレート記法

### 3.1 単票値

出力データの先頭行から1回だけ取得する。

```text
${company_name}
${target_month:yyyy}年
${target_month:M}月分
${payment_date:M}/${payment_date:d}支払
```

文字列中へ埋め込むこともできる。`:書式` は日付または数値の表示書式として扱う。

### 3.2 明細値

繰り返したい1行へ `row.` を付けて指定する。

```text
${row.employee_name}
${row.basic_salary}
${row.income_tax}
```

`row.` を含む行を明細テンプレート行として認識する。出力件数分だけ書式・罫線・数式を複製する。

### 3.3 明細領域と合計行

- 明細テンプレート行から、次の固定文字または固定数値を持つ行までを予約明細領域とする。
- 出力件数が予約行数以内なら、未使用行を空欄にする。
- 出力件数が予約行数を超えたら、合計行と後続行を下へ移動して明細行を追加する。
- 合計行の `SUM(F4:F36)` のような明細範囲は、実際の最終明細行へ自動調整する。

1シートに設定できる明細テンプレート行は1行である。複数の独立した明細表が必要な帳票はV1共通基盤の対象外とし、仕様を確認してから拡張する。

## 4. 値の型

- 数値：Excel数値セルとして出力
- 真偽値：Excel真偽値セルとして出力
- 日付・日時：書式指定がなければExcel日時セルとして出力
- 書式指定または文章内埋込：表示文字列として出力
- NULL：空欄

金額を文字列化しないため、テンプレート側の合計式や表示形式をそのまま利用できる。

## 5. カラム名の検証

テンプレートに指定したカラムがoutputTableの結果に存在しない場合、ファイルを生成せず次の情報を含むエラーにする。

```text
key=missing_column, sheet=Sheet1, cell=A1
```

欠落値を無言で空欄にしないことで、View・履歴・出力テーブル・テンプレート間の不整合を早期に検出する。

## 6. 新しいExcel帳票の追加手順

1. View、historyTable、outputTable、ストアドを作成する。
2. outputTableが返すカラム名を確定する。
3. `.xlsx`原本の固定セルへ `${column_name}` を設定する。
4. 明細のひな形1行へ `${row.column_name}` を設定する。
5. 合計式・印刷範囲・改ページ・列幅・行高をExcel側で設定する。
6. 帳票マスターを `output_format = EXCEL`、`template_file_name = *.xlsx` で登録する。
7. 書類管理からS3の帳票テンプレート領域へ登録する。
8. テスト印刷で値、数式、行追加、印刷範囲を確認する。
9. 締め、履歴再出力、再締め、7年バックアップを確認する。

帳票固有のJavaクラス追加は不要である。

## 7. テンプレート更新時の注意

`BundledReportTemplateInitializer` はS3にファイルが存在しない場合だけ初期配置する。デプロイで既存テンプレートを上書きしない。

そのためテンプレートだけを修正した場合は、書類管理から該当ファイルを更新する。プレースホルダー配置の変更だけならBackendの再build・再deployは不要である。

## 8. V1制約

- `.xls`は直接処理せず、`.xlsx`へ変換して登録する。
- 1シートにつき明細領域は1つ。
- 複数明細表、クロス集計、画像の動的差し込みは個別に仕様を確定してから共通基盤を拡張する。
- テンプレート内マクロは使用しない。
- 数式の正しさと印刷レイアウトは帳票ごとのテストで確認する。

## 9. 実装・テスト資産

```text
backend/src/main/java/com/project/backend/features/system/report/service/api/exporter/
  ExcelReportExporter.java
  ExcelTemplateReportRenderer.java
  ExcelTemplateReportRendererRegistry.java
  GenericExcelTemplateReportRenderer.java

backend/src/test/java/com/project/backend/features/system/report/service/api/exporter/
  GenericExcelTemplateReportRendererTest.java
```

確認項目：

- 単票値と日付書式
- 35名出力時の明細行追加
- 数値型の維持
- 合計行と合計範囲
- 未解決プレースホルダーが残らないこと
- 存在しない出力カラムの検出

## 10. V1最終監査（2026-08-27）

ローカル環境の帳票マスター11件とストレージ上のテンプレートを照合した。

- PDF帳票9件は、対応する`.jrxml`テンプレートをすべて確認した。
- Excel帳票`MONTHLY_LABOR_COST_LIST`は、`monthly_labor_cost_list.xlsx`と汎用Rendererを使用する。
- CSV帳票`EMPLOYEE_CSV`はテンプレートを持たず、outputTableの列から生成する。
- `reportCode`は履歴、テンプレート、締め設定の参照キーになるため、作成後は変更不可とした。
- `reportCode`、動的テーブル名、View名、ストアド名は安全な識別子だけを許可する。
- PDFは`.jrxml`、テンプレート付きExcelは`.xlsx`だけを許可し、CSVへのテンプレート指定を拒否する。
- 帳票マスター、テンプレート、テスト印刷、履歴、ダウンロードの管理APIは`SYS_ADMIN`に限定する。

再締めの統合テストでは、初回と再締めで異なるVersionの履歴とファイルが両方残ることを確認した。
帳票関連の単体テスト、全JRXMLコンパイル、汎用Excel行展開、FrontendのLint・本番Buildも成功している。
