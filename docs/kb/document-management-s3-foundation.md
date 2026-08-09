# 書類管理（S3）共通基盤

## 1. 目的

ProjectAdminSystem V1の管理者向け書類管理機能において、ローカル検証環境とAWS環境で共通利用できるストレージ基盤を提供する。

管理画面にはSyncfusion Essential JS 2 FileManagerを使用する。ただし、ストレージ操作・アクセス制御・S3キー設計はSyncfusionへ依存させず、既存の書類管理サービスをアダプター経由で利用する。

## 2. 対象範囲

本KBの対象は次のとおり。

- 書類領域の定義
- S3キーの生成とパストラバーサル防止
- ファイル／フォルダの一覧取得
- アップロード、作成、コピー、移動、名称変更、削除
- ダウンロード、検索
- ローカルストレージとS3の共通インターフェース
- 管理者専用API
- Syncfusion FileManager向けアダプターAPI
- 「管理者メニュー → 書類管理」画面

次の項目は別工程とする。

- Syncfusion Spreadsheetの導入
- 既存の生成帳票を新しいS3キーへ移行する処理
- 年次バックアップLambdaとの接続
- 台帳マスタ、台帳テンプレート、台帳生成処理

## 3. S3フォルダ構成

```text
documents/
├── general/                    # 自由書類
├── generated-reports/         # システム生成帳票
├── backups/
│   ├── reports/               # 年次帳票バックアップ
│   └── system/                # システムバックアップ
└── templates/
    ├── ledgers/
    │   └── {ledgerCode}/
    │       └── template.json   # Syncfusion Spreadsheet JSON
    └── reports/                # 帳票テンプレート
```

以下の既存領域はFileManagerへ表示しない。

- `_deployment/`
- 既存の`reports-output/`
- `imports/`
- その他、アプリケーション内部処理用の領域

## 4. 操作権限

FileManager自体は`SYS_ADMIN`ロールだけが利用できる。

| 領域 | 閲覧 | 検索 | 詳細 | DL | UP | フォルダ作成 | コピー | 移動 | 名称変更 | 削除 |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| 自由書類 | ○ | ○ | ○ | ○ | ○ | ○ | ○ | ○ | ○ | ○ |
| 生成帳票 | ○ | ○ | ○ | ○ | × | × | × | × | × | × |
| バックアップ | ○ | ○ | ○ | ○ | × | × | × | × | × | × |
| テンプレート | ○ | ○ | ○ | ○ | × | × | × | × | × | × |

生成帳票、バックアップ、テンプレートは、業務処理や専用管理画面だけが更新する。FileManagerから変更するとDB履歴やマスタとの整合性が崩れるため、参照専用とする。

## 5. 設定

`application.yml`の設定値は次のとおり。

```yaml
project:
  storage:
    document:
      root-path: documents
      general-path: general
      generated-reports-path: generated-reports
      backups-path: backups
      templates-path: templates
```

環境ごとのストレージ切替は既存の`project.storage.default-type`を使用する。

- ローカル検証環境：`LOCAL`
- AWS環境：`S3`

## 6. API

ベースURL：

```text
/api/admin/documents
```

すべてのAPIに`SYS_ADMIN`ロールが必要。

| Method | Path | 用途 |
|---|---|---|
| GET | `/areas` | 領域と許可操作の取得 |
| GET | `/{area}/entries` | フォルダ直下の一覧 |
| GET | `/{area}/search` | 領域内検索 |
| GET | `/{area}/download` | ファイルダウンロード |
| POST | `/{area}/directories` | フォルダ作成 |
| POST | `/{area}/upload` | ファイルアップロード |
| POST | `/{area}/copy` | コピー |
| POST | `/{area}/move` | 移動 |
| PATCH | `/{area}/rename` | 名称変更 |
| DELETE | `/{area}` | 削除 |

領域識別子：

```text
GENERAL
GENERATED_REPORTS
BACKUPS
TEMPLATES
```

