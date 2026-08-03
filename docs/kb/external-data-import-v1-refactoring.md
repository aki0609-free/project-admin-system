# 外部データ取り込み V1 リファクタリング

## 1. 目的

ProjectAdminSystem V1の外部データ取り込み機能について、既存の取込方式を維持しながら、次を改善する。

- 取込定義、実行、履歴、エラー確認の責務整理
- 取込先テーブルとカラムの許可カタログ化
- `DELETE_INSERT` の正式対応
- CSV読込設定の整合
- スクリプト実行とファイルパスの安全性向上
- `SYS_ADMIN` 限定の権限制御
- 実行履歴とエラー追跡の改善

## 2. V1対象範囲

### 取込元

- `UPLOAD`
- `SERVER_FILE`
- `SCRIPT`

### スクリプト

- `SHELL`
- `PYTHON`

### 取込モード

- `INSERT_ONLY`
- `UPDATE_ONLY`
- `UPSERT`
- `DELETE_INSERT`

### 管理機能

- 取込定義のCRUD
- CSVアップロード実行
- サーバーファイル実行
- スクリプト生成後のCSV実行
- 取込履歴
- 行単位のエラー確認

## 3. 変更前の問題

### 3.1 取込先を自由入力できる

`tableName` と `columnName` を管理画面から文字列で指定できた。SQL識別子の文字チェックは存在したが、業務上許可されていないテーブルへの書込みを防止できなかった。

### 3.2 DELETE_INSERTが全件削除

従来の `DELETE_INSERT` は次のSQLを実行していた。

```sql
DELETE FROM <table_name>;
```

テナント管理対象テーブルでも全テナントの行を削除する可能性があった。

### 3.3 CSV設定が実処理へ反映されない

取込定義には次の項目が存在したが、CSV処理は先頭行・カンマ区切りで固定されていた。

- `charset`
- `delimiter`
- `headerRowNumber`
- `dataStartRowNumber`

### 3.4 履歴情報不足

- `fileName` がSpring BatchのJobパラメータへ渡されていなかった
- `executedBy` が常に `system` だった

### 3.5 ファイルとスクリプトの問題

- アップロードファイル名のパス正規化が不足
- アップロード後の一時ファイルが残る
- スクリプト実行時間に上限がない
- スクリプトの標準出力が無制限
- ローカル環境とコンテナ環境でCSV出力先が固定パスに依存

### 3.6 画面と型の不整合

API型と選択肢には `DELETE_INSERT` が存在したが、Zodスキーマには存在しなかった。

## 4. V1確定仕様

## 4.1 権限

外部データ取り込みは、参照を含むすべての操作を `SYS_ADMIN` 限定とする。

バックエンドの対象API：

- `/api/system/import-targets/**`
- `/api/system/import-target-catalogs/**`
- `/api/system/import/**`
- `/api/system/import-history/**`

フロントエンドでも、`SYS_ADMIN` 以外には「外部データ取込」メニューを表示しない。

バックエンドの認可を最終的なアクセス制御とする。

## 4.2 取込先カタログ

管理画面から任意のテーブル名とカラム名を入力させない。

次のカタログへ登録された有効なテーブルとカラムだけを利用できる。

- `import_target_catalog`
- `import_target_catalog_column`

テーブルカタログの主な項目：

| 項目 | 内容 |
|---|---|
| `table_name` | 物理テーブル名 |
| `display_name` | 画面表示名 |
| `tenant_scoped_flag` | テナントデータか |
| `allow_delete_insert_flag` | DELETE_INSERTを許可するか |
| `active_flag` | 利用可能か |

カラムカタログの主な項目：

| 項目 | 内容 |
|---|---|
| `column_name` | 物理カラム名 |
| `display_name` | 画面表示名 |
| `data_type` | 取込データ型 |
| `order_no` | 表示順 |
| `active_flag` | 利用可能か |

保存時に次をバックエンドで再検証する。

- テーブルが有効なカタログに存在する
- カラムが対象テーブルの有効なカタログに存在する
- カラムのデータ型がカタログと一致する
- `columnName`、`csvHeaderName`、`orderNo` が重複しない

## 4.3 targetCode

- 英大文字、数字、アンダースコアだけを許可する
- 先頭は英大文字
- 作成後は変更不可
- 削除は物理削除せず論理削除

