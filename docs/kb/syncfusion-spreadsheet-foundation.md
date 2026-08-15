# Syncfusion Spreadsheet導入基盤

## 1. 目的

システム運用の「台帳管理」から、締め処理で利用するSpreadsheetテンプレートを編集し、書類管理S3へ保存できる基盤を整備する。

V1の台帳UIはExcelファイルを利用せず、Syncfusion Spreadsheetへ統一する。旧Apache POI処理は締め処理側のSpreadsheet生成が完成するまでバックエンド内部に限定して残し、画面や公開DTOからは除外する。

## 2. 今回の実装範囲

- Syncfusion Essential JS 2 Vue Spreadsheetの導入
- Spreadsheet依存コンポーネントのMaterial 3テーマ導入
- Spreadsheetリボン・標準メニューの日本語化
- ライセンスキーの環境変数登録
- 台帳マスターからのSpreadsheetテンプレート編集
- Workbook JSONの保存・再読込
- S3／LOCAL共通ストレージへの保存
- テンプレートサイズ上限（10MB）
- 未保存変更の確認
- 締め処理での対象月指定
- 許可済みViewからのデータ取得
- テンプレート変数展開と明細行複製
- 生成台帳のSpreadsheetプレビュー
- 生成台帳JSONのS3／LOCAL保存

## 3. 保存仕様

| 項目 | 内容 |
| --- | --- |
| 書類領域 | `TEMPLATES` |
| 相対パス | `ledgers/{tenantId}/{bookCode}/template.json` |
| S3キー | `documents/templates/ledgers/{tenantId}/{bookCode}/template.json` |
| Content-Type | `application/json` |
| 最大サイズ | 10MB |
| 保存内容 | Syncfusion `saveAsJson()` の `jsonObject` |

AWS環境では既存の書類管理S3バケットへ保存される。ローカル環境では同じキー構成でLOCALストレージへ保存される。

### 3.1 台帳の生成方式

台帳には、次の2つの生成方式がある。ローカルとAWSで方式は変わらない。

| 画面表示 | 方式 | テンプレート保存 | 用途 |
| --- | --- | --- | --- |
| `テンプレート` | 保存済みWorkbook JSONへ値を展開 | 必須 | 管理画面からレイアウトを変更する台帳 |
| `コード生成` | 帳票固有RendererがWorkbook全体を生成 | 不要 | 複雑な行・列・シート構成を持つ台帳 |
| `未設定` | テンプレート方式だがWorkbook JSONがない | 未設定 | 生成不可 |

現在のV1台帳は次のとおり。

| 台帳 | 生成方式 | Renderer／テンプレート |
| --- | --- | --- |
| 月間集計表 | テンプレート | `spreadsheet/monthly_summary_template.json` |
| 入金確認表 | テンプレート | `spreadsheet/receipt_confirmation_template.json` |
| 月間労務表 | コード生成 | `MONTHLY_LABOR_V1` |
| 労務費支払一覧 | コード生成 | `LABOR_COST_PAYMENT_V1` |

コード生成台帳にもDB上の`template_file_path`が残る場合があるが、V1の互換列であり実際の生成には使用しない。画面の生成可否は、保存テンプレートの有無だけでなくRendererの方式を含めて判定する。

労務費支払一覧は対象データが0件でも、10名分の空欄を持つA4横の完成フォーマットを表示する。これにより、データ投入前でもレイアウトを確認できる。

## 4. ライセンスキー管理

ライセンスキーをソースコードやGit管理ファイルへ直接記載しない。

### 4.1 ローカル

`frontend/.env.local` を作成する。

```properties
VITE_SYNCFUSION_LICENSE_KEY=発行されたライセンスキー
```

`.env.local` はGitの管理対象外である。

ライセンスキーは、利用するSyncfusionパッケージと同じメジャーバージョン（現在はv33.x）で発行する。Spreadsheetを利用するため、ライセンス発行画面ではSpreadsheet Editor SDKを含むエディションを選択する。

ローカルのDocker Composeでは、次のように`.env.local`を明示して起動する。

```bash
docker compose --env-file frontend/.env.local up --build -d
```

キーなしで一度ビルドした後にキーを設定した場合、BuildKit Secretの変更だけでは古いビルド層が再利用されることがある。その場合は、次の手順でフロントエンドだけキャッシュを破棄して再生成する。

