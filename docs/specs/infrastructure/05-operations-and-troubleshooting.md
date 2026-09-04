# 05. 運用・更新・停止・障害対応

## 1. 日常運用の原則

- AWS操作前にAccount、Profile、Regionを確認する
- Terraform変更はfmt → validate → plan → review → applyの順にする
- Application更新はCI/CDを優先する
- EC2へ直接手修正した内容を恒久対応にしない
- SecretをLogやCommand引数へ出さない
- DB変更前にBackup/SnapshotとRollback方法を確認する
- DEVを使わない日はEC2とRDSを停止する
- エラー時は画面の再試行より先にHealthとLogを確認する

## 2. DEVリソースを起動する

### 2.1 AWS認証

```bash
aws login --profile project-admin-terraform
AWS_PROFILE=project-admin-terraform aws sts get-caller-identity
```

### 2.2 起動Script

```bash
cd /Users/tatsukiakiyama/Documents/ProjectAdmin/public-repository

AWS_PROFILE=project-admin-terraform \
infrastructure/scripts/operations/start_dev_resources.sh
```

ScriptはRDSを先に起動し、Availableを待ってからEC2を起動する。RDS起動は数分以上かかることがある。

期待値:

```text
Starting RDS...
Starting EC2...
DEV_RESOURCES_STARTED
```

### 2.3 起動後確認

```bash
INSTANCE_ID="$(terraform -chdir=infrastructure/environments/dev output -raw app_instance_id)"

aws ssm describe-instance-information \
  --filters "Key=InstanceIds,Values=${INSTANCE_ID}" \
  --query 'InstanceInformationList[0].PingStatus' \
  --output text
```

`Online` になった後、Application URLを開く。Dockerは `restart: unless-stopped` なのでEC2起動後に自動復帰するが、RDS接続待ちでBackendの起動完了に時間がかかることがある。

## 3. DEVリソースを停止する

### 3.1 停止Script

```bash
cd /Users/tatsukiakiyama/Documents/ProjectAdmin/public-repository

AWS_PROFILE=project-admin-terraform \
infrastructure/scripts/operations/stop_dev_resources.sh
```

期待値:

```text
Stopping EC2...
Stopping RDS...
Waiting for RDS to stop...
DEV_RESOURCES_STOPPED
```

EC2を先に止めることで、停止途中のRDSへアプリが接続を続ける状態を避ける。

### 3.2 停止確認

```bash
INSTANCE_ID="$(terraform -chdir=infrastructure/environments/dev output -raw app_instance_id)"
DB_ID="$(terraform -chdir=infrastructure/environments/dev output -raw mysql_instance_identifier)"

aws ec2 describe-instances \
  --instance-ids "${INSTANCE_ID}" \
  --query 'Reservations[0].Instances[0].State.Name' \
  --output text

aws rds describe-db-instances \
  --db-instance-identifier "${DB_ID}" \
  --query 'DBInstances[0].DBInstanceStatus' \
  --output text
```

両方 `stopped` であることを確認する。

### 3.3 停止時の注意

- RDSは7日間停止するとAWSが自動起動する
- RDS Storage、Snapshot、EBS、S3、ECR、Secrets、Logsなどの費用は停止中も残る
- 数時間後に再開するなら、起動時間との兼ね合いで停止しない判断もできる
- Production相当の環境で可用性が必要なら、日常停止運用をそのまま採用しない