画面は`GET /areas`が返す許可操作を利用してボタンを制御する。ただし、バックエンドでも同じ制御を必ず実行する。

### 6.1 Syncfusion FileManagerアダプター

ベースURL：

```text
/api/admin/documents/file-manager
```

| Method | Path | 用途 |
|---|---|---|
| POST | `/{area}/operations` | 一覧、検索、詳細、作成、コピー、移動、名称変更、削除 |
| POST | `/{area}/upload` | FileManager形式のマルチパートアップロード |
| POST | `/{area}/download` | 単一ファイルまたはZIPダウンロード |

アダプターはSyncfusion固有のリクエストを`DocumentManagementService`の操作へ変換する。S3 SDKやローカルファイルシステムをコントローラーから直接操作しない。

アップロード上限：

```yaml
spring:
  servlet:
    multipart:
      max-file-size: 50MB
      max-request-size: 55MB
```

### 6.2 フロントエンド

- 画面URL：`/admin/document`
- メニュー：`管理者メニュー → 書類管理`
- 必要ロール：`SYS_ADMIN`
- Syncfusionパッケージ：`@syncfusion/ej2-vue-filemanager`
- 表示形式：詳細表示
- サムネイル：無効
- 1ファイル上限：50MB
- 認証：FileManagerの全Ajax処理へJWTと`X-Tenant-ID`を付与
- ダウンロード：フォームPOSTを使わずAjax方式で認証ヘッダーを付与

初期バンドル肥大化を避けるため、書類管理画面は遅延読み込みする。

## 7. 安全対策

- `.`、`..`、制御文字を含むパスを拒否する
- 書類領域のルート自体は削除、移動、名称変更の対象にしない
- フォルダ自身の配下へのコピー／移動を拒否する
- 移動はコピー完了後にコピー元を削除する
- コピーに失敗した場合、コピー元を削除しない
- S3の403エラーを「ファイルなし」と誤判定しない
- S3一覧はページングへ対応し、1000オブジェクトを超えても処理できる
- APIレスポンスでは物理S3キーの`documents/{area}/`部分を隠す
- S3 CopyObjectのコピー元キーは日本語と空白を含めてURLエンコードする

## 8. 動作確認

関連テスト：

```text
LocalStorageServiceTest
S3StorageServiceTest
StorageServiceTest
DocumentAreaPolicyTest
DocumentStorageKeyResolverTest
DocumentManagementServiceTest
SyncfusionFileManagerServiceTest
```

確認内容：

- ローカルとS3の階層一覧
- ページング
- 日本語ファイル名のコピー
- S3の404／403判定
- パストラバーサル拒否
- 領域ごとの操作制御
- 物理S3キーの非公開化
- コピー失敗時に移動元を保持
- ルート削除の拒否
- Syncfusion形式の一覧、作成、名称変更、検索、詳細、削除
- 単一ファイル／複数項目ZIPのダウンロード
- 参照専用領域の更新拒否
- ローカル画面でフォルダ作成、名称変更、削除
- バックアップ領域で更新系ボタンが表示されないこと

ローカル起動：

```bash
cd /Users/tatsukiakiyama/Documents/ProjectAdmin/public-repository
docker compose --env-file frontend/.env.local up --build -d
```

確認URL：

```text
http://localhost:5173/admin/document
```

フロントエンドのSyncfusionライセンスキーは`frontend/.env.local`の環境変数で渡し、Gitへコミットしない。

Docker上のViteビルドはSyncfusion追加後のメモリ使用量を考慮し、`NODE_OPTIONS=--max-old-space-size=4096`を設定している。

### 8.1 AWS DEV実環境確認

2026-07-28にCloudflare Access経由のAWS DEVで確認した。

- backendイメージ：`manual-20260727-132114Z`
- frontendイメージ：`manual-20260727-132641Z`
- backend、frontend、Redis、Cloudflare Tunnel：正常
- Actuator：`UP`
- backend再起動回数：0