```bash
docker compose --env-file frontend/.env.local build --no-cache frontend
docker compose --env-file frontend/.env.local up -d --no-deps frontend
```

アプリ起動時にライセンスを登録してからVueを描画する。これにより、ライセンスが設定済みでも初回描画時だけ試用版バーが表示される現象を防ぐ。

### 4.2 GitHub Actions／DEVデプロイ

GitHubのDEV Environmentへ次のSecretを登録する。

| 種別 | 名前 | 値 |
| --- | --- | --- |
| Environment Secret | `SYNCFUSION_LICENSE` | 発行されたライセンスキー |

DEVデプロイ時にDockerのbuild argumentへ渡し、Viteのフロントエンド資材へ登録する。CIではSecretを要求せず、公開リポジトリのPull Requestからもビルド検証できるようにする。

## 5. API

### テンプレート取得

```http
GET /api/system/excel-book-masters/{id}/spreadsheet-template
```

保存済みテンプレートがない場合、`workbook` は `null` となり、画面側で空のテンプレートを初期表示する。

### テンプレート保存

```http
PUT /api/system/excel-book-masters/{id}/spreadsheet-template
Content-Type: application/json
```

```json
{
  "workbook": {
    "sheets": []
  }
}
```

実際の`workbook`には、セル、計算式、書式、シート等のSyncfusion Workbook JSONが入る。

## 6. テンプレート変数

テンプレートでは、将来の締め処理による値展開を想定して次の形式を使用する。

```text
${targetMonth}
${bookCode}
${bookName}
${generatedAt}
${rows.employeeCode}
```

変数は台帳マスタの変数対応表へ登録する。画面では許可済みデータソースの項目だけを選択でき、次を保持する。

| 項目 | 内容 |
| --- | --- |
| `variableKey` | `${...}`の中で使用するキー |
| `sourceColumn` | カタログで許可された参照項目 |
| `scope` | `CONTEXT`（単一値）または`ROW`（明細行） |
| `dataType` | `STRING`、`NUMBER`、`DATE`、`DATETIME`、`BOOLEAN` |
| `orderNo` | 展開順 |

同じ台帳内で`variableKey`は重複不可とする。`ROW`変数を含むテンプレート行は、取得したデータ件数分だけ複製する。値はマッピングの`dataType`に従って文字列、数値、真偽値へ変換する。

## 7. 台帳管理の責務と既存Excel機能への影響

「システム運用 → 台帳管理」は次だけを担当する。

- 台帳マスタCRUD
- Spreadsheetテンプレート編集
- データソースと変数マッピングの設定
- テンプレート保存・再読込

台帳の生成・対象月選択・ダウンロードは「締め処理 → 台帳」で行う。

- `templateFilePath`と`outputFilePath`は画面、リクエスト、レスポンスから除外する。
- 既存DB列は移行中の互換性維持のため残し、新規データでは空文字を設定する。
- 既存のExcel台帳更新APIとApache POI処理は、締め処理側のSpreadsheet生成が完成するまで内部互換用として残す。
- 旧Excel台帳更新APIも`SYS_ADMIN`に制限し、物理テーブル名ではなく許可カタログからView名を解決する。
- Spreadsheet生成の動作確認後、旧API、Apache POI処理、DB列の順に削除を検討する。

## 8. マスタ管理ルール

- `bookCode`は作成後変更不可
- `bookCode`は半角英大文字、数字、`_`、`-`だけを許可
- S3保存先には変更不可の`bookCode`を使用
- 削除は論理削除とし、S3テンプレートは即時削除しない
- S3バージョニングをテンプレートの復旧に利用する
- 台帳マスタCRUDとテンプレート編集は`SYS_ADMIN`だけに許可する
- 締め処理側の台帳生成・閲覧はRole管理の`operation`権限で制御する

## 9. データソースと変数マッピング方針

物理テーブル名や自由なSQLを画面から入力させない。台帳用データソースカタログに登録したViewとカラムだけを利用する。

テンプレート変数例：

```text
${targetMonth}
${company.name}
${rows.employeeCode}
${rows.employeeName}
${rows.amount}
```

単一値、明細行、データ型、参照カラムをマスタで定義し、計算式と書式はSpreadsheetテンプレート側に保持する。

V1ではRule管理のカタログを直接参照せず、台帳ドメイン専用の次のテーブルを使用する。これによりRule仕様変更が台帳生成へ波及することを防ぐ。