[AWS公式: RDS一時停止の制限](https://docs.aws.amazon.com/AmazonRDS/latest/UserGuide/USER_StopInstance.html)

## 4. ContainerとHealthを確認する

Session ManagerでEC2へ接続し、次を実行する。

```bash
sudo -i
cd /opt/project-admin/runtime

docker compose --env-file deployment.env ps
curl --fail --silent --show-error http://127.0.0.1:8080/actuator/health
```

Backend詳細:

```bash
docker inspect project-admin-dev-backend-1 \
  --format 'STATUS={{.State.Status}} HEALTH={{if .State.Health}}{{.State.Health.Status}}{{end}} RESTARTS={{.RestartCount}}'

docker exec project-admin-dev-backend-1 printenv SPRING_PROFILES_ACTIVE
```

期待値:

```text
STATUS=running HEALTH=healthy RESTARTS=0
aws
```

## 5. Logを確認する

### 5.1 Docker Log

```bash
cd /opt/project-admin/runtime

docker compose --env-file deployment.env logs --no-color --tail=200 backend
docker compose --env-file deployment.env logs --no-color --tail=200 frontend
docker compose --env-file deployment.env logs --no-color --tail=200 cloudflared
docker compose --env-file deployment.env logs --no-color --tail=200 redis
```

Password、JWT、MongoDB URI、Mail Credentialを貼り付ける前にLogを確認し、秘密値部分をマスクする。

### 5.2 CloudWatch Logs

Log Group:

```text
/project-admin/dev/runtime
```

Log Streams:

- `backend`
- `frontend`
- `redis`
- `cloudflared`

Backend ERROR検索:

```text
fields @timestamp, traceId, logger_name, message
| filter level = "ERROR"
| sort @timestamp desc
| limit 100
```

Trace ID検索:

```text
fields @timestamp, level, logger_name, message, httpMethod, httpPath, httpStatus
| filter traceId = "対象Trace ID"
| sort @timestamp asc
```

## 6. Applicationを更新する

### 6.1 推奨: GitHub Actions

1. 対象CommitでCIが成功していることを確認する。
2. EC2とRDSが起動済みであることを確認する。
3. GitHub Actions → `Deploy DEV` を開く。
4. `DEPLOY DEV` を入力して手動実行する。
5. Verify、Image Build/Push、SSM Deployment、Cloudflare境界確認の全Jobを確認する。
6. Applicationへログインし、変更箇所と代表機能をSmoke Testする。

### 6.2 緊急時または初回: 手動Push/Deploy

```bash
infrastructure/scripts/deployment/push_backend_image.sh
infrastructure/scripts/deployment/push_frontend_image.sh
```

出力された2つのImage URIを使う。

```bash
export BACKEND_IMAGE='<BACKEND_IMAGE_URI>'
export FRONTEND_IMAGE='<FRONTEND_IMAGE_URI>'

infrastructure/scripts/deployment/deploy_backend_runtime.sh
```

### 6.3 Rollback

ECRの以前に動作していたImmutable Tagを指定し、同じDeploy Scriptを実行する。

RollbackはImageだけを戻す。DB Schemaに後方互換性がない場合は、Snapshot復元を含む別の復旧手順が必要である。DB MigrationはImage Deployより先にRollback可能性を確認する。

## 7. Terraformを更新する

```bash
cd infrastructure/environments/dev

export AWS_PROFILE=project-admin-terraform
export AWS_REGION=ap-northeast-1
read -r -s "CLOUDFLARE_API_TOKEN?Cloudflare API Token: "
echo
export CLOUDFLARE_API_TOKEN
export TF_VAR_cloudflare_allowed_emails='["authorized-user@example.com"]'

terraform fmt -check -recursive ../../
terraform init
terraform validate
terraform plan -out=tfplan-change
terraform apply "tfplan-change"
terraform plan
```

最後のplanが `No changes` であることを確認する。

### 7.1 Applyしてはいけない例

- 理由不明のEC2 Replacement
- RDS Destroy/Replacement
- S3 Bucket Destroy
- Cloudflare Access Policyの全削除
- 許可メールアドレスが空になる変更
- GitHub Deploy Roleの信頼先が別Repositoryになる変更
- Secret自体の削除

EC2とRDSには`prevent_destroy`や削除保護があるが、それを最後の安全装置として頼らずPlanを読む。

## 8. Database Schemaを更新する

通常AWS ProfileはHibernate `ddl-auto=validate` である。Entityを変更しただけではAWS DBにTable/Columnは追加されない。

1. SQLを適切なDomain配下へ追加する。
2. 必要なら `runtime-schema-manifest.txt` へ追加する。
3. Local Dockerで新規DB・既存DBへの適用を確認する。
4. AWS適用前にRDS Snapshotを作る。
5. `apply_runtime_schema_upgrade.sh` を実行する。
6. 検証SQLが成功することを確認する。
7. Application ImageをDeployする。
8. `ddl-auto=validate` 起動と代表業務を確認する。

業務のView、Stored Procedure、Master DataはHibernateだけでは管理されない。最終的な自動配置Script/Lambdaの整備までは、ManifestとKBを正本として適用漏れを防ぐ。

## 9. MySQL WorkbenchからRDSへ接続する

RDSをPublicにせず、Session Manager Port Forwardingを使う。

### 9.1 Session Manager Plugin

Local PCへAWS Session Manager Pluginを導入する。AWS公式手順に従い、`session-manager-plugin` を実行できることを確認する。

### 9.2 Port Forwarding

```bash
INSTANCE_ID="$(terraform -chdir=infrastructure/environments/dev output -raw app_instance_id)"
DB_HOST="$(terraform -chdir=infrastructure/environments/dev output -raw mysql_address)"

AWS_PROFILE=project-admin-terraform \
aws ssm start-session \
  --target "${INSTANCE_ID}" \
  --document-name AWS-StartPortForwardingSessionToRemoteHost \
  --parameters "host=${DB_HOST},portNumber=3306,localPortNumber=13306"
```

このTerminalは接続中そのままにする。

MySQL Workbench:

| 項目 | 値 |
|---|---|
| Hostname | `127.0.0.1` |
| Port | `13306` |
| Username | `projectadmin_app` |
| Password | Application DB Password |
| Default Schema | `ADMIN` |
| SSL | REQUIRED |

Master Userを日常の参照・編集に使わない。

## 10. MongoDB Compassから接続する

MongoDB AtlasへLocal PCから直接接続する場合、Atlas Network Accessへ現在のGlobal IPを一時的に `/32` で追加する。

1. Atlasで現在IPだけを追加する。
2. CompassへAtlas URIを入力する。
3. 作業完了後、Local IPの許可を削除する。

`0.0.0.0/0` を恒久登録しない。AWS Runtime用のElastic IP許可は削除しない。

## 11. Backupの区別

| Backup | 対象 | 目的 | 保持 |
|---|---|---|---|
| RDS Automated Backup | MySQL全体 | 障害・誤操作からDB復旧 | 現在7日 |
| RDS Manual Snapshot | MySQL全体 | 大きな変更前の復旧点 | 手動管理 |
| S3 Versioning | 同じObjectの過去Version | 上書き・削除の保護 | Lifecycleに従う |
| 帳票Backup | 確定帳票 | 法令・業務保管 | 仕様上7年 |
| Download ZIP Backup | 顧客・取引・従業員・日報 | 業務データ持出し・確認 | 運用ルールに従う |

RDS Backupと帳票・業務書類Backupは代替関係ではない。DBを復元しても、法定保管する確定帳票の独立Backupにはならない。

## 12. よくあるエラー

### 12.1 `Your session has expired`

原因: AWS CLI一時認証の期限切れ。

```bash
aws logout --profile project-admin-terraform
aws login --profile project-admin-terraform
AWS_PROFILE=project-admin-terraform aws sts get-caller-identity
```

### 12.2 AWS認証画面が`400 Bad Request`

- 古い認証Tabや古いAuthorization URLを使わない
- Terminalで新しい `aws login` を開始する
- 同じBrowser ProfileのCookie問題ならPrivate Windowを試す
- Remote Codeや長いURLをチャットへ貼らない
- 認証対象がRootかIAM Userかを確認する

### 12.3 `S3 bucket ... does not exist`

原因: DEV BackendをState Bucket作成前に初期化した、またはDocument Bucket名をState Bucketへ誤用した。

対処:

1. BootstrapでState Bucketを先に作る。
2. BackendのBucket、Key、Regionを確認する。
3. 作成直後は少し待って再度initする。
4. `terraform init -reconfigure` はBackend値が正しいことを確認してから使う。

### 12.4 `Secret ... is missing required keys: username`

Database Secret JSONのキー名を確認する。

必須:

```text
username, password, host, port, dbname
```

`usename` は無効。

### 12.5 `no basic auth credentials`

原因: EC2でECR Login前にPrivate ImageをPullしようとした。

対処: 手動で `docker compose up` を繰り返さず、Deploy Scriptを再実行する。Scriptは一時Docker ConfigでECR Login → Pull → Logoutを行う。

### 12.6 RedisがUnhealthy

```bash
docker compose --env-file deployment.env logs --tail=200 redis
docker inspect project-admin-dev-redis-1 --format '{{json .State.Health}}'
```

Memory不足、Permission、古いContainer設定を確認する。RedisはCache用途で永続化しないため、必要ならContainerを再作成できる。

### 12.7 BackendがUnhealthy

確認順:

1. RDSがAvailable
2. RedisがHealthy
3. Database Secretの5キー
4. Runtime SecretのJWT、Mongo URI、Tunnel Token
5. MongoDB AtlasのEIP許可
6. `SPRING_PROFILES_ACTIVE=aws`
7. DB SchemaがEntityと一致
8. Backend Logの最初の例外

```bash
docker compose --env-file deployment.env logs --no-color --tail=500 backend
curl -v http://127.0.0.1:8080/actuator/health
```

`health: starting` は起動途中であり、Start Periodは180秒である。即座に失敗と判断しない。

### 12.8 TunnelがInactive

確認:

- `cloudflared` ContainerがRunning
- Runtime Secretに正しいTunnel Token
- Config Treeの `cloudflare.tunnel.token` が空でない
- Tunnel IDとTokenが同じTunnelのもの
- EC2が外向きHTTPS通信可能
- Cloudflare Originが `http://frontend:8080`
- FrontendがHealthy

### 12.9 SES送信が失敗

- RegionがSES Identityと同じ `ap-northeast-1`
- Domain / DKIMがVerified
- Runtime Secretのmailキーが5件ある
- From Addressが検証Domain配下
- Sandbox中は送信先もVerified
- LocalはMailpitでありSESへ外部送信しない

### 12.10 Syncfusion Trial Banner

- `frontend/.env.local` のキー名が `VITE_SYNCFUSION_LICENSE_KEY`
- GitHub Environment Secret名が `SYNCFUSION_LICENSE`
- Licenseを設定した後にFrontend ImageをBuildし直したか
- 古いFrontend Image TagをDeployしていないか
- LicenseをGit、Image Layer、Logへ表示していないか

### 12.11 OpenAI `Selected model is at capacity`

V1でAIを無効化している場合、Runtimeは `APP_AI_ENABLED=false`、各Model `none` で起動する。画面の非AI機能とBackend Healthが正常ならインフラ障害ではない。AIを有効化する変更とは分けて扱う。

## 13. 月次の運用確認

- [ ] Cost Explorerでサービス別費用を確認
- [ ] Budget Alertが有効
- [ ] 不要なECR ImageがLifecycleで整理されている
- [ ] S3の異常な増加がない
- [ ] CloudWatch Log Groupが14日保持
- [ ] CloudWatch Alarm状態を確認
- [ ] RDS Storage残量を確認
- [ ] RDS Automated Backupを確認
- [ ] 不要なManual Snapshotを運用ルールに従って整理
- [ ] root Access Keyが0件
- [ ] IAM / GitHub / Cloudflareの退職者権限を削除
- [ ] MongoDB Atlas Network Accessに不要IPがない
- [ ] SES Sending状況とBounce/Complaintを確認
- [ ] Terraform planが意図しない差分なし

## 14. 構成変更時の完了報告テンプレート

```text
作業名:
作業日:
作業者:
対象環境: DEV
変更理由:

Terraform:
- fmt:
- validate:
- plan結果:
- apply結果:
- apply後plan:

Application:
- Backend Image:
- Frontend Image:
- Container Health:
- Backend Health:
- Cloudflare Access:

Database:
- Snapshot:
- 適用SQL/Manifest:
- 検証結果:

Monitoring:
- CloudWatch Error:
- Alarm状態:

Rollback方法:
残課題:
```
