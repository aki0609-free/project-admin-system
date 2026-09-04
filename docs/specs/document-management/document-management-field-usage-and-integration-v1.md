# 書類管理 領域・パラメーターの利用先 V1

ドメイン：書類管理

## 1. 共通パラメーター

| パラメーター | 用途 | 検証・注意 |
|---|---|---|
| `area` | 論理領域と操作権限を決定 | Enum必須、未知値は変換エラー |
| `path` | 領域内の相対パス | ルート操作禁止、相対脱出禁止 |
| `name/newName` | 作成・変更名称 | 必須、`/`・`\`・`.`・`..`・制御文字禁止 |
| `directory` / `isFile` | ファイルかフォルダか | コピー、削除、ZIP判定に使用 |
| `continuationToken` | 一覧ページ継続 | LOCALは数値offset、S3はS3 Token |
| `maxKeys` | 1ページ件数 | 1～1000 |
| `searchString` | 名前検索 | `*`だけの場合は現在フォルダー配下の全件、それ以外は大小文字を無視した部分一致 |
| `uploadFiles` | Syncfusion upload | 通常50MB以下、取込スクリプト1MB以下 |

## 2. 領域別の意味

### 会社書類

- 利用者が任意のフォルダを作成し、契約書・社内資料等を保管する。
- ファイルの業務メタデータDBはなく、S3/LOCALのキー・名前・サイズ・更新日時が一覧の正本。
- 書類と顧客・従業員等をIDで関連付ける機能はない。

### 生成帳票

- 帳票基盤・月次締めが生成した成果物の参照口。
- 書類管理から変更できない。
- 帳票コード、対象月、履歴Version等の構造はファイル生成側が決定する。

### バックアップ

- 年度帳票およびシステムバックアップの参照口。
- 書類管理から削除・移動・名称変更できない。
- DBの実行履歴や保持期限を変更する画面ではない。

### テンプレート

- 帳票・台帳テンプレートを参照する。
- 現在は参照専用のため、テンプレート更新は各管理機能または配置処理を使う。

### 取込スクリプト

- `.py`と`.sh`だけを許可し、最大1MB。
- 保存しただけでは実行されず、外部データ取込マスターの`scriptType`と`scriptPath`が実行対象を決める。
- LOCALでは`{localBasePath}/{imports.script.path}/{scriptPath}`、S3では`imports/scripts/{scriptPath}`を解決する。

## 3. 保存設定

| 設定 | 既定値 | 利用先 |
|---|---|---|
| `project.storage.default-type` | LOCAL相当 | 新規読書きのBackend |
| `project.storage.local-base-path` | `storage` | LOCALの基準ディレクトリ |
| `project.storage.s3.enabled` | false | S3 Backend登録 |
| `project.storage.s3.bucket` | 環境設定 | S3 Bucket |
| `project.storage.template.path` | `documents/templates/reports` | 帳票テンプレート |
| `project.storage.output.path` | `documents/generated-reports/reports` | 生成帳票 |
| `project.storage.imports.script.path` | `imports/scripts` | 取込スクリプト |
| `project.storage.imports.csv.path` | `imports/csv` | 外部データ取込CSV |
| `project.storage.document.root-path` | `documents` | 書類領域の基準 |

旧`project.storage.type`も互換用に残り、`default-type`未設定時のみ使用する。

## 4. ファイル情報

書類一覧が返す主な値：

| 値 | LOCAL | S3 |
|---|---|---|
| path/name | ファイルPathから算出 | Object keyから算出 |
| directory | `Files.isDirectory` | key末尾`/`またはCommonPrefix |
| size | `Files.size` | `S3Object.size` |
| lastModified | filesystem更新日時 | S3 lastModified |
| eTag | NULL | S3 ETag |
| createdAt | 保持しない | 保持しない。Syncfusionでは更新日時を作成日時としても表示 |

## 5. 同名登録と削除

- upload、コピー、移動、名称変更は同名のファイルまたはフォルダーを拒否する。
- ファイルを差し替える場合は、既存ファイルを名称変更または削除してからuploadする。無確認の上書きは行わない。
- S3 Versioningが有効なため、同一キー上書き・削除後も旧VersionはS3内部に残り得るが、現画面からVersion選択・復元はできない。
- 年度帳票バックアップ配下だけはLifecycleで現行・非現行Versionの期限管理を行う。その他領域に一般的な非現行Version削除ルールはない。

## 6. 認証・認可

- Controller：`hasRole('SYS_ADMIN')`
- 領域操作：`DocumentAreaPolicy`
- S3：EC2 Instance Role/IAM Policy
- S3 Bucket：Public Access Block、TLS以外をBucket Policyで拒否
- BrowserはS3へ直接接続せず、Backend APIのみ利用

## 7. 操作監査

upload、download、フォルダー作成、copy、move、rename、deleteは、成功・失敗、操作、領域、相対パス、利用者ID、Tenant IDを構造化したアプリケーションログへ記録する。AWSではBackendログをCloudWatch Logsから追跡する。ファイル内容や認証情報は記録しない。