- `excel_book_data_source_catalog`
- `excel_book_data_source_catalog_column`
- `excel_book_variable_mapping`

DDLは次に置く。

```text
backend/src/main/resources/sql/system/excelbook/spreadsheet_v1.sql
```

動作確認専用のView・カタログは次に置く。本番マスタ確定時には業務用データソースへ置き換える。

```text
backend/src/main/resources/sql/system/excelbook/verification_seed.sql
```

`physical_name`には台帳専用Viewを登録する。画面へ物理名とWHERE句を公開せず、利用者は`sourceCode`だけを選択する。

## 10. 現在の完了範囲

- Spreadsheetパッケージとライセンス登録
- テンプレート編集ダイアログ
- Workbook JSON保存・再読込API
- 10MB上限
- 未保存変更確認
- `bookCode`基準のS3保存先
- 旧Excelパスの画面・DTOからの除外
- 台帳生成操作をシステム運用画面から除外
- `SYS_ADMIN`制御
- 論理削除
- 台帳専用データソースカタログAPI
- データソース選択式UI
- テンプレート変数マッピングUI・永続化
- 未許可カラム、重複変数、型、単位のサーバー検証
- ローカル検証用台帳マスタ`EMPLOYEE_LEDGER_VERIFY`でCRUD・再読込を確認
- ローカル保存先へのWorkbook JSON保存を確認
- Spreadsheetリボン、ツールチップ、右クリックメニューの日本語表示を確認
- 締め処理側の台帳一覧・対象月指定・生成API
- 許可済みカタログだけを参照するデータ取得
- `CONTEXT`／`ROW`変数の展開
- 明細テンプレート行の複製
- 複製行に含まれる相対参照数式の行番号シフト
- 生成台帳のSpreadsheetプレビュー（月間集計表は締め前編集可）
- 生成台帳JSONの「生成帳票」領域への保存

ローカル確認済み保存先：

```text
/app/storage/documents/templates/ledgers/default/EMPLOYEE_LEDGER_VERIFY/template.json
/app/storage/documents/generated-reports/ledgers/default/EMPLOYEE_LEDGER_VERIFY/{yyyy-MM}/EMPLOYEE_LEDGER_VERIFY-{yyyy-MM}-{timestamp}.json
```

## 11. 次の工程

1. AWS DEVでの台帳生成・S3保存確認
2. 本番用View・カタログ・台帳マスタの確定
3. 複数明細ブロック、集計行、複雑な数式を含む業務台帳への拡張
4. Excel／Apache POI処理の廃止判定

## 12. DDL影響範囲

初期基盤は新規テーブル追加だけである。月間集計対応では既存の
`excel_book_master`へ既定値付きの列を1つ追加し、既存行は
`REPEATING_ROW`として維持する。既存列の変更・削除はない。

| 対象 | 変更 |
| --- | --- |
| RDS | 新規3テーブルとインデックスを追加 |
| `excel_book_master` | 月間集計対応で`layout_type`を追加 |
| 既存台帳 | `source_name`を内部的に`dataSourceCode`として扱う |
| S3 | 既存テンプレートの移動・削除なし |
| Rule管理 | 変更なし |

既存の`source_name`に物理テーブル名が入っている台帳は、同じ値を`source_code`に持つカタログを準備するまで更新できない。V1検証環境の既存台帳を初期化する場合はこの移行対応は不要。

## 13. AWS DEV適用記録

2026-07-28にAWS DEVへ台帳Spreadsheet基盤を適用した。

### 13.1 復旧点

| 項目 | 値 |
| --- | --- |
| RDS | `project-admin-dev-mysql` |
| Database | `ADMIN` |
| 適用前スナップショット | `project-admin-dev-before-spreadsheet-ledger-20260728-2105` |
| Snapshot ARN | `arn:aws:rds:ap-northeast-1:813222083501:snapshot:project-admin-dev-before-spreadsheet-ledger-20260728-2105` |

### 13.2 RDS適用内容

- `excel_book_data_source_catalog`を追加
- `excel_book_data_source_catalog_column`を追加
- `excel_book_variable_mapping`を追加
- `vw_excel_book_employee_verification`を追加
- `EMPLOYEE_VERIFICATION`カタログを1件追加
- 許可カラムを5件追加
- 既存テーブルの列変更・削除なし
- 既存業務データの更新なし
- Backendの`ddl-auto=validate`起動に成功

