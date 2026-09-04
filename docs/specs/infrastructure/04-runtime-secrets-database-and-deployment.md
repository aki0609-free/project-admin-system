# 04. Runtime、Secrets、Database、Docker、デプロイ

## 1. この章の完了条件

- MongoDB Atlasの接続元がEC2 Elastic IPだけに制限される
- RDSに最小権限のApplication Userが存在する
- Database SecretとRuntime Secretが必要なキーを持つ
- Cloudflare TunnelがHealthyになる
- 初回DB Schemaと追加DB資産が適用される
- Backend / Frontend ImageがECRへPushされる
- EC2上の4コンテナがHealthyになる
- Cloudflare Access経由でアプリを表示できる
- SESテストメールを受信できる
- GitHub Actionsから手動DEV Deployできる

## 2. 全体の順序

順番を入れ替えると、DB Schema validation、Secret不足、Tunnel Inactive、ECR認証で失敗しやすい。

1. MongoDB Atlasを準備
2. RDS Application Userを作成
3. Database Secretを登録
4. Runtime Secretを登録
5. SES SMTP CredentialをRuntime Secretへ追加
6. Backend / Frontend ImageをECRへPush
7. Runtime BundleをEC2へ配置
8. 初回だけHibernate Bootstrap
9. 追加DB資産を適用
10. 通常Profile `aws` で再起動
11. Cloudflare Access経由で確認
12. GitHub Actions OIDCとEnvironmentを設定

## 3. MongoDB Atlasを準備する

### 3.1 ClusterとDatabase User

1. MongoDB AtlasでProjectを作る。
2. DEV用Clusterを作る。
3. Database AccessからProjectAdmin専用Userを作る。
4. 強固なランダムPasswordをPassword Managerへ保存する。
5. 必要なDatabaseだけへRead/Write権限を付ける。

管理者ユーザーや他システムのUserを流用しない。

### 3.2 EC2 Elastic IPを許可する

Terraform OutputからElastic IPを取得する。

```bash
cd /Users/tatsukiakiyama/Documents/ProjectAdmin/public-repository/infrastructure/environments/dev
terraform output -raw app_elastic_ip
```

Atlasの `Network Access` → `Add IP Address` で次を登録する。

```text
<APP_ELASTIC_IP>/32
```

`0.0.0.0/0` は使用しない。Elastic IPを付けている理由の1つは、Atlasの接続元を固定するためである。

### 3.3 Connection URIを作る

AtlasのDriver接続情報からURIを取得し、Database名をDEV用へ合わせる。

```text
mongodb+srv://<USER>:<URL_ENCODED_PASSWORD>@<CLUSTER_HOST>/project_admin_dev
```

Passwordに記号が含まれる場合はURL Encodeする。URIはSecrets Manager以外へ保存しない。

## 4. RDS Application Userを作る

### 4.1 なぜMaster Userをアプリから使わないのか

RDS Master UserはSchema作成・権限管理に使用する高権限Userである。アプリがMaster Passwordを持つと、アプリ侵害時の影響が大きすぎる。

通常稼働では `projectadmin_app` に次だけを許可する。

- SELECT
- INSERT
- UPDATE
- DELETE
- EXECUTE
- SHOW VIEW
- CREATE TEMPORARY TABLES

接続元HostはVPCのApp Subnet範囲に合わせて `10.20.0.%` とする。

### 4.2 RDS Master Secretを確認する

```bash
cd infrastructure/environments/dev

DB_IDENTIFIER="$(terraform output -raw mysql_instance_identifier)"

aws rds describe-db-instances \
  --db-instance-identifier "${DB_IDENTIFIER}" \
  --query 'DBInstances[0].MasterUserSecret.SecretArn' \
  --output text
```

Secret ValueはTerminalへ不用意に表示せず、AWS ConsoleのSecrets Managerから必要時だけ確認する。

### 4.3 SSM Sessionを開く

1. AWS ConsoleでSystems Managerを開く。
2. `Session Manager` → `Start session` を選ぶ。
3. ProjectAdminのEC2を選択する。
4. Sessionを開始する。

EC2へSSH Keyを配布したりPort 22を開けたりしない。

### 4.4 MySQL Client Containerから接続する

