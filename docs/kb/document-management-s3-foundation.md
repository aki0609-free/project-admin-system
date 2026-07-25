# 書類管理（S3）共通基盤

## 1. 目的

ProjectAdminSystem V1の管理者向け書類管理機能において、ローカル検証環境とAWS環境で共通利用できるストレージ基盤を提供する。

将来、Syncfusion Essential JS 2 FileManagerを接続するが、ストレージ操作・アクセス制御・S3キー設計はSyncfusionへ依存させない。

## 2. 対象範囲

本KBの対象は次のとおり。

- 書類領域の定義
- S3キーの生成とパストラバーサル防止
- ファイル／フォルダの一覧取得
- アップロード、作成、コピー、移動、名称変更、削除
- ダウンロード、検索
- ローカルストレージとS3の共通インターフェース
- 管理者専用API

次の項目は別工程とする。

- Syncfusion FileManagerの画面実装
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

## 9. Syncfusion導入時の接続方針

Syncfusionのライセンス取得後、FileManagerが要求するリクエスト／レスポンス形式へ変換するアダプターを追加する。

既存の`DocumentManagementService`を利用し、FileManagerのコントローラーからS3 SDKを直接呼び出さない。

これにより、画面部品を変更しても、S3キー、権限、整合性ルールを維持できる。

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

## 11. 次工程

1. Syncfusionライセンス取得完了の確認
2. フロントエンドへSyncfusionのライセンス登録方法を追加
3. FileManager画面とバックエンドアダプターを追加
4. ローカルストレージで全操作を確認
5. AWS S3で一覧、アップロード、ダウンロードを確認
6. Spreadsheetによる台帳マスタ画面を設計
7. 既存生成帳票と年次バックアップを新しい領域へ接続