### 13.3 デプロイ資材

| 対象 | Image |
| --- | --- |
| Backend | `project-admin-dev-backend:manual-20260728-121811Z` |
| Backend digest | `sha256:a8b306c66cc235fe99572adbe41f47d77062c63a89fbff77bb9d53ec5e056177` |
| Frontend | `project-admin-dev-frontend:manual-20260728-124959Z` |
| Frontend digest | `sha256:581d5d59ccf771480a244d03f02e076f534229ab88c45525a5e7c970b6c495d2` |

Backend、Frontend、Redis、Cloudflare Tunnelはすべて`healthy`、Actuatorは`UP`を確認した。

### 13.4 AWS画面確認

- Cloudflare Access経由で台帳管理画面を表示
- `EMPLOYEE_LEDGER_VERIFY`台帳マスタを作成
- `EMPLOYEE_VERIFICATION`を選択
- `rows.employeeCode`と`employee_code`の変数マッピングを保存
- Spreadsheet編集画面を表示
- Syncfusion試用版表示なし
- Syncfusionライセンス未設定警告なし
- SpreadsheetテンプレートをS3へ保存
- 画面を閉じて再度開き、保存済みテンプレートを再読込
- リボンの「ファイル／ホーム／挿入／数式／データ／レビュー／ビュー」を日本語表示
- 「元に戻す／やり直し／切り取り／コピー」等のツールチップを日本語表示
- セルの右クリックメニューを日本語表示
- 読込エラーなし

S3確認結果：

| 項目 | 値 |
| --- | --- |
| Bucket | `project-admin-dev-documents-ff0dd38f` |
| Key | `documents/templates/ledgers/default/EMPLOYEE_LEDGER_VERIFY/template.json` |
| Content-Type | `application/json` |
| Size | 1701 bytes |
| Server-side encryption | `AES256` |
| Version ID | `AzVRlUc00C7BV_WTHwWTQulbhDBq4_BX` |

### 13.5 DDL適用方法

次のスクリプトを使用する。

```bash
AWS_PROFILE=project-admin-terraform \
AWS_REGION=ap-northeast-1 \
infrastructure/scripts/database/apply_spreadsheet_ledger_ddl.sh
```

処理内容：

1. TerraformからEC2、RDS、S3情報を取得
2. EC2 RoleへRDS管理Secretの一時読取権限を付与
3. DDLと検証データを一時S3領域へ配置
4. SSM経由でEC2へ取得
5. 一時MySQL 8.4コンテナからTLS接続
6. 既存台帳テーブルが0件であることを確認
7. DDL、検証View、検証カタログを適用
8. テーブル・カタログ・許可カラム件数を確認
9. 一時IAM権限、S3オブジェクト、EC2一時ファイルを削除

RDS管理Secretは`username`と`password`だけを保持していたため、接続先はTerraform Outputから補完する。パスワードはMySQL option file向けに引用・エスケープし、DockerでSQL標準入力を渡す際は`--interactive`を指定する。

適用後に次が残っていないことを確認済み。

- 一時IAM Inline Policy：0件
- `_deployment/database/`の一時S3オブジェクト：0件
- `/tmp/spreadsheet-ledger-ddl-*`：0件

## 14. Spreadsheet追加設定と業務テンプレート

### 14.1 日本語化の実装方式

Syncfusion公式の`@syncfusion/ej2-locale`が提供する日本語リソースをアプリ内へ組み込み、`L10n.load()`で読み込む。Spreadsheetには`locale="ja"`を指定し、全Syncfusionコンポーネントの既定カルチャーも`setCulture("ja")`へ統一する。

公式リソースのうち機械翻訳調の表示は、共通初期化処理で業務画面向けの自然な日本語へ上書きする。例：

| 公式リソースの表示 | V1での表示 |
| --- | --- |
| セーブ | 保存 |
| 切る | 切り取り |
| ペースト | 貼り付け |
| 大胆な | 太字 |
| 国境 | 罫線 |
| 選別 | 並べ替え |
| わかった | OK |
| 近い | 閉じる |

主な実装箇所：

```text
frontend/src/app/plugins/syncfusion.ts
frontend/src/features/system/excelbook/components/SpreadsheetTemplateEditorDialog.vue
```

