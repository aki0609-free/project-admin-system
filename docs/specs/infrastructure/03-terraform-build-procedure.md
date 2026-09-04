# 03. TerraformによるAWS・Cloudflare構築

## 1. この章の完了条件

- Terraform state専用S3バケットが存在する
- BootstrapとDEVのstateがS3 Backendへ保存される
- `terraform plan`の内容を確認してからapplyしている
- VPC、EC2、RDS、S3、ECR、IAM、Secrets、CloudWatch、SESが作成される
- Cloudflare Tunnel、DNS、Access Application、OTPが作成される
- Terraform Outputsから後続作業に必要な値を取得できる

## 2. Terraformが担当する範囲

Terraformは「存在すべきインフラ」をコードから作る。DB内の業務データや秘密値そのものは作らない。

### 2.1 Terraformが作るもの

- Terraform state用S3
- VPC、Subnet、Route Table、Internet Gateway、Security Group
- EC2、EBS、Elastic IP、EC2 Instance Role
- RDS MySQL、Parameter Group、Subnet Group、RDS管理Master Secret
- 書類用S3とBackend用S3権限
- Backend / Frontend ECR
- Application DB Secretの器、Runtime Secretの器
- CloudWatch Logs、Dashboard、Alarm
- Cloudflare Tunnel、Tunnel Config、DNS、Access、One-time PIN
- SES Domain Identity、DKIM DNS、SMTP IAM User
- GitHub Actions OIDC Provider、Deploy Role、権限

### 2.2 Terraform適用後に手動またはScriptで入れるもの

- DBアプリユーザーとパスワード
- Application DB SecretのJSON値
- Runtime SecretのJSON値
- Cloudflare Tunnel Token
- MongoDB AtlasのUser、URI、IP Access List
- SES SMTP Access Keyから変換したSMTP Credential
- SES Sandbox解除申請
- GitHub Environment Variables / Secrets
- 初回DB SchemaとMaster Data
- Docker Imagesと実行中Containers

## 3. 作業前の禁止事項

- `terraform.tfstate`、`.tfstate.backup`、`.tfplan`をGitへ追加しない
- Token、Password、API Keyを`.tf`や`.tfvars`へ書かない
- `terraform apply`をplanなしで実行しない
- `Plan: ... to destroy` が1件でもあれば理由を確認せずapplyしない
- AWS ConsoleからTerraform管理リソースを並行変更しない
- `terraform destroy`を実行しない
- EC2とRDSには削除保護があるため、無理に解除しない

## 4. 作業変数を決める

次の値を安全な作業メモへ記録する。秘密値ではないが、実環境の識別情報なのでPublicな文書へ固定しない。

```text
AWS_REGION=ap-northeast-1
AWS_PROFILE=project-admin-terraform
AWS_ACCOUNT_ID=<12桁Account ID>
STATE_BUCKET=<世界で一意なTerraform state用Bucket名>
DOCUMENT_BUCKET=<世界で一意な書類用Bucket名>
CLOUDFLARE_ACCOUNT_ID=<Cloudflare Account ID>
CLOUDFLARE_ZONE_ID=<Domain Zone ID>
CLOUDFLARE_ZONE_NAME=<取得したDomain>
GITHUB_REPOSITORY=<owner/repository>
GITHUB_REPOSITORY_OWNER_ID=<数値ID>
GITHUB_REPOSITORY_ID=<数値ID>
```

S3 Bucket名は全AWSアカウントで一意である。英小文字、数字、ハイフンを使い、用途・環境・推測困難なSuffixを含める。

例:

```text
project-admin-dev-tfstate-<random>
project-admin-dev-documents-<random>
```

## 5. AWS認証を確認する

```bash
cd /Users/tatsukiakiyama/Documents/ProjectAdmin/public-repository

export AWS_PROFILE=project-admin-terraform
export AWS_REGION=ap-northeast-1

aws sts get-caller-identity
```

Account IDが構築対象と一致しない場合は中止する。

## 6. Terraform state基盤を構築する

### 6.1 なぜ最初に別構築するのか

TerraformはstateをS3へ保存するが、そのS3 Bucket自体がまだ存在しない。最初の1回だけLocal stateでBucketを作り、その後Local stateを作成済みS3へ移す。
この最初の処理をBootstrapと呼ぶ。

### 6.2 tfvarsを作る

```bash
cd infrastructure/bootstrap
cp terraform.tfvars.example terraform.tfvars
```

`terraform.tfvars` の値を変更する。

```hcl
state_bucket_name = "<STATE_BUCKET>"
```

### 6.3 Local stateでState Bucketを作る

```bash
terraform fmt -check
terraform init -backend=false
terraform validate
terraform plan -out=tfplan-bootstrap
terraform apply "tfplan-bootstrap"
```

