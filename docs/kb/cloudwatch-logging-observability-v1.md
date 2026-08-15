# CloudWatch・ログ監視基盤 V1

## 1. 目的

ProjectAdminSystem DEV環境のアプリケーションログとAWS基盤メトリクスをCloudWatchへ集約し、障害調査に必要な情報を一か所で確認できるようにする。

V1では、小規模な社内利用と費用を考慮し、CloudWatch Agent用の追加サーバーは構築しない。

## 2. 構成

```text
Spring Boot JSON標準出力 ─┐
Frontend nginxログ       ─┤
Redisログ                ─┼─ Docker awslogs driver
cloudflaredログ          ─┘          │
                                     ▼
                    /project-admin/dev/runtime
                                     │
                    ┌────────────────┼───────────────┐
                    ▼                ▼               ▼
              Logs Insights     Metric Filter    Dashboard
                                      │
                                      ▼
                                ERROR Alarm
```

## 3. Terraform管理資産

モジュール：

`infrastructure/modules/cloudwatch_observability`

作成する資産：

- CloudWatch Logsロググループ
- EC2用CloudWatch Logs書込IAMポリシー
- Backend ERRORメトリクスフィルター
- Backend ERRORアラーム
- EC2 StatusCheckFailedアラーム
- RDS空き容量アラーム
- CloudWatch運用ダッシュボード

DEV環境から次の名前で作成する。

| 資産 | 名前 |
|---|---|
| ロググループ | `/project-admin/dev/runtime` |
| ダッシュボード | `project-admin-dev-operations` |
| ERRORメトリクス | `ProjectAdmin/Dev / BackendErrorCount` |

## 4. ログストリーム

| ストリーム | 内容 |
|---|---|
| `backend` | Spring Boot JSONログ |
| `frontend` | nginxアクセス・エラーログ |
| `redis` | Redisログ |
| `cloudflared` | Cloudflare Tunnelログ |

ログ保持期間は14日とする。

業務上の確定履歴、監査履歴、帳票履歴の保管期間とは別であり、CloudWatch Logsを業務データの保存先として使用しない。

## 5. Spring Bootログ仕様

AWSプロファイルでは、Logstash JSON形式を標準出力へ一度だけ出力する。

主なフィールド：

- `@timestamp`
- `level`
- `logger_name`
- `message`
- `app`
- `environment`
- `traceId`
- `httpMethod`
- `httpPath`
- `httpStatus`
- `durationMs`

HTTPレスポンスには `X-Trace-Id` ヘッダーを付ける。

画面でエラーが発生した場合、この値を使ってCloudWatch Logsから同一処理のログを検索できる。

URLのクエリ文字列、リクエスト本文、パスワード、JWT、Secret値はアクセスログへ出力しない。

ヘルスチェック `/actuator/health` は30秒ごとに実行されるため、アクセス完了ログの対象外とする。

## 6. ローカル環境

ローカルプロファイルでは、従来どおり次のログを使用する。

- 人間向けコンソールログ
- ローテーションファイルログ
- ERROR専用ファイル
- JSONファイル

ローカルDockerからCloudWatchへの送信は行わない。

## 7. アラーム

### 7.1 Backend ERROR

5分間に1件以上のJSON `ERROR` ログがある場合にALARMとする。

### 7.2 EC2 Status Check

EC2の `StatusCheckFailed` が2回連続して1以上の場合にALARMとする。

### 7.3 RDS空き容量

RDSの空き容量が2 GiB未満の状態を2回連続で検出した場合にALARMとする。

V1初期導入では通知先を設定せず、CloudWatch上で状態を確認する。通知が必要になった時点でSNSトピックと通知先メールアドレスをTerraformへ追加する。

## 8. Logs Insights検索例

### ERRORログ

```text
fields @timestamp, traceId, logger_name, message
| filter level = "ERROR"
| sort @timestamp desc
| limit 100
```

### Trace ID検索

```text
fields @timestamp, level, logger_name, message, httpMethod, httpPath, httpStatus
| filter traceId = "画面またはAPI応答に表示されたTrace ID"
| sort @timestamp asc
```

### 遅いHTTP処理

```text
fields @timestamp, traceId, httpMethod, httpPath, httpStatus, durationMs
| filter ispresent(durationMs)
| sort durationMs desc
| limit 100
```

## 9. 適用手順

```bash
cd /Users/tatsukiakiyama/Documents/ProjectAdmin/public-repository/infrastructure/environments/dev

AWS_PROFILE=project-admin-terraform terraform init
AWS_PROFILE=project-admin-terraform terraform plan -out=tfplan-cloudwatch
AWS_PROFILE=project-admin-terraform terraform apply "tfplan-cloudwatch"
```

Terraform適用後、runtime bundleを再デプロイする。

Dockerコンテナはロググループが存在し、EC2 IAM権限が反映されてから再作成する必要がある。

## 10. 動作確認

1. CloudWatchコンソールを開く。
2. `ロググループ` を開く。
3. `/project-admin/dev/runtime` を選択する。
4. `backend`、`frontend`、`redis`、`cloudflared` が存在することを確認する。
5. アプリへログインし、任意の一覧画面を開く。
6. `backend` にJSONログが追加されることを確認する。
7. CloudWatchダッシュボード `project-admin-dev-operations` を開く。

## 11. 注意事項

- Terraformより先にawslogsドライバーへ切り替えると、ロググループまたは権限不足でコンテナを起動できない
- CloudWatchへSecret、パスワード、JWT、個人情報を意図的に出力しない
- INFOログを過剰に追加しない
- SQLパラメータや帳票データ全体をログへ出力しない
- 障害調査後も、必要性がなければ保持期間を延長しない
- SSMの最新AMI更新を理由に既存EC2が自動置換されないよう、EC2には `ignore_changes = [ami]` と `prevent_destroy = true` を設定する
- AMI更新は、バックアップ・移行・復旧確認を含む独立した作業として実施する
