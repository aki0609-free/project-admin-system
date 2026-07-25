# DBバックアップ V1 リファクタリング

## 1. 目的

ProjectAdminSystem V1の「システム運用 → バックアップ」について、既存のDBテーブルCSV/ZIP出力を維持しながら、安全性、テナント分離、S3・書類管理との整合を改善する。

本機能は、会計年度終了後に生成済み帳票を保管する「年度帳票バックアップ」とは別機能とする。

## 2. バックアップ機能の責務分離

### DBバックアップ

本KBの対象。

- DBテーブルをCSVへ出力
- 複数対象をZIPへまとめる
- 手動ダウンロード
- LOCALまたはS3への保存
- 実行履歴
- バッチからの定期実行

保存先：

```text
documents/backups/system/{tenantId}/{outputDir}/
```

### 年度帳票バックアップ

別工程。

- 生成済み月次帳票を会計年度単位でコピー
- 対象帳票コードを設定で管理
- 7年間保持
- バックアップ成功後に元の帳票履歴を削除可能
- 将来、EventBridgeとLambdaで年次実行

保存先：

```text
documents/backups/reports/{fiscalYear}/
```

年度帳票バックアップは本リファクタリングでは実行処理を追加しない。FileManagerとLambda接続時に実装する。

## 3. 変更前の問題

### 3.1 全テナントのデータを出力する可能性

生成SQLは次の形式で固定されていた。

```sql
SELECT column1, column2 FROM target_table
```

`tenant_id` を持つテーブルでもテナント条件がなく、他テナントのデータを含む可能性があった。

### 3.2 テーブル・カラムの実在確認がない

管理画面で入力したテーブル名とカラム名は文字種だけを確認していた。実DBに存在するか、対象テーブルに属するカラムかは保存時に確認していなかった。

### 3.3 保存先が書類管理から見えない

永続保存したファイルは `reports-output` 配下へ保存され、書類管理のバックアップ領域から参照できなかった。

### 3.4 バッチ実行でファイルが残らない

`outputMode=DOWNLOAD` の定義を定期バッチから実行すると、レスポンスのバイト列は利用されず、ファイルがどこにも保存されなかった。

### 3.5 履歴と権限

- 実行者が常に `system`
- 履歴を全件取得
- Repository検索条件に `tenant_id` がない
- バックアップAPIに明示的な認可がない

### 3.6 ファイル上書き

ファイル名が秒単位だったため、短時間に同じ定義を実行すると同じキーへ上書きする可能性があった。

## 4. V1確定仕様

## 4.1 権限

バックアップ管理は、参照・定義変更・実行・履歴ダウンロードを含めて `SYS_ADMIN` 限定とする。

対象API：

```text
/api/system/backup/**
```

フロントエンドでも `SYS_ADMIN` 以外には「バックアップ」メニューを表示しない。

## 4.2 バックアップ定義

`targetCode`：

- 英大文字で開始
- 英大文字、数字、アンダースコアだけを許可
- テナント内で一意
- 作成後は変更不可

`tableName`：

- SQL識別子として安全な文字だけを許可
- 作成後は変更不可
- 保存時に実DBへ接続して存在確認

カラム：

- 対象テーブルに実在するカラムだけを許可
- `columnName`、`csvHeaderName`、`orderNo` は定義内で重複不可
- 出力対象カラムを1件以上必要とする

テーブルやカラムの追加・変更を行う場合は、先にDB資産を反映してからバックアップ定義を更新する。

## 4.3 テナント条件

実行時に対象テーブルの実スキーマを確認する。

`tenant_id` カラムを持つ場合：

```sql
SELECT column1, column2
FROM target_table
WHERE tenant_id = :tenantId
```

`tenant_id` カラムを持たない共通マスタの場合：

```sql
SELECT column1, column2
FROM target_table
```

テナント対象かどうかを画面入力値へ依存させず、実際のテーブル構造から判定する。