想定される資産:

- S3 Bucket
- Public Access Block
- Bucket owner enforcement
- Versioning
- AES256 server-side encryption
- HTTPS以外を拒否するBucket Policy

Bucketには`prevent_destroy = true`が設定される。

### 6.4 Bootstrap stateをS3へ移す

```bash
terraform init -migrate-state \
  -backend-config="bucket=<STATE_BUCKET>" \
  -backend-config="key=project-admin/bootstrap/terraform.tfstate" \
  -backend-config="region=ap-northeast-1" \
  -backend-config="use_lockfile=true"
```

移行確認に同意する。完了後にplanが差分なしであることを確認する。

```bash
terraform plan
```

S3 Backendはstate lockingを使用でき、Versioningは誤削除・人為ミスからの復旧に有効である。[HashiCorp公式: S3 Backend](https://developer.hashicorp.com/terraform/language/backend/s3)

Localに残ったstateやplanはGitへ追加しない。安全なS3移行を確認してから不要なplanだけを削除する。

## 7. Cloudflareを準備する

### 7.1 DomainをCloudflareでActiveにする

1. CloudflareへDomainを追加する。
2. Registrar側でCloudflare指定のNameserverへ変更する。
3. CloudflareのZone Statusが `Active` になるまで待つ。
4. Account IDとZone IDをCloudflare Overviewから控える。
5. Zero Trust Organizationを作る。

### 7.2 Terraform用API Tokenを作る

Cloudflare Profile → API Tokens → Create Tokenから、対象Accountと対象Zoneだけに限定したTokenを作る。

本構成に必要な操作は次の種類である。

- Account: Cloudflare Tunnelの作成・編集
- Account: Access Application / Policyの作成・編集
- Account: Access Identity Providerの作成・編集
- Zone: Zoneの参照
- Zone: DNS Recordの作成・編集

Cloudflareの画面上の権限名が変わった場合は、上記リソースを作成できる最小権限を選ぶ。全Account・全Zone対象にしない。

Tokenは作成直後に一度しか表示されない。Password Managerへ保存し、Git、tfvars、Shell履歴、KBへ残さない。下の `read -s` は入力値を画面とShell履歴へ残さない。

### 7.3 実行時だけ環境変数へ設定する

```bash
read -r -s "CLOUDFLARE_API_TOKEN?Cloudflare API Token: "
echo
export CLOUDFLARE_API_TOKEN
export TF_VAR_cloudflare_allowed_emails='["authorized-user@example.com"]'
```

メールアドレスを複数許可する場合:

```bash
export TF_VAR_cloudflare_allowed_emails='["user1@example.com","user2@example.com"]'
```

Shell終了後は環境変数が消える。`.zshrc`へ秘密値を固定しない。

Cloudflare TunnelはOriginからCloudflareへ外向き接続するため、EC2の受信ポートを開けずに公開できる。[Cloudflare公式: Private Web Application](https://developers.cloudflare.com/cloudflare-one/setup/secure-private-apps/private-web-app/)

## 8. GitHub Repositoryの数値IDを取得する

GitHub CLIへログインする。

```bash
gh auth status
```

Repository情報を取得する。

```bash
gh api repos/<OWNER>/<REPOSITORY> \
  --jq '{repository_id: .id, owner_id: .owner.id, full_name: .full_name}'
```

Repositoryを新しく作り直すと、名前が同じでもRepository IDが変わる。tfvarsの数値IDを必ず新しいRepositoryに合わせる。

## 9. DEV用tfvarsを作る

```bash
cd /Users/tatsukiakiyama/Documents/ProjectAdmin/public-repository/infrastructure/environments/dev
cp terraform.tfvars.example terraform.tfvars
```

例:

```hcl
document_bucket_name        = "<DOCUMENT_BUCKET>"
cloudflare_account_id       = "<CLOUDFLARE_ACCOUNT_ID>"
cloudflare_zone_id          = "<CLOUDFLARE_ZONE_ID>"
cloudflare_zone_name        = "<CLOUDFLARE_ZONE_NAME>"
cloudflare_tunnel_name      = "project-admin-dev-ec2"
github_repository           = "<OWNER>/<REPOSITORY>"
github_repository_owner_id  = "<GITHUB_REPOSITORY_OWNER_ID>"
github_repository_id        = "<GITHUB_REPOSITORY_ID>"
```

コピー元にある `cloudflare_allowed_emails` のブロックは、実際の `terraform.tfvars` から削除する。実メールは `TF_VAR_cloudflare_allowed_emails` だけから渡す。

Terraformでは `terraform.tfvars` の値が `TF_VAR_...` より優先されるため、tfvarsに空配列やサンプルメールを残すと、環境変数の許可メールが使われない。

`terraform.tfvars.example`へ実値を上書きしない。

## 10. DEV Backendを初期化する

```bash
terraform init \
  -backend-config="bucket=<STATE_BUCKET>" \
  -backend-config="key=project-admin/dev/terraform.tfstate" \
  -backend-config="region=ap-northeast-1" \
  -backend-config="use_lockfile=true"
```

`terraform init`はProviderを取得しBackendを準備する。何度実行してもよい。[HashiCorp公式: terraform init](https://developer.hashicorp.com/terraform/cli/commands/init)

## 11. Format・Validate・Planを実行する

```bash
terraform fmt -check -recursive ../../
terraform validate
terraform plan -out=tfplan-dev
```

### 11.1 Planの確認項目

- Providerが `aws ~> 6.53.0`、`cloudflare ~> 5.22.0`
- Regionが `ap-northeast-1`
- Resourceが対象Accountだけに作成される
- RDSがPublicではない
- App Security Groupに `0.0.0.0/0` からの受信許可がない
- RDS Security GroupはApp Security Groupからの3306だけ
- S3 Public Access Blockが有効
- Cloudflare Originが `http://frontend:8080`
- Cloudflare Access許可メールが正しい
- GitHub RepositoryとEnvironmentが正しい
- 意図しない `destroy`、`replace` がない

Plan Fileには機密情報が含まれる可能性があるためCommitしない。

## 12. Applyする

Planが正しい場合だけ実行する。

```bash
terraform apply "tfplan-dev"
```

EC2、RDS、Cloudflare、SES DKIMの作成には時間がかかる。途中でTerminalを閉じない。Apply失敗時に同じplanを機械的に再実行せず、エラー原因と実際に作成済みの資産を確認する。

## 13. Outputsを保存する

```bash
terraform output
```

後続作業で最低限必要な値:

```bash
terraform output -raw app_instance_id
terraform output -raw app_elastic_ip
terraform output -raw mysql_address
terraform output -raw mysql_application_secret_name
terraform output -raw application_runtime_secret_name
terraform output -raw document_bucket_name
terraform output -raw backend_ecr_repository_url
terraform output -raw frontend_ecr_repository_url
terraform output -raw github_actions_deploy_role_arn
terraform output -raw cloudflare_application_hostname
terraform output -raw ses_smtp_iam_user_name
```

Outputsはこの環境の識別情報である。PublicなREADMEへ実値を固定しない。

## 14. Apply直後のAWS確認

### 14.1 EC2

- Instance State: Running
- Instance Type: `t3a.medium`
- IAM Roleが付いている
- Elastic IPが付いている
- Security Groupに公開受信Ruleがない
- Systems Manager Managed NodeとしてOnline

### 14.2 RDS

- Status: Available
- Engine: MySQL 8.0
- Publicly accessible: No
- Encryption: Enabled
- Deletion protection: Enabled
- Backup retention: 7 days
- Security Group: App Security Groupのみ3306許可

### 14.3 S3 / ECR / Secrets

- 書類BucketのPublic Access Blockが有効
- Backend / Frontend ECRが存在
- Application DB SecretとRuntime Secretは存在するが、まだ値が未設定
- RDS Master SecretはRDS管理で自動生成済み

### 14.4 Cloudflare

- Tunnelは存在するが、TokenをRuntimeへ登録するまでInactiveでよい
- DNS CNAMEはTunnel IDを向いている
- Access ApplicationのDomainが正しい
- Allow PolicyのEmailが正しい
- Catch-allは `http_status:404`

### 14.5 SES

- Verified Identityが存在
- DKIM CNAMEが3件作成済み
- DKIM StatusがPendingの場合はDNS反映を待つ

## 15. 既存Cloudflare資産を取り込む場合だけ

空の環境ではこの節を使わない。先にCloudflare画面からTunnel、DNS、Accessを手作業で作っていた場合だけ、Terraformが二重作成しないようimportする。

```bash
infrastructure/scripts/cloudflare/import_existing_resources.sh
```

必要な環境変数はScript冒頭を確認する。Import後は必ず次を実行する。

```bash
terraform plan
```

最終的に `No changes` になるまで、Terraform定義と実環境の差を確認する。

## 16. Terraform完了チェックリスト

- [ ] Bootstrap stateがS3にある
- [ ] DEV stateがS3にある
- [ ] S3 VersioningとLockが有効
- [ ] `terraform validate` 成功
- [ ] Apply後の `terraform plan` が差分なし
- [ ] EC2がSSM Online
- [ ] RDSがAvailable、Publicではない
- [ ] Cloudflare DNS・Accessが存在
- [ ] SES DKIMが3件
- [ ] Outputsを後続作業用に確認済み
