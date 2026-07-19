# ProjectAdminSystem Infrastructure

ProjectAdminSystemのAWSインフラをTerraformで管理する。

## 構成

- `bootstrap`: Terraform state管理基盤
- `environments/dev`: dev環境
- `modules/document_bucket`: 書類管理用S3モジュール
- `modules/backend_runtime_iam`: バックエンドECSタスクロール・書類管理S3最小権限ポリシー
- `modules/container_repository`: バックエンドコンテナイメージ用ECRプライベートリポジトリ
- `modules/ecs_task_execution_iam`: ECR pull・CloudWatch Logs送信用ECSタスク実行ロール

## AWSリージョン

`ap-northeast-1`

## 認証

長期アクセスキーは使用しない。

AWS CLIの`aws login`と一時認証情報を使用する。

## 禁止事項

- Terraform stateをGitへ登録しない
- AWS認証情報をTerraformへ記載しない
- AWSコンソールからTerraform管理対象を手動変更しない
- `terraform apply`前にplanを確認する

## Cloudflare Terraform管理

- Cloudflare Providerは`5.22.x`へ固定する。
- API Tokenは`CLOUDFLARE_API_TOKEN`として実行時だけ渡し、Git、tfvars、Confluenceへ保存しない。
- 許可メールアドレスは`TF_VAR_cloudflare_allowed_emails`として渡し、Git管理ファイルへ実値を記載しない。
- 既存のTunnel、Tunnel設定、DNS、Access application、Access policy、One-time PINは新規作成せず、`scripts/cloudflare/import_existing_resources.sh`でstateへ取り込む。
- import後は必ず`terraform plan`を確認し、意図しない作り直しや設定変更がない状態にしてからapplyする。
- state管理用S3へ業務書類を保存しない