## 4.4 出力方法

既存DB値との互換性のため、列挙値は維持する。

| DB値 | 画面表示 | 動作 |
|---|---|---|
| `DOWNLOAD` | ダウンロードのみ | 画面へファイルを返す |
| `SERVER_FILE` | ストレージ保存のみ | LOCALまたはS3へ保存 |
| `BOTH` | 保存＋ダウンロード | 保存後、画面にも返す |

`SERVER_FILE` という名称は互換用であり、AWS環境ではS3へ保存される。

## 4.5 保存先

ストレージ保存時のキー：

```text
documents/backups/system/{tenantId}/{outputDir}/{fileName}
```

例：

```text
documents/backups/system/default/master-data/BACKUP_REPORT_MASTER_20260725_120000_123_a1b2c3d4.csv
```

`outputDir` は物理パスやS3の完全キーではなく、テナント配下のサブフォルダだけを指定する。

許可例：

```text
master-data
monthly/master-data
```

拒否例：

```text
/var/backup
../backup
documents/backups/system
```

## 4.6 ファイル名

未設定時：

```text
{targetCode}_{timestamp}.csv
```

任意パターンで使用できる変数：

```text
{targetCode}
{timestamp}
```

`{timestamp}` は上書き防止のため必須とする。実際の値にはミリ秒とランダム8文字を含める。

例：

```text
{targetCode}_{timestamp}.csv
```

