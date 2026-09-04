# ProjectAdminSystem インフラ構築・運用手順書 V1

## 1. この手順書の目的

このフォルダは、空のAWSアカウントからProjectAdminSystemの現在のDEV環境を再構築し、ローカルDocker環境とAWS環境の両方で動作確認できる状態まで進めるための正本である。

対象読者は、AWS、Terraform、Dockerを初めて扱う開発者・運用担当者とする。

## 2. 完了条件

次の状態をすべて満たしたとき、構築完了とする。

- AWS rootユーザーがMFAで保護され、rootアクセスキーが存在しない
- 日常作業をrootユーザーではなく管理用ユーザーで行える
- AWS Budgetsで課金通知を受け取れる
- Terraform stateが専用S3バケットで暗号化・バージョン管理される
- TerraformからVPC、EC2、RDS、S3、ECR、IAM、Secrets Manager、CloudWatch、SESを再現できる
- Cloudflare TunnelとCloudflare Accessで、許可された利用者だけがアプリへアクセスできる
- EC2上でBackend、Frontend、Redis、cloudflaredの4コンテナが稼働する
- MongoDB AtlasへEC2の固定Elastic IPから接続できる
- GitHub Actionsから長期AWSアクセスキーなしでDEVへデプロイできる
- ローカルではDocker ComposeだけでMySQL、MongoDB、Redis、Mailpit、Backend、Frontendを起動できる
- 起動、停止、ログ確認、障害調査の基本操作を実施できる

## 3. 読む順番

| 順番 | 文書 | 内容 |
|---:|---|---|
| 1 | [01-current-architecture-and-basics.md](01-current-architecture-and-basics.md) | 現行構成、用語、責任分界、費用発生箇所 |
| 2 | [02-aws-account-and-prerequisites.md](02-aws-account-and-prerequisites.md) | AWSアカウント作成、MFA、課金対策、ローカルツール |
| 3 | [03-terraform-build-procedure.md](03-terraform-build-procedure.md) | Terraform state基盤とAWS・Cloudflare資産の構築 |
| 3-A | [03-terraform-code-and-network-details.md](03-terraform-code-and-network-details.md) | 03の補足資料。実際のTerraformコード、HCL文法、State、ネットワーク、IAM、各Moduleの詳細解説 |
| 4 | [04-runtime-secrets-database-and-deployment.md](04-runtime-secrets-database-and-deployment.md) | DB、Secrets、MongoDB Atlas、SES、Docker、CI/CD |
| 5 | [05-operations-and-troubleshooting.md](05-operations-and-troubleshooting.md) | 起動停止、更新、監視、バックアップ、障害対応 |

## 4. 正本とする資産

この手順書と実装に差がある場合、まず次のファイルを確認する。

| 対象 | 正本 |
|---|---|
| AWSリソース | `infrastructure/environments/dev`、`infrastructure/modules` |
| Terraform state基盤 | `infrastructure/bootstrap` |
| AWS用Docker構成 | `infrastructure/runtime/dev/compose.yaml` |
| ローカルDocker構成 | `docker-compose.yml` |
| Backendイメージ | `backend/Dockerfile` |
| Frontendイメージ | `frontend/Dockerfile`、`frontend/nginx/default.conf` |
| Secrets変換 | `infrastructure/scripts/runtime/prepare_configtree.py` |
| DB追加資産 | `backend/src/main/resources/sql/runtime-schema-manifest.txt` |
| 手動デプロイ | `infrastructure/scripts/deployment` |
| 起動・停止 | `infrastructure/scripts/operations` |
| CI/CD | `.github/workflows/ci.yml`、`.github/workflows/deploy-dev.yml` |

## 5. 機密情報のルール

次の値はGit、Confluence、チャット、Terraformのtfvarsへ貼らない。

- AWSパスワード、アクセスキー、セッショントークン
- Terraform stateとplanファイル
- Cloudflare API Token、Tunnel Token
- MongoDBユーザー名、パスワード、接続URI
- JWT Secret、OpenAI API Key、SES SMTPパスワード
- Syncfusion License Key

秘密値を誤って共有した場合は、削除だけで済ませず必ず発行元でローテーションする。

## 6. 文書の更新ルール

- TerraformまたはDocker構成を変更したPRでは、このフォルダへの影響を確認する
- AWSコンソールからTerraform管理対象を変更しない
- やむを得ず手動変更した場合はTerraformへ反映し、`terraform plan`を差分なしに戻す
- AWS、Cloudflare、GitHubの画面名は変わるため、画面名より「目的」と「確認結果」を優先する
- コマンド例のID、ARN、ドメイン、メールアドレスは各環境の実値へ置き換える