例：

```text
IMPORT_INCOME_TAX_TABLE
IMPORT_RESIDENT_TAX
```

## 4.4 取込モード

### INSERT_ONLY

CSVの各行を新規登録する。既存キーとの重複は行エラーになる。

### UPDATE_ONLY

`keyFlag=true` のカラムで更新対象を検索し、存在する行だけ更新する。対象がなければスキップする。

### UPSERT

`keyFlag=true` のカラムで存在確認し、存在すれば更新、存在しなければ登録する。

### DELETE_INSERT

取込開始前に対象範囲を削除し、その後CSVを登録する。

利用条件：

- カタログの `allow_delete_insert_flag=true`
- テナント対象テーブルでは現在テナントの行だけ削除
- 非テナント対象テーブルではテーブル全体を削除

非テナント対象テーブルでの実行前には、バックアップまたは復旧可能な元データが存在することを確認する。

## 4.5 テナント対象テーブル

`tenant_scoped_flag=true` の場合は、バックエンドが次を自動制御する。

- INSERT時に `tenant_id`、`created_at`、`updated_at` を設定
- UPDATE／存在確認時に `tenant_id` を条件へ追加
- UPDATE時に `updated_at` を更新
- DELETE_INSERT時に現在テナントだけ削除

CSVへ監査カラムを含める必要はない。

## 4.6 CSV形式

取込定義の次の設定を実際のCSV処理へ反映する。

| 項目 | 仕様 |
|---|---|
| `charset` | Javaで利用可能な文字コード |
| `delimiter` | 1文字 |
| `headerRowNumber` | 1以上 |
| `dataStartRowNumber` | headerRowNumberより後 |

アップロード制限：

- 拡張子：`.csv`
- 最大サイズ：20MB
- ファイル名からディレクトリ部分を除去
- 処理後に一時ファイルと一時ディレクトリを削除

## 4.7 取込元別設定

### UPLOAD

- 画面でCSVを選択する
- `scriptType` は `NONE`
- `fixedFilePath` は保存しない

### SERVER_FILE

- `fixedFilePath` 必須
- `.csv` ファイルだけ許可
- `fixedFilePath` は `project.storage.imports.csv.path` からの相対パス

例：

```text
resident_tax_2026.csv
```

### SCRIPT

- `scriptType` は `SHELL` または `PYTHON`
- `scriptPath` 必須
- `fixedFilePath` 必須
- Pythonは `.py`、Shellは `.sh`
- `scriptPath` は `project.storage.imports.script.path` からの相対パス

例：

```text
convert_income_tax_table.py
```

## 4.8 スクリプト実行

既定設定：

| 設定 | 既定値 |
|---|---|
| `project.imports.script.timeout-seconds` | 120秒 |
| `project.imports.script.max-output-characters` | 20,000文字 |
| `project.imports.script.python-command` | 環境変数またはローカルPython |

スクリプト引数では、次のプレースホルダーを利用できる。

```text
${IMPORT_CSV_DIR}
```

実行時に、その環境の `imports/csv` 絶対パスへ置換される。

例：

```text
--output ${IMPORT_CSV_DIR}/income_tax_table_2026.csv
```

## 4.9 履歴

履歴へ次を保存する。

- targetCode
- targetName
- tableName
- sourceType
- importMode
- fileName
- 総件数
- 登録件数
- 更新件数
- スキップ件数
- エラー件数
- Spring Batch Job Execution ID
- 実行ユーザー名
- 実行日時
- 全体エラー

状態：

- `SUCCESS`
- `PARTIAL_SUCCESS`
- `FAILED`

行エラーの保存上限は1回の取込につき1,000件とする。履歴の `errorCount` は実際の全エラー件数を保持する。

## 5. DDLと初期データ

## 5.1 適用順

空の環境へ初期データを作る場合：

1. Hibernate `ddl-auto=update` で既存エンティティテーブルを生成
2. `sql/system/import/init2.sql`
3. `sql/system/import/column_def.sql`
4. `sql/system/import/catalog_v1.sql`

既存環境へ追加する場合：

1. RDSスナップショットを作成
2. `sql/system/import/catalog_v1.sql` を実行
3. カタログ件数と既存取込定義の対応を確認
4. アプリケーションを再起動