### 14.2 第7期 月間集計表の移行仕様

#### 14.2.1 保存単位

画面では年度と月を切り替える。S3では1か月につき1つのWorkbook JSONを保持し、締め前の再生成・編集保存では同じキーを更新する。

```text
documents/generated-reports/ledgers/{tenantId}/MONTHLY_SUMMARY/{yyyy-MM}/MONTHLY_SUMMARY-{yyyy-MM}.json
```

年度開始月はV1で8月とする。将来は会社設定から取得できるよう、画面上の年度選択と実際の`targetMonth`を分離している。

#### 14.2.2 レイアウト方式

`excel_book_master.layout_type`を追加し、次の方式を切り替える。

| 値 | 用途 |
| --- | --- |
| `REPEATING_ROW` | `${rows.name}`を含む汎用明細行の複製 |
| `MONTHLY_SUMMARY` | 第7期 月間集計表互換の固定セル配置 |

業務データ取得、セル配置、テンプレートを分離する。仕様変更時は専用Viewまたは`MonthlySummarySpreadsheetRenderer`を変更し、汎用展開処理へ条件分岐を増やさない。

#### 14.2.3 集計ルール

| 項目 | V1ルール |
| --- | --- |
| 対象 | `approval_status = APPROVED`かつ未削除の日報 |
| 行グループ | 顧客＋現場＋職種＋現場役職＋請求単位＋適用単価 |
| 人数 | 日付・行グループごとの従業員重複除外人数 |
| 残業／深夜 | 日報スナップショット時間の合計 |
| 通勤等 | `走行距離×通勤請求単価＋休日時間×休日請求単価` |
| 基本／残業／深夜単価 | 日報保存時の請求単価スナップショット |
| 支払給計 | 日付ごとの`estimated_gross_pay_amount`合計 |
| 社会保険負担額 | 締め前にSpreadsheetで手入力 |
| 粗利益 | 原本数式を維持 |
| 当月利益率 | 0除算時は0% |

原本に休日専用列がないため、休日請求額は「通勤等」へ加算する。列追加が確定した場合はViewの`holiday_work_hours`と`holiday_unit_price`を利用して新しいレイアウトへ移行する。

V1の原本数式は日単価を前提とするため、`billing_unit != DAILY`は誤計算せずエラーとする。時間単価・月額・固定額は、後付けで別Rendererを追加する。

#### 14.2.4 黄色セルの扱い

既存月シートの黄色は手入力の進捗管理用で、業務データや締め状態ではない。新テンプレートへは移行しない。

- 入力進捗は画面の未保存表示で管理
- 締め状態は`monthly_closings.status`で管理
- 基本単価の緑色入力欄は原本の意味を維持

#### 14.2.5 テンプレート作成

旧`.xls`は原本を1シート化し、数式エラーを除去したうえでSyncfusionネイティブJSONへ変換する。

旧ファイルには約2,100個の結合セルがあり、SyncfusionのExcel変換サービスへ一括送信すると`UnsupportedFile`になる。そのため次の順で作成した。

1. 原本範囲からセル値・数式・書式をJSON化
2. 原本の結合範囲を`rowSpan`／`colSpan`へ復元
3. 列幅、行高、基本単価欄の緑色を復元
4. 黄色の進捗色は除外
5. 数式エラー文字列が0件であることを確認

完成したJSONは次のデプロイ資産として管理する。

```text
backend/src/main/resources/spreadsheet/monthly_summary_template.json
```

アプリ起動時、S3に未登録の場合だけ次のキーへ自動登録する。すでに管理画面で編集・保存されたテンプレートは上書きしない。

```text
documents/templates/ledgers/default/MONTHLY_SUMMARY/template.json
```

#### 14.2.6 締め前編集

- 月間集計表は締め前だけ編集可能
- 編集後は`saveAsJson()`の結果を同じ月次S3キーへ上書き
- `monthly_closings.status = CLOSED`の場合は画面を参照専用にし、バックエンドでも保存を拒否
- 閉じる際に未保存変更があれば警告

#### 14.2.7 計測項目

生成APIは次を返す。

| 項目 | 内容 |
| --- | --- |
| `workbookBytes` | S3へ保存したJSONサイズ |
| `generationDurationMs` | データ取得からS3保存までの時間 |
| `editable` | 対象月が締め前か |

