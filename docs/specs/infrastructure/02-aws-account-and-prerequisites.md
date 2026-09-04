# 02. AWSアカウント作成と事前準備

## 1. この章の完了条件

- AWSアカウントへログインできる
- rootユーザーのMFAが有効
- rootアクセスキーが0件
- 日常作業用の管理者でログインできる
- Free/Paid plan、クレジット残高、利用額を確認できる
- AWS Budgetsの通知を受け取れる
- MacへAWS CLI、Terraform、Docker、GitHub CLI、補助ツールが入っている
- `project-admin-terraform` Profileで一時認証できる

## 2. AWSアカウントを作成する

### 2.1 用意するもの

- 長期利用できるメールアドレス
- 強固で他サービスと使い回さないパスワード
- SMSまたは音声通話を受けられる電話番号
- 有効な支払方法
- MFA用の認証アプリ、PasskeyまたはSecurity Key

root用メールは個人退職などで失われないものが望ましい。回復用メールと電話番号も、第三者に簡単に奪われないよう保護する。

### 2.2 作成手順

1. [AWS Free Tier](https://aws.amazon.com/free/)を開く。
2. `Create free account` または `AWSアカウントを作成` を選ぶ。
3. root用メールアドレスとアカウント名を登録する。
4. メール認証を完了する。
5. 強固なrootパスワードを設定する。
6. 連絡先情報を入力する。
7. 支払方法を登録する。
8. 電話番号による本人確認を完了する。
9. Support planは、特別な契約がなければBasic Supportから開始する。
10. Free account planまたはPaid account planを選択する。

新規アカウントではFreeとPaidの2種類がある。Free planは最大6か月またはクレジット消化までで、一部サービスに制限があり、終了するとアカウントが閉じる。Paid planは全サービスを利用でき、クレジット超過分が従量課金される。[AWS公式: Free Tier](https://docs.aws.amazon.com/awsaccountbilling/latest/aboutv2/free-tier.html)

ProjectAdminSystemを継続運用し、RDS・SES・周辺機能の制限を避ける場合は、**予算通知を設定したうえでPaid account planを推奨**する。学習だけで資産を失ってもよい場合はFree planから開始できる。

## 3. rootユーザーを保護する

rootユーザーは全リソースと請求情報へ無制限にアクセスできる。日常のTerraformやコンソール操作には使わない。

AWSもroot MFAとrootアクセスキーを作成しないことを強く推奨している。[AWS公式: rootユーザーのベストプラクティス](https://docs.aws.amazon.com/IAM/latest/UserGuide/root-user-best-practices.html)

### 3.1 root MFAを確認・登録する

1. AWSログイン画面で `Root user` を選ぶ。
2. rootメールアドレスとパスワードでログインする。
3. 右上のアカウントメニューから `Security credentials` を開く。
4. `Multi-factor authentication (MFA)` を探す。
5. MFA Deviceが1件以上 `Assigned` であることを確認する。
6. 未登録なら `Assign MFA device` を選び、Passkey/Security KeyまたはAuthenticator Appを登録する。
7. 登録後、一度ログアウトし、MFAを使って再ログインできることを確認する。

可能なら異なる方式のMFAを2つ登録し、両方を同じ場所に保管しない。

### 3.2 rootアクセスキーを確認する

1. rootの `Security credentials` を開く。
2. `Access keys` セクションを開く。
3. `You do not have any access keys` または0件であることを確認する。
4. 不明なAccess Keyがあれば、使用箇所を確認して一時認証へ移行し、無効化後に削除する。

rootアクセスキーは作らない。AWS CLIは後述の `aws login` による期限付き認証を使う。

## 4. 日常作業用の管理者を作る

### 4.1 方針

AWSの現在の推奨はIAM Identity CenterなどのFederationと一時認証である。[AWS公式: IAM Security Best Practices](https://docs.aws.amazon.com/IAM/latest/UserGuide/best-practices.html)

既存ProjectAdmin環境と同じ小規模構成を再現する場合、まず管理用IAMユーザーを1名作成し、MFAを必須にする。
既存環境の管理ユーザー名は `tatsuki-admin` であるが、新規環境では担当者が分かる名前へ置き換えてよい。

### 4.2 IAM管理ユーザー作成

1. rootでAWS Consoleへログインする。
2. IAMを開く。
3. `Users` → `Create user` を選ぶ。
4. 管理用ユーザー名を入力する。
5. AWS Management Consoleへのアクセスを有効にする。
6. 初回パスワードを安全に受け渡す。
7. 初回構築ではAWS管理Policy `AdministratorAccess` を直接または管理者Group経由で付与する。
8. ユーザー作成後、MFAを割り当てる。
9. そのユーザーでログインできることを確認する。
10. rootからログアウトし、以後の日常作業は管理ユーザーで行う。

初回構築で強い権限を使う場合も、利用者へ同じ権限を配らない。構築後はTerraform管理権限・運用権限・閲覧権限を分けることが将来課題である。

## 5. Plan、クレジット、利用額を確認する

1. AWS Console Homeの `Cost and Usage` ウィジェットを確認する。
2. 表示されない場合は `Billing and Cost Management` を開く。
3. Free planの場合は、Planの種類、残日数、終了日、クレジット残高を確認する。
4. Paid planの場合は、`Credits` で残高と有効期限を確認する。
5. `Bills` で当月のサービス別利用額を確認する。
6. `Cost Explorer` を有効化し、日別・サービス別の増加を確認できるようにする。

Planとクレジット情報はCost and UsageウィジェットまたはBilling Consoleで確認できる。[AWS公式: Planの選択と確認](https://docs.aws.amazon.com/awsaccountbilling/latest/aboutv2/free-tier-plans.html)

BillingとCost Explorerの表示は即時ではなく、通常は時間差がある。作成直後に0円でも無料とは判断しない。

管理ユーザーでBillingがAccess Deniedになる場合は、rootユーザーでAccount設定を開き、IAM user/roleによるBilling情報へのアクセス設定を確認する。
確認後は再びrootからログアウトする。

## 6. AWS Budgetsを設定する

Budgetは請求を停止する上限ではなく、実績・予測が閾値に達したことを知らせる監視機能である。

### 6.1 推奨Budget

最初は次の2つを作る。

| Budget | 目的 | 推奨通知 |
|---|---|---|
| Zero spend | 意図しない課金の早期検知 | Actual > 0.01 USD |
| Monthly cost | DEV全体の月額監視 | Actual 50%、80%、100%、Forecasted 100% |

月額値は許容予算に合わせる。実際の契約・為替・使用時間を見ずに固定額を流用しない。

### 6.2 作成手順

1. `Billing and Cost Management` を開く。
2. 左メニューの `Budgets` を選ぶ。
3. `Create budget` を選ぶ。
4. 初回のZero spendは `Use a template (simplified)` → `Zero spend budget` を選ぶ。
5. 通知先メールアドレスを入力する。
6. 内容を確認して作成する。
7. 続けてMonthly cost budgetを作成する。
8. `Cost budget`、期間 `Monthly`、更新 `Recurring`、方式 `Fixed` を選ぶ。
9. 月額予算を入力する。
10. ActualとForecastedの通知閾値を設定する。
11. 通知メールが届くことを確認する。

画面の詳細は[AWS公式: Cost Budget作成](https://docs.aws.amazon.com/cost-management/latest/userguide/create-cost-budget.html)を参照する。

## 7. 外部アカウントを準備する

AWSだけでは現在の構成を再現できない。次も必要である。

### 7.1 GitHub

- ProjectAdminSystemのRepository
- GitHub Actionsが有効
- Environment `dev` を作成できる権限
- Repository Variables / Secretsを設定できる権限

PrivateまたはPublicのどちらでもよいが、Publicへ移す場合も秘密値と非公開業務データをCommitしない。

### 7.2 Cloudflare

- Cloudflare Account
- Zero Trust Organization
- Cloudflareで管理する独自ドメイン
- Terraform用API Token

現在のドメイン・組織名を別環境でそのまま複製せず、構築対象の所有権を確認する。

### 7.3 MongoDB Atlas

- Atlas Project
- Cluster
- ProjectAdmin専用Database User
- EC2 Elastic IPを登録できるNetwork Access権限

パスワードをGitやTerraformへ置かず、AWS Secrets Managerだけへ登録する。

### 7.4 Syncfusion

- 有効なEssential Studio License Key
- GitHub Environment `dev` のSecretへ登録できること
- ローカルでは `frontend/.env.local` にのみ保存すること

## 8. Macへツールを準備する

### 8.1 必須ツール

| Tool | 用途 | 確認 |
|---|---|---|
| Git | Repository取得 | `git --version` |
| AWS CLI v2 | AWS認証・操作 | `aws --version` |
| Terraform 1.15.x | AWS/Cloudflare構築 | `terraform version` |
| Docker Desktop | Local Docker、Image build | `docker version` |
| Docker Compose | 複数Container起動 | `docker compose version` |
| jq | JSON処理 | `jq --version` |
| GitHub CLI | GitHub OIDC・Environment確認 | `gh --version` |

Docker DesktopにはDocker Engine、CLI、Composeが含まれる。[Docker公式: Composeの導入](https://docs.docker.com/compose/install/)

Terraformの導入は[HashiCorp公式: Install Terraform](https://developer.hashicorp.com/terraform/tutorials/aws-get-started/install-cli)に従う。Repositoryの`required_version`は `~> 1.15.0` なので、別のMajor/Minorを無断で使わない。

### 8.2 導入例（Homebrew）

```bash
brew install awscli jq gh
brew tap hashicorp/tap
brew install hashicorp/tap/terraform
```

Docker Desktopは公式InstallerまたはHomebrew Caskから導入し、アプリを起動する。

```bash
docker version
docker compose version
terraform version
aws --version
```

## 9. Repositoryを取得する

```bash
cd /Users/tatsukiakiyama/Documents/ProjectAdmin
git clone <PROJECT_REPOSITORY_SSH_URL> public-repository
cd public-repository
git status
```

すでにRepositoryがある場合は再Cloneしない。作業中の変更を確認してから対象Branchへ切り替える。

## 10. AWS CLIを期限付き認証へ設定する

### 10.1 Profile名

本Repositoryの運用スクリプトは、既定で次を使用する。

```text
project-admin-terraform
```

### 10.2 初回ログイン

```bash
aws login --profile project-admin-terraform
```

リージョンを聞かれたら次を入力する。

```text
ap-northeast-1
```

ブラウザがうまく開かない場合だけRemote方式を使う。

```bash
aws login --remote --profile project-admin-terraform
```

`aws login`はConsole認証を利用し、期限付きの認証情報をCLIやTerraformへ共有する。[AWS公式: aws login](https://docs.aws.amazon.com/cli/latest/userguide/cli-configure-sign-in.html)

### 10.3 確認

```bash
AWS_PROFILE=project-admin-terraform \
aws sts get-caller-identity
```

確認点:

- `Account` が構築対象のAWS Account ID
- `Arn` が想定した管理者
- エラーや期限切れがない

### 10.4 `already configured with Credential Process` の場合

別Profileが `credential_process` からこのProfileを呼ぶ構成では、同じProfileを再設定しようとしてエラーになることがある。まず次で認証元を確認する。

```bash
AWS_PROFILE=project-admin-terraform aws configure list
```

すでに `login` または有効な `credential_process` が認証元なら、Profileを作り直さず、設定済みのログイン元Profileで再認証する。設定ファイルを一括削除しない。

### 10.5 期限切れ

```bash
aws logout --profile project-admin-terraform
aws login --profile project-admin-terraform
AWS_PROFILE=project-admin-terraform aws sts get-caller-identity
```

認証コード、セッショントークン、表示された長いConfirmation URLをチャットやKBへ保存しない。

## 11. 事前確認チェックリスト

- [ ] root MFAが有効
- [ ] root Access Keyが0件
- [ ] 管理者MFAが有効
- [ ] Budget通知を受信済み
- [ ] Plan、Credits、当月利用額を確認済み
- [ ] AWS CLIが対象Accountへログイン済み
- [ ] Regionが `ap-northeast-1`
- [ ] Terraform 1.15.x
- [ ] Docker Desktopが起動済み
- [ ] GitHub、Cloudflare、MongoDB Atlas、Syncfusionを準備済み