Session Manager上で実行する。

```bash
sudo docker run --rm -it mysql:8.4 \
  mysql \
  --host=<MYSQL_ADDRESS> \
  --port=3306 \
  --user=projectadmin \
  --password \
  --ssl-mode=REQUIRED \
  ADMIN
```

Promptが出てからRDS Master Passwordを貼る。コマンド引数へPasswordを書かない。

### 4.5 Userを作成する

Repositoryの `infrastructure/scripts/mysql/create_application_user.sql` と同じSQLを実行する。

```sql
CREATE USER 'projectadmin_app'@'10.20.0.%'
  IDENTIFIED BY RANDOM PASSWORD
  REQUIRE SSL;

GRANT
  SELECT,
  INSERT,
  UPDATE,
  DELETE,
  EXECUTE,
  SHOW VIEW,
  CREATE TEMPORARY TABLES
ON ADMIN.*
TO 'projectadmin_app'@'10.20.0.%';

SHOW GRANTS FOR 'projectadmin_app'@'10.20.0.%';
```

MySQLが返したランダムPasswordをその場でPassword Managerへ保存する。再表示できない。

## 5. Application DB Secretを登録する

TerraformはSecretの「器」だけを作る。値は手動で登録する。

### 5.1 Secret名

```bash
terraform output -raw mysql_application_secret_name
```

既定:

```text
project-admin/dev/database/application
```

### 5.2 JSON形式

Secrets Manager ConsoleでSecret Valueを次のJSONへ更新する。

```json
{
  "username": "projectadmin_app",
  "password": "<APPLICATION_DB_PASSWORD>",
  "host": "<MYSQL_ADDRESS>",
  "port": "3306",
  "dbname": "ADMIN"
}
```

キー名は大文字・小文字を含め完全一致が必要である。特に `username` を `usename` と誤記しない。

## 6. Runtime Secretを登録する

### 6.1 Secret名

```bash
terraform output -raw application_runtime_secret_name
```

既定:

```text
project-admin/dev/application/runtime
```

### 6.2 JWT Secretを生成する

Local Terminalで生成し、1行のままPassword Managerへ保存する。

```bash
openssl rand -base64 64 | tr -d '\n'
```

Terminal表示が折り返して2行に見えても、改行を除去した1つの値なら問題ない。

### 6.3 Cloudflare Tunnel Tokenを取得する

1. Cloudflare Zero Trustを開く。
2. Networks → Connectors / Tunnelsを開く。
3. Terraformが作った `project-admin-dev-ec2` を開く。
4. Connector追加またはInstall connectorを開く。
5. 表示されたCommandの `--token` に続くTokenだけを取得する。
6. Password Managerへ保存する。

これはTerraform用API Tokenとは別物である。

### 6.4 Runtime JSON

Secrets Manager Consoleで次のJSONを登録する。

```json
{
  "jwtSecret": "<ONE_LINE_RANDOM_JWT_SECRET>",
  "mongoUri": "<MONGODB_ATLAS_URI>",
  "cloudflareTunnelToken": "<CLOUDFLARE_TUNNEL_TOKEN>",
  "openAiApiKey": "",
  "keystorePassword": "<KEYSTORE_PASSWORD_IF_REQUIRED>"
}
```

OpenAIをV1で無効にする場合は空文字またはキー自体を省略してよい。Runtime Compose側でも `APP_AI_ENABLED=false` と各AI Model `none` を設定する。

Runtime起動時に `prepare_configtree.py` が2つのSecret JSONをSpring BootのConfig Treeへ変換する。

必須ファイル:

- `spring.datasource.url`
- `spring.datasource.username`
- `spring.datasource.password`
- `jwt.secret`
- `spring.data.mongodb.uri`
- `cloudflare.tunnel.token`

Secretを環境変数一覧やDocker Imageへ焼き込まず、EC2上のroot管理・Container読取専用Fileとして渡す。

## 7. Amazon SESを設定する

### 7.1 DomainとDKIMを確認する

1. AWS ConsoleでSESを開く。
2. Regionが `ap-northeast-1` であることを確認する。
3. Verified identitiesからDomainを開く。
4. Identity statusがVerifiedになるまで待つ。
5. DKIMがSuccessfulであることを確認する。