画面では`openFromJson()`開始から描画フレーム完了までを計測し、「ブラウザ表示 ms」として表示する。

2026-08-02に旧`.xls`原本を再変換し、デプロイ資産とS3登録結果を確認した値：

| 項目 | 結果 |
| --- | --- |
| JSONサイズ | 3,002,155 bytes（約2.86MB） |
| シート数 | 1 |
| 行数／列数 | 92行／142列 |
| 数式数 | 4,443 |
| 結合範囲数 | 2,100 |
| 原本Workbook描画 | 13シートすべてPNG生成成功 |
| AWS生成JSON | 3,002,076 bytes、92行×142列、数式4,443件 |
| AWS生成時間 | 601ms（データ0件） |
| AWS `openFromJson()`描画 | 933ms |
| 数式エラー文字列 | 0件 |

計測値は端末性能・ブラウザ・実データ量で変動するため、固定の合格値ではなく継続監視の基準値として扱う。

#### 14.2.8 DDL／マスターデータ

```text
backend/src/main/resources/sql/system/excelbook/monthly_summary_v1.sql
```

このSQLには次を含む。

- `excel_book_master.layout_type`
- `vw_monthly_summary_ledger`
- `MONTHLY_SUMMARY_LEDGER`データソースカタログ
- 許可カラム18件
- `MONTHLY_SUMMARY`台帳マスター（冪等登録）

台帳マスターはSQLから次の内容で自動登録する。管理画面で手作業作成する必要はない。

| 項目 | 値 |
| --- | --- |
| Book Code | `MONTHLY_SUMMARY` |
| Source Type | `SNAPSHOT` |
| Layout Type | `MONTHLY_SUMMARY` |
| Data Source | `MONTHLY_SUMMARY_LEDGER` |
| Template Sheet | `TEMPLATE` |
| 変数マッピング | なし |

### 14.3 将来変更への対応方針

- 既存の`MONTHLY_SUMMARY`へ場当たり的な列番号分岐を追加しない
- レイアウト変更が互換でない場合は、新しい`layout_type`とテンプレートVersionを追加する
- Viewは業務集計だけを担当し、セル番地を持たせない
- Rendererはセル配置だけを担当し、DBへ直接アクセスしない
- S3テンプレートはVersionをメタデータへ記録する
- 過去の締め済みJSONは再生成で上書きしない
- 仕様未確定の時間単価・月額・固定額はエラーにして、黙って日単価計算しない

依存パッケージ：

```text
@syncfusion/ej2-locale 34.1.29
```

参考：

