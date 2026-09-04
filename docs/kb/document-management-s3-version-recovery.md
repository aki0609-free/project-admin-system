# 書類管理 S3 Version確認・復元手順

## 1. 用途

会社書類、生成帳票、テンプレート、取込スクリプト、バックアップを誤って更新・削除した場合に、S3 Versioningから復元するためのAWS管理者向け手順である。

書類管理画面にはVersion履歴の表示・復元機能を持たせない。通常利用者が誤って過去版へ戻さないよう、復元はAWS管理者が実施する。

## 2. 前提

- AWS CLIの`project-admin-dev`または`project-admin-terraform`プロファイルへログイン済みであること
- 対象バケットが`project-admin-dev-documents-ff0dd38f`であること
- 復元対象の論理パスが分かること
- 復元前に現在のVersion IDを記録すること

## 3. Version一覧を確認

例として`documents/general/contracts/example.pdf`を確認する。

```bash
AWS_PROFILE=project-admin-dev aws s3api list-object-versions \
  --bucket project-admin-dev-documents-ff0dd38f \
  --prefix documents/general/contracts/example.pdf
```

確認する値：

- `Versions[].VersionId`：保存された各版の識別子
- `Versions[].IsLatest`：現在表示される版か
- `Versions[].LastModified`：更新日時
- `DeleteMarkers[]`：画面やAPIで削除された記録

## 4. 過去版を安全に確認

復元候補を一時ファイルへダウンロードし、内容を確認する。

```bash
AWS_PROFILE=project-admin-dev aws s3api get-object \
  --bucket project-admin-dev-documents-ff0dd38f \
  --key documents/general/contracts/example.pdf \
  --version-id VERSION_ID \
  /tmp/project-admin-recovery-example.pdf
```

この操作だけでは現行ファイルを変更しない。

## 5. 過去版を現行版として復元

確認済みの過去Versionを同じキーへコピーすると、その内容を持つ新しい現行Versionが作られる。既存Version自体は破壊しない。

```bash
AWS_PROFILE=project-admin-dev aws s3api copy-object \
  --bucket project-admin-dev-documents-ff0dd38f \
  --copy-source 'project-admin-dev-documents-ff0dd38f/documents/general/contracts/example.pdf?versionId=VERSION_ID' \
  --key documents/general/contracts/example.pdf
```

日本語・空白を含むキーでは`copy-source`のURLエンコードが必要になるため、AWSコンソールの「バージョンを表示」から対象Versionをダウンロードし、書類管理画面へ別名で登録して確認してから差し替える方法を推奨する。

## 6. 削除したファイルを復元

削除マーカーのVersion IDを確認し、その削除マーカーだけを削除する。

```bash
AWS_PROFILE=project-admin-dev aws s3api delete-object \
  --bucket project-admin-dev-documents-ff0dd38f \
  --key documents/general/contracts/example.pdf \
  --version-id DELETE_MARKER_VERSION_ID
```

`--version-id`を省略すると新しい削除マーカーが増えるため、必ず対象IDを指定する。

## 7. 復元後の確認

1. 書類管理画面を再読み込みする。
2. ファイル名、容量、更新日時を確認する。
3. ダウンロードして内容を確認する。
4. CloudWatch Logsで復元後のdownload操作を確認する。
5. 実施日時、対象キー、復元元Version ID、担当者を運用記録へ残す。

## 8. 注意事項

- Lifecycle期限を過ぎて削除された非現行Versionは復元できない。
- 年次帳票バックアップを手動で削除・上書きしない。
- Terraformが管理するLifecycleをAWSコンソールから直接変更しない。
- ウイルススキャンはV1対象外であり、復元ファイルも信頼できる業務ファイルだけを使用する。