`catalog_v1.sql` は既存の有効な `import_target` と `import_column` から初期カタログを生成する。

## 5.2 住民税テーブル名

既存SQLの `resident_tax` はエンティティの物理テーブル名と一致していなかったため、次へ修正した。

```text
resident_tax_monthly
```

## 6. 画面仕様

「システム運用 → 外部データ取込」は次の3タブで構成する。

### インポート定義

- 取込定義一覧
- 新規作成
- 編集
- 論理削除
- 取込先テーブルをカタログから選択
- 取込先カラムをカタログから選択

### インポート実行

- 有効な取込定義を選択
- UPLOADの場合はCSVを選択
- SERVER_FILE／SCRIPTの場合は定義済みファイルを利用

### 履歴

- 取込結果一覧
- 実行ユーザー表示
- エラー件数がある履歴の行エラー表示

## 7. 主な影響範囲

### バックエンド

- 外部取込Controllerの認可
- 取込先カタログEntity／Repository／Service／API
- 取込定義Validator／Mapper
- CSV Batch Reader／Processor
- INSERT／UPDATE／UPSERT／DELETE_INSERT SQL生成
- スクリプト実行
- アップロード一時ファイル
- 履歴保存

### フロントエンド

- 外部取込メニューのロール制御
- 取込定義の型
- DELETE_INSERTスキーマ
- テーブル／カラムのカタログ選択
- Mutationの型
- 履歴の実行者表示

### DB

- `import_target_catalog`
- `import_target_catalog_column`
- 住民税取込定義のテーブル名

## 8. 動作確認

確認済み：

- バックエンド全テスト：成功
- 外部取込カタログValidatorテスト：成功
- テナント限定SQLテスト：成功
- CSV文字コード・区切り・ヘッダー行テスト：成功
- 外部取込フロントエンドLint：成功
- フロントエンド本番ビルド：成功
- 外部取込ドメインのTypeScriptエラー：解消

プロジェクト全体のTypeScript型チェックには、応募者、顧客、日報既存箇所、メール、帳票、共通テーブル等の別ドメインの既存エラーが残っている。

### 8.1 AWS DEV適用記録

2026-07-27にAWS DEVのRDSへ`catalog_v1.sql`を適用した。

- 対象DB：`project-admin-dev-mysql` / `ADMIN`
- 適用前スナップショット：`project-admin-dev-before-import-catalog-v1-20260727-141123z`
- 適用前の有効な`import_target`：0件
- 適用前の有効な`import_column`：0件
- 作成したテーブル：
  - `import_target_catalog`
  - `import_target_catalog_column`
- 初期移行件数：0件
- 一意制約、外部キー、インデックス：確認済み
- DDL適用に使用した一時IAM権限、S3一時ファイル、EC2一時ファイル：削除済み

最新バックエンドによるHibernateスキーマ検証では、外部データ取込カタログの不足は解消した。次に検出された不足は別ドメインであるRule管理の`rule_data_source.catalog_code`であり、外部データ取込の適用結果とは分離して扱う。

## 9. 本番データを使う前の確認項目

- RDSスナップショットを取得した
- カタログDDLを適用した
- カタログのテーブルとカラムが想定どおり
- `allow_delete_insert_flag` を確認した
- SERVER_FILEのCSVが `imports/csv` 配下にある
- SCRIPTファイルが `imports/scripts` 配下にある
- Python依存ライブラリがコンテナへ導入済み
- 少量CSVでINSERT／UPDATE／UPSERTを検証した
- DELETE_INSERTは復旧可能な検証データで確認した
- 履歴の実行者、件数、エラー内容を確認した

## 10. 既知の制約・後続対応

- CSVの複数行フィールドはV1対象外
- 大容量取込のページング表示と非同期進捗通知はV2候補
- DELETE_INSERTはステージングテーブル入替方式ではない
- カタログ自体のCRUD画面はV1対象外。DDL／マスターデータで管理する
- AWSでSCRIPTを利用する場合も、実行ファイルはコンテナのローカル領域へ配置する
- S3は自由保管書類や生成帳票の管理に利用し、OSプロセスとして実行するスクリプトはデプロイ資材として管理する