- [Syncfusion EJ2 Locale公式リポジトリ](https://github.com/syncfusion/ej2-locale)
- [Syncfusion Vue国際化ドキュメント](https://ej2.syncfusion.com/vue/documentation/common/internationalization)

### 14.4 日本語化の確認結果

ローカルDockerとAWS DEVの両方で次を確認した。

- リボンタブの日本語表示
- ホームタブの操作名・ツールチップの日本語表示
- セル右クリックメニューの日本語表示
- 既存Workbook JSONの再読込
- `${rows.employeeCode}`変数セルの維持
- 計算式セルの維持
- Syncfusion試用版表示なし
- Syncfusionライセンス警告なし

全体型チェックは、並行開発中のHR・顧客・日報等に未完了の型エラーがあるため現時点では失敗する。今回変更したSyncfusion対象ファイルのESLintとVite本番ビルドは成功している。全体型チェックの既存エラーは各ドメインのリファクタリング完了時に解消する。

## 15. 締め処理の台帳生成（V1）

### 15.1 画面責務

| 画面 | 責務 |
| --- | --- |
| システム運用 → 台帳管理 | 台帳マスタ、データソース、変数マッピング、Spreadsheetテンプレートを管理 |
| 締め処理 → 台帳 | 対象月を指定し、台帳を生成・プレビュー |
| 管理者メニュー → 書類管理 | 保存済みの生成台帳JSONを「生成帳票」から参照・管理 |

締め処理画面で生成履歴テーブルは新設しない。生成物の保管と閲覧は書類管理へ集約し、帳票履歴DBを増加させない。

### 15.2 API

#### 有効な台帳一覧

```http
GET /api/operation/excel-books
```

#### 台帳生成

```http
POST /api/operation/excel-books/{bookCode}/generate
Content-Type: application/json
```

```json
{
  "targetMonth": "2026-07"
}
```

応答には台帳名、対象月、データ件数、生成日時、保存先、展開済みWorkbook JSONを含む。

### 15.3 生成処理

1. 有効かつ未削除の台帳マスタを`bookCode`で取得する。
2. 台帳専用データソースカタログと許可カラムを取得する。
3. テナントIDと対象月だけを名前付きパラメータとしてViewへ渡す。
4. 保存済みSpreadsheetテンプレートを取得する。
5. `CONTEXT`変数と予約変数を単一値として展開する。
6. `ROW`変数を含むテンプレート行をデータ件数分複製する。
7. 複製行内の相対行参照数式を行数に合わせてシフトする。
8. 未展開の`${...}`が残っていないことを検証する。
9. 展開済みWorkbook JSONを生成帳票領域へ保存する。
10. 読取専用Spreadsheetダイアログでプレビューする。

### 15.4 予約変数

| 変数 | 内容 |
| --- | --- |
| `${targetMonth}` | 画面で指定した対象月（`yyyy-MM`） |
| `${target_month}` | 既存資産との互換用対象月 |
| `${bookCode}` | 台帳コード |
| `${bookName}` | 台帳名 |
| `${generatedAt}` | 生成日時（ISO-8601） |

予約変数は変数マッピングへ登録しない。

### 15.5 明細行と数式

- `ROW`変数を含む行を明細テンプレート行と判定する。
- V1では1シートにつき明細テンプレート行は1行までとする。
- 同じ明細行に複数の`ROW`変数を置ける。
- `${rows.amount}`のようにセル値全体が変数の場合、数値・真偽値の型を維持する。
- `従業員: ${rows.employeeName}`のような埋め込み変数は文字列として展開する。
- `=B2*C2`は2件目で`=B3*C3`へシフトする。
- `=$B$2*C2`の絶対行参照は固定し、相対行参照だけをシフトする。
- データ0件の場合はテンプレートの明細行を空行として1行維持し、応答件数は0件とする。
- 未登録変数または解決できない変数が残った場合は生成を失敗させる。

複数の明細ブロック、可変の中間集計、複数データソースの結合はV1対象外とし、業務台帳確定後に拡張する。

### 15.6 データ取得の安全策

- 画面から物理テーブル名、SELECT句、自由なWHERE句を受け取らない。
- カタログへ登録済みのViewと許可カラムだけをSELECTする。
- 識別子は英数字と`_`だけを許可する。
- WHERE句はDB資産として管理し、`;`、SQLコメント、更新・削除系キーワードを拒否する。
- `tenantScoped=true`のデータソースには`:tenantId`を必須とする。
- 利用可能な名前付きパラメータは`:tenantId`と`:targetMonth`だけとする。
- カタログの`maxRows`を超えた場合は生成を失敗させる。
- 並び順は許可カラムの登録順で固定し、生成結果を再現可能にする。

### 15.7 生成物の保存仕様

| 項目 | 内容 |
| --- | --- |
| 書類領域 | `GENERATED_REPORTS` |
| 相対パス | `ledgers/{tenantId}/{bookCode}/{targetMonth}/{bookCode}-{targetMonth}-{timestamp}.json` |
| S3キー | `documents/generated-reports/ledgers/{tenantId}/{bookCode}/{targetMonth}/{bookCode}-{targetMonth}-{timestamp}.json` |
| Content-Type | `application/json` |
| 最大サイズ | 20MB |
| 時刻基準 | `Clock`注入、Asia/Tokyo |

### 15.8 ローカル確認記録

2026-07-28に次を確認した。

- Backend、Frontend、MySQL、MongoDB、Redisがすべて`healthy`
- `EMPLOYEE_LEDGER_VERIFY`のテンプレート変数を`${rows.employeeCode}`へ更新
- テンプレートをLOCALストレージへ保存
- 締め処理の台帳一覧に有効マスタを表示
- 対象月`2026-07`で生成成功
- ローカルDBの対象データは0件
- 読取専用Spreadsheetプレビューを表示
- 生成台帳JSONを生成帳票領域へ保存
- 保存JSONに未展開の`${...}`が残っていないことを確認

確認済み生成物：

```text
/app/storage/documents/generated-reports/ledgers/default/EMPLOYEE_LEDGER_VERIFY/2026-07/EMPLOYEE_LEDGER_VERIFY-2026-07-20260728-223915671.json
```

自動テストでは、複数行の展開、型維持、相対／絶対参照数式、未登録変数、複数明細行の拒否、SQL安全策、最大件数、保存パスとClockを確認する。

### 15.9 AWS DEV適用記録

2026-07-28に締め処理の台帳生成基盤をAWS DEVへ適用した。DBスキーマ、Terraform資産、既存業務データの変更はない。

| 対象 | Image／Digest |
| --- | --- |
| Backend | `project-admin-dev-backend:manual-20260728-134627Z` |
| Backend digest | `sha256:b59ab28f2f07790eb8f5a417d30ad19ae1c2160bdbfedc573e279dda8f04e424` |
| Frontend | `project-admin-dev-frontend:manual-20260728-134639Z` |
| Frontend digest | `sha256:27cccc511d19f409d0c9b4a2ac93ab26a7ffdfb99faef0775491b7b45089c1a6` |

デプロイ後に次を確認した。

- Backend、Frontend、Redis、Cloudflare Tunnelがすべて`healthy`
- Actuatorが`UP`
- Cloudflare Access経由で締め処理の台帳画面を表示
- `EMPLOYEE_LEDGER_VERIFY`のテンプレート変数を`${rows.employeeCode}`へ更新
- 更新したテンプレートをS3へ保存
- 対象月`2026-07`で台帳生成成功
- AWS DEVの対象データは0件
- 読取専用Spreadsheetプレビューを表示
- 生成台帳をS3の生成帳票領域へ保存
- 保存JSONに未展開の`${...}`が残っていないことを確認

生成物：

| 項目 | 値 |
| --- | --- |
| Bucket | `project-admin-dev-documents-ff0dd38f` |
| Key | `documents/generated-reports/ledgers/default/EMPLOYEE_LEDGER_VERIFY/2026-07/EMPLOYEE_LEDGER_VERIFY-2026-07-20260728-225527576.json` |
| Content-Type | `application/json` |
| Size | 1704 bytes |
| Server-side encryption | `AES256` |
| Version ID | `fzghrjtX92khzSLarzRKi_FSJDfOjQXa` |

### 15.10 Testcontainers統合確認

2026-07-29に、台帳生成のService、実MySQL、台帳専用View、LOCALストレージを接続した統合テストを追加した。

確認結果：

- 対象テナント・対象月の2件だけを生成
- 別テナント1件を除外
- 別月1件を除外
- 明細行を2行へ展開
- 数値型を維持
- 相対参照数式を2件目で行シフト
- 生成JSONをLOCALストレージへ保存・再読込
- 未展開変数なし
- テスト成功後にテストデータとファイルを削除

詳細は次を参照する。

```text
docs/kb/testcontainers-integration-test-foundation.md
```

## 16. 旧Excel／Apache POI機能の廃止判断

Spreadsheet生成の画面、API、ローカル、AWS DEV、Testcontainers確認は完了した。ただし、旧機能の削除は次の理由で段階的に行う。

### 16.1 現時点で削除しないもの

| 対象 | 理由 |
| --- | --- |
| `poi-ooxml`／`poi`依存 | 税額表パーサーと共通Excel出力でも使用している |
| `excel_book_master.template_file_path` | DB列削除はRDSマイグレーションと既存値調査が必要 |
| `excel_book_master.output_file_path` | DB列削除はRDSマイグレーションと既存値調査が必要 |
| 旧Excel台帳更新API | 削除すると公開API仕様が変わるため、利用元調査後に行う |

### 16.2 現在確認できた利用状況

- 新しい「締め処理 → 台帳」は旧Excel台帳更新APIを使用していない
- システム運用の台帳画面も旧更新APIを使用していない
- Frontendの旧更新Mutationは参照されていない
- 旧更新Service、旧Excelパーサー、旧SnapshotQueryは旧更新APIだけから参照されている
- Apache POIは台帳以外にも利用されている

### 16.3 削除条件

次を満たした時点で、旧台帳APIと台帳専用Apache POI処理を削除する。

1. 本番用台帳View・カタログ・マスタが確定する
2. 少なくとも1つの業務台帳で複数件生成を確認する
3. 外部クライアントや手動運用が旧APIを利用していないことを確認する
4. 旧ローカルExcelファイルを移行または不要と判断する
5. DB列削除用DDLと復旧手順を別途レビューする