TerraformがSES Easy DKIM用CNAMEをCloudflareへ3件作成する。

### 7.2 SMTP Credentialを生成してSecretへ保存する

Project rootで実行する。

```bash
cd /Users/tatsukiakiyama/Documents/ProjectAdmin/public-repository

SES_USER="$(terraform -chdir=infrastructure/environments/dev output -raw ses_smtp_iam_user_name)"
RUNTIME_SECRET="$(terraform -chdir=infrastructure/environments/dev output -raw application_runtime_secret_name)"
DOMAIN="<CLOUDFLARE_ZONE_NAME>"

python3 infrastructure/scripts/ses/configure_smtp_credentials.py \
  --user-name "${SES_USER}" \
  --secret-name "${RUNTIME_SECRET}" \
  --from-address "no-reply@${DOMAIN}" \
  --region ap-northeast-1
```

このScriptは専用IAM UserのAccess Keyを1つだけ作成し、SES SMTP Passwordへ変換し、次のキーをRuntime Secretへ追記する。

- `mailHost`
- `mailPort`
- `mailUsername`
- `mailPassword`
- `mailFromAddress`

Access KeyやSMTP Passwordを画面へ出し続けず、Secrets Managerへ直接保存する。

### 7.3 SES Sandbox

Sandbox中は、送信元に加えて送信先も検証済みIdentityに制限される。検証済みテストアドレスへ送信確認した後、実利用前にProduction Accessを申請する。[AWS公式: SES IdentityとSandbox](https://docs.aws.amazon.com/ses/latest/dg/sending-authorization-identity-owner-tasks-verification.html)

## 8. Backend / Frontend ImageをECRへPushする

### 8.1 Syncfusion License

次のファイルを作る。

```text
frontend/.env.local
```

内容:

```dotenv
VITE_SYNCFUSION_LICENSE_KEY=<VALID_LICENSE_KEY>
```

`.env.local`はGitとDocker Build Contextから除外され、BuildKit SecretとしてのみFrontend buildへ渡される。

### 8.2 BackendをPushする

```bash
cd /Users/tatsukiakiyama/Documents/ProjectAdmin/public-repository
infrastructure/scripts/deployment/push_backend_image.sh
```

終了時の `BACKEND_IMAGE=` を安全な作業メモへ控える。

### 8.3 FrontendをPushする

```bash
infrastructure/scripts/deployment/push_frontend_image.sh
```

終了時の `FRONTEND_IMAGE=` を控える。

両Scriptは `linux/amd64` Imageを作成する。EC2がx86_64のためPlatformを変えない。ECR TagはImmutableなので、同じTagを上書きしない。

## 9. Runtime BundleをEC2へ配置する

```bash
export BACKEND_IMAGE='<BACKEND_IMAGE_FULL_URI>'
export FRONTEND_IMAGE='<FRONTEND_IMAGE_FULL_URI>'

infrastructure/scripts/deployment/deploy_backend_runtime.sh
```

処理内容:

1. ECR Imageの存在確認
2. EC2 Running / SSM Online確認
3. Compose、Deployment Settings、Secrets変換Scriptを一時Bundle化
4. 書類S3の `_deployment/runtime` へ一時Upload
5. SSM Run CommandでEC2へDownload
6. `/opt/project-admin` へroot権限でInstall
7. Secrets ManagerからConfig Tree生成
8. 一時ECR LoginでImage Pull
9. Docker Compose起動とHealth Check
10. 一時S3 Objectを削除

初回の空DBでは通常Profile `aws` が `ddl-auto=validate` のため、Schema不足でBackendがHealthyにならない。この時点ではBundleとImageが正しく配置されていれば次のBootstrapへ進む。

## 10. 初回だけDatabase SchemaをBootstrapする

### 10.1 事前Snapshot

空DBでも初回処理の直前状態を保存する。

```bash
infrastructure/scripts/operations/create_pre_bootstrap_snapshot.sh
```

### 10.2 一時的にSchema作成権限を付ける

RDS Master Userで接続し、Repositoryの `grant_schema_bootstrap.sql` と同じSQLを実行する。

```sql
GRANT
  CREATE,
  ALTER,
  INDEX,
  REFERENCES
ON ADMIN.*
TO 'projectadmin_app'@'10.20.0.%';

SHOW GRANTS FOR 'projectadmin_app'@'10.20.0.%';
```

### 10.3 `aws-bootstrap` で起動する

Session ManagerからEC2へ入り、root shellで実行する。

```bash
sudo -i
cd /opt/project-admin/runtime

cp -i deployment.env deployment.env.before-bootstrap
sed -i 's/^SPRING_PROFILES_ACTIVE=.*/SPRING_PROFILES_ACTIVE=aws-bootstrap/' deployment.env

docker compose --env-file deployment.env up -d --remove-orphans --wait --wait-timeout 360
docker compose --env-file deployment.env ps
curl --fail --silent --show-error http://127.0.0.1:8080/actuator/health
```

`application-aws-bootstrap.yml` はHibernate `ddl-auto=update` とSpring Batch Schema初期化を有効にする。これは最初のSchema生成専用で、通常運用には使わない。

### 10.4 追加Runtime Schemaを適用する

Local Terminalへ戻って実行する。

```bash
infrastructure/scripts/database/apply_runtime_schema_upgrade.sh
```

このScriptは `backend/src/main/resources/sql/runtime-schema-manifest.txt` に列挙されたSQLを順番に適用し、検証する。RDS Master Secretへの一時読取権限は処理後に削除される。

### 10.5 Schema作成権限を戻す

RDS Master Userで接続し、Repositoryの `revoke_schema_bootstrap.sql` と同じSQLを実行する。

```sql
REVOKE
  CREATE,
  ALTER,
  INDEX,
  REFERENCES
ON ADMIN.*
FROM 'projectadmin_app'@'10.20.0.%';

SHOW GRANTS FOR 'projectadmin_app'@'10.20.0.%';
```

通常権限だけに戻ったことを確認する。

### 10.6 通常Profileへ戻す

Session Managerで実行する。

```bash
sudo -i
cd /opt/project-admin/runtime

sed -i 's/^SPRING_PROFILES_ACTIVE=.*/SPRING_PROFILES_ACTIVE=aws/' deployment.env
docker compose --env-file deployment.env up -d --remove-orphans --wait --wait-timeout 360

docker compose --env-file deployment.env ps
docker inspect project-admin-dev-backend-1 \
  --format 'STATUS={{.State.Status}} HEALTH={{if .State.Health}}{{.State.Health.Status}}{{end}} RESTARTS={{.RestartCount}}'
docker exec project-admin-dev-backend-1 printenv SPRING_PROFILES_ACTIVE
curl --fail --silent --show-error http://127.0.0.1:8080/actuator/health
```

期待値:

```text
HEALTH=healthy
SPRING_PROFILES_ACTIVE=aws
{"status":"UP"}
```

## 11. Cloudflare経由で確認する

1. CloudflareのTunnel StatusがHealthyになることを確認する。
2. Application URLをPrivate Browserで開く。
3. Cloudflare AccessのOne-time PIN画面が出ることを確認する。
4. 許可メールアドレスへPINが届くことを確認する。
5. 未許可メールでは通過できないことを確認する。
6. アプリログイン画面が表示されることを確認する。
7. アプリへログインし、Dashboardと代表的なAPIを確認する。

CloudflareのOriginは `http://frontend:8080` のままにする。Backendへ直接向けるとVue画面、静的資産、Frontend nginxのAPI Proxyを通らない。

## 12. GitHub Actions OIDCを設定する

### 12.1 なぜOIDCを使うのか

GitHubへAWS Access Keyを長期Secretとして保存せず、Workflow実行時だけ期限付きAWS権限を受け取るためである。[GitHub公式: AWS OIDC](https://docs.github.com/en/actions/how-tos/secure-your-work/security-harden-deployments/oidc-in-aws)

### 12.2 Immutable Subjectを有効にする

TerraformのIAM Trust PolicyはRepository Owner IDとRepository IDを含むImmutable Subjectを期待する。Terraform ApplyでIAM Roleを先に作成した後、Repositoryで有効にする。

```bash
gh api \
  --method PUT \
  -H "X-GitHub-Api-Version: 2026-03-10" \
  repos/<OWNER>/<REPOSITORY>/actions/oidc/customization/sub \
  -F use_default=true \
  -F use_immutable_subject=true
```

設定を確認する。

```bash
gh api repos/<OWNER>/<REPOSITORY>/actions/oidc/customization/sub
```

Immutable SubjectはRepositoryをRenameしても数値IDで信頼対象を固定する。GitHubの仕様は変更される可能性があるため、エラー時は[GitHub公式: OIDC REST API](https://docs.github.com/en/rest/actions/oidc)を確認する。

### 12.3 GitHub Environment `dev`

Repository Settings → Environments → New environmentで `dev` を作る。

可能ならDeployment branch/tag ruleとRequired reviewerを設定する。

### 12.4 Environment Variables

| Name | Value |
|---|---|
| `AWS_DEPLOY_ROLE_ARN` | `terraform output -raw github_actions_deploy_role_arn` |
| `AWS_REGION` | `ap-northeast-1` |
| `AWS_ACCOUNT_ID` | 12桁Account ID |
| `APP_INSTANCE_ID` | `terraform output -raw app_instance_id` |
| `DOCUMENT_BUCKET_NAME` | `terraform output -raw document_bucket_name` |
| `BACKEND_REPOSITORY` | `terraform output -raw backend_ecr_repository_name` |
| `FRONTEND_REPOSITORY` | `terraform output -raw frontend_ecr_repository_name` |
| `APPLICATION_URL` | `https://<cloudflare_application_hostname>` |

### 12.5 Environment Secret

| Name | Value |
|---|---|
| `SYNCFUSION_LICENSE` | 有効なSyncfusion License Key |

AWS Key、Cloudflare Token、DB PasswordはGitHubへ置かない。

### 12.6 Deploy Workflowを実行する

1. GitHub Actionsを開く。
2. `Deploy DEV` を選ぶ。
3. `Run workflow` を選ぶ。
4. 確認欄へ正確に `DEPLOY DEV` と入力する。
5. Verify JobとDeploy Jobが成功することを確認する。
6. Cloudflare Accessの302 Redirect検証が成功することを確認する。

Workflowは自動Push Deployではなく手動実行である。通常のCommit/PushだけではDEV Deployは開始しない。

## 13. ローカルDocker環境を起動する

### 13.1 構成

`docker-compose.yml` は次を起動する。

- MySQL 8.4
- MongoDB 7
- Redis 7.4
- Mailpit
- Spring Boot Backend
- Runtime Schema適用用One-shot Container
- Vue/nginx Frontend

### 13.2 起動

```bash
cd /Users/tatsukiakiyama/Documents/ProjectAdmin/public-repository
docker compose up --build --wait
docker compose ps
```

### 13.3 URL

| 用途 | URL |
|---|---|
| Application | `http://localhost:5173` |
| Backend Health | `http://localhost:8080/actuator/health` |
| Mailpit | `http://localhost:8025` |
| MySQL | `localhost:3306` |
| MongoDB | `localhost:27017` |
| Redis | `localhost:6379` |

### 13.4 停止

Containerだけ停止しDataを残す。

```bash
docker compose down
```

Dataも完全に消す場合だけ実行する。

```bash
docker compose down --volumes
```

`--volumes` はLocal MySQL、MongoDB、Redis、Local Storageを削除する。必要なテストデータがある場合は実行しない。

## 14. 初回構築完了チェック

- [ ] Database Secretのキーが5件正しい
- [ ] Runtime SecretにJWT、Mongo URI、Tunnel Tokenがある
- [ ] SES SMTPキーがRuntime Secretへ追記済み
- [ ] MongoDB Atlasの許可IPがEIP `/32`
- [ ] Application DB UserがSSL必須・最小権限
- [ ] `aws-bootstrap` を通常起動へ残していない
- [ ] Runtime Schema適用成功
- [ ] 4 ContainersがHealthy
- [ ] Backend HealthがUP
- [ ] TunnelがHealthy
- [ ] Cloudflare Accessが未許可Userを拒否
- [ ] SES Test Mail受信
- [ ] GitHub Actions Deploy成功
- [ ] Local Docker起動成功