自由書類で確認した操作：

1. FileManagerから検証フォルダを作成
2. `documents/general/{検証フォルダ}/`のS3ディレクトリマーカーを確認
3. FileManagerからフォルダ名称を変更
4. S3で旧キーが削除され、新キーが作成されたことを確認
5. 非機密の検証テキストをS3へ一時配置
6. FileManagerの一覧へファイル名、更新日時、サイズが表示されることを確認
7. FileManagerからダウンロード操作を実行
8. FileManagerからフォルダを削除
9. S3上でフォルダと配下ファイルが残っていないことを確認
10. FileManagerのアップロードボタンからJPEGファイルを手動アップロード
11. S3でファイルサイズ、`image/jpeg`、AES256暗号化、Version IDを確認
12. FileManagerの一覧表示とダウンロードを確認
13. FileManagerからアップロードファイルを削除

バックアップ領域で確認した内容：

- 「参照専用」と表示される
- 新しいフォルダー、アップロード、削除、名称変更などの更新ボタンが表示されない
- ダウンロード、並べ替え、再読み込み、表示、詳細だけが表示される

ブラウザ自動操作ではOSのファイル選択ダイアログを操作できないため、ファイル選択だけを手動で行い、以降のS3保存確認、一覧、ダウンロード、削除は上記手順で確認した。

S3バージョニングが有効なため、FileManagerの通常削除後には旧バージョンと削除マーカーが残る。今回の検証データはキーとVersion IDを限定し、旧バージョンと削除マーカーまで完全削除した。業務書類の削除では復旧性を維持するため、通常はVersion IDを指定した完全削除を行わない。

検証用のS3オブジェクト、旧バージョン、削除マーカー、ローカル一時ファイルは確認後にすべて削除した。

## 9. Syncfusion接続方針

FileManagerが要求する形式への変換は`SyncfusionFileManagerService`へ閉じ込める。

既存の`DocumentManagementService`を利用し、画面ライブラリを変更してもS3キー、権限、整合性ルールを維持する。

`GET /api/admin/documents/areas`の許可操作を画面へ反映し、バックエンドの`DocumentAreaPolicy`でも同じ権限を再検証する。画面上のボタン非表示だけに依存しない。

## 10. 台帳管理との境界

「システム運用 → 台帳管理」は次の責務だけを持つ。

- 台帳マスタCRUD
- Spreadsheetテンプレート編集
- 変数マッピング設定
- データ取得方法の設定
- 更新ロジックの設定
- テストプレビュー

実際の月次台帳作成は「締め処理 → 台帳」で行う。

SpreadsheetテンプレートはExcelファイルではなく、Syncfusion SpreadsheetのJSONとして次へ保存する。

```text
documents/templates/ledgers/{ledgerCode}/template.json
```

テンプレートの更新は台帳管理専用APIから行い、FileManagerでは参照／ダウンロードだけを許可する。

## 11. 完了状況と次工程

完了：

1. Syncfusionライセンスの環境変数登録
2. FileManager画面
3. FileManagerバックエンドアダプター
4. ローカルストレージでの主要操作確認
5. 領域別の参照専用制御
6. AWS DEVへの最新版デプロイ
7. AWS S3での一覧、フォルダ作成、名称変更、ダウンロード、再帰削除
8. Cloudflare Access経由の画面確認
9. FileManagerからAWS S3への実ファイルアップロード

次工程：

1. Spreadsheetによる台帳マスタ画面を設計
2. 新規生成帳票を`documents/generated-reports/reports`へ接続（完了）
3. 年次バックアップを`backups/reports`へ接続
4. 年次バックアップLambdaを接続

既存の帳票履歴に保存されている旧キーは変更しない。
ダウンロード処理は履歴に記録された`storage_type`と`stored_file_key`を使用するため、
旧`reports-output`配下のファイルも引き続き参照できる。
新しく生成する帳票から書類管理の「生成帳票」領域へ保存する。