ファイル名に `/`、`\`、未定義の変数は使用できない。

## 4.7 バッチ実行

バッチ種別 `BACKUP` から実行する定義は、次のいずれかでなければならない。

```text
SERVER_FILE
BOTH
```

`DOWNLOAD` の定義をバッチ実行した場合は、ファイル消失を防ぐため設定エラーとして失敗させる。

保存成功時は、バッチ実行履歴にも次を記録する。

- StorageType
- ファイルキー
- ファイル名
- Content-Type
- ファイルサイズ

## 4.8 実行履歴

履歴へ次を保存する。

- 対象コード
- ファイル名
- CSV／ZIP
- ファイルサイズ
- StorageType
- 保存キー
- 成功／失敗
- 実行者
- 実行日時
- エラーメッセージ

手動実行はログインユーザー、定期実行は `system` を記録する。

画面取得は対象テナントの最新200件とする。

成功履歴の保存に失敗した場合：

1. 保存済みLOCAL／S3ファイルを削除
2. 可能であれば失敗履歴を保存
3. 元の例外を呼出元へ返す

DB履歴のない孤立ファイルをできる限り残さない。

## 4.9 複数対象のZIP

- 2件以上はZIP出力
- `zipRequired=true` の対象を含む場合もZIP出力
- ストレージ保存対象の `outputDir` は全件同じ値に統一
- 異なる保存先を同時選択した場合は実行を拒否

## 5. API

| Method | Path | 内容 |
|---|---|---|
| GET | `/api/system/backup/targets` | 有効な定義一覧 |
| GET | `/api/system/backup/targets/{id}` | 定義詳細 |
| POST | `/api/system/backup/targets` | 定義作成 |
| PUT | `/api/system/backup/targets/{id}` | 定義更新 |
| DELETE | `/api/system/backup/targets/{id}` | 論理削除 |
| POST | `/api/system/backup/execute` | 選択対象をCSV／ZIP出力 |
| GET | `/api/system/backup/histories` | 最新200件の履歴 |
| GET | `/api/system/backup/histories/{id}/file` | 保存済みファイル取得 |

すべて `SYS_ADMIN` 権限が必要。

## 6. DB変更

DDL：

```text
backend/src/main/resources/sql/system/backup/backup_v1.sql
```

主な変更：

- `backup_target` の一意制約を `tenant_id + target_code` に変更
- `backup_column` の `target_id + column_name` を一意化
- `backup_column` の `target_id + order_no` を一意化
- `backup_history` へテナント複合インデックスを追加

既存DBにはHibernateが作成した `target_code` 単独のUNIQUEインデックスが残る可能性がある。`SHOW INDEX FROM backup_target` でインデックス名を確認し、単独UNIQUEを削除してからDDLを適用する。

## 7. 既存マスターデータの確認

バッチ定義から参照されるバックアップ対象が `DOWNLOAD` の場合、次へ変更する。

```text
outputMode = SERVER_FILE
outputDir = master-data
```

対象確認SQLと更新例は `backup_v1.sql` にコメントで記載している。DB資産をまとめて整備する工程で、対象コードと保存先を確認してから適用する。

## 8. 画面変更

- バックアップメニューをSYS_ADMIN限定
- 作成後の対象コードとテーブル名を読取専用化
- 出力方法を日本語表示
- 保存先が書類管理配下であることを表示
- DBスキーマとの照合を説明
- ファイル名パターン、パス、文字数を画面でも検証
- サーバーが返した実ファイル名をダウンロード時に利用

## 9. テスト

追加確認：

- テナント対象SQLへ `tenant_id` 条件を強制
- 共通マスタにはテナント条件を付けない
- 不正なSQL識別子を拒否
- 書類管理から参照可能な保存キー生成
- ファイル名によるパストラバーサル拒否
- 存在しない物理カラムを拒否
- 対象コード、テーブル名の変更拒否
- `{timestamp}` のないファイル名パターンを拒否
- バッチ実行結果への保存ファイル情報設定
- DOWNLOADのみの定義をバッチ実行時に拒否

確認結果：

- バックアップ関連テスト：成功
- 対象フロントエンドESLint：成功
- バックアップ関連TypeScriptエラー：0件

フロントエンド全体の型検査には別機能の既存エラーが残っている。

## 10. デプロイ後の確認

1. SYS_ADMINだけがバックアップ画面を表示できる
2. 定義保存時に実在しないテーブル・カラムが拒否される
3. 対象コードとテーブル名を作成後に変更できない
4. テナント対象テーブルのCSVに他テナントデータが含まれない
5. DOWNLOADで実ファイル名のCSV／ZIPを取得できる
6. SERVER_FILE／BOTHで書類管理のバックアップ領域から参照できる
7. 履歴から保存済みファイルを再ダウンロードできる
8. BACKUPバッチでファイル情報がバッチ履歴へ記録される
9. DOWNLOAD定義のバッチ実行が失敗する
10. 複数対象のZIP保存先不一致が拒否される

## 11. V2・別工程

- 年度帳票バックアップLambda
- EventBridgeによる会計年度終了後の実行
- 対象帳票コードの設定マスター
- 7年保持とS3 Lifecycle
- バックアップ成功後の帳票履歴削除
- 大規模テーブルのストリーミングCSV出力
- バックアップファイルの暗号化キー個別管理
- 復元手順と復元テスト

## 12. 業務日時とClock

バックアップ機能の業務タイムゾーンは`Asia/Tokyo`とする。

アプリケーション共通の`Clock`を次へ適用した。

- CSVファイル名の日時
- ZIPファイル名の日時
- 成功履歴の実行日時
- 失敗履歴の実行日時
- バックアップ定義の論理削除日時
- バックアップ定義配下カラムの論理削除日時

ファイル名形式は変更しない。

```text
{targetCode}_yyyyMMdd_HHmmss_SSS_{random8}.csv
backup_yyyyMMdd_HHmmss_SSS_{random8}.zip
```

定義と配下カラムの削除日時には、同一のClockから取得した同一`Instant`を設定する。

追加テスト：

```text
BackupFileNameBuilderTest
BackupHistoryBuilderTest
BackupTargetCommandServiceTest
```

UTCでは12月31日、日本時間では翌年1月1日となる時刻を固定し、ファイル名が日本時間の新年で生成されることを確認した。

API、DBカラム、S3キー構造の変更はない。
