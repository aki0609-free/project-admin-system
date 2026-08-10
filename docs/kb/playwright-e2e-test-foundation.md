# Playwright E2Eテスト基盤

## 1. 目的

ProjectAdminSystem V1の主要な画面操作を、実際のChromiumブラウザで自動確認する。

業務計算の全パターンを画面テストへ寄せず、次のように責務を分ける。

| テスト層 | 主な責務 |
|---|---|
| JUnit単体テスト | Utility、Validator、Mapper、単一Service |
| Testcontainers統合テスト | MySQL、MongoDB、Redis、税金、控除、日報、締め、履歴、時系列 |
| Playwright E2E | ログイン、権限制御、画面遷移、入力、保存、プレビュー、ダウンロード |
| AWSスモークテスト | デプロイ後の代表機能と外部接続経路 |

## 2. 現在の確認範囲

現在は次の20件を実装した。

1. 未認証状態で保護画面を開くとログインへ戻る
2. 誤った認証情報ではログインできず、エラーメッセージを表示する
3. ローカルE2E専用SYS_ADMINでログインできる
4. 日次管理画面を開ける
5. 日次帳票一覧を開き、日別労務費一覧のHTMLプレビューに対象日の従業員データが表示される
6. 台帳管理画面を開ける
7. 日単価契約を使って月間集計表をテンプレートから生成し、対象月・保存先・顧客・現場・Workbook内容をSpreadsheetへ表示できる
8. 固定従業員を登録・更新し、従業員情報画面で確認できる
9. 固定日報を登録・更新し、時給・残業計算結果と承認状態を日報画面で確認できる
10. 固定対象月の月次集計画面で、日報を集計した概要を確認できる
11. 日報の控除タブで、寮費・携帯料金の金額、支払日数、現在残、保存後残日数を確認できる
12. 従業員画面から新規登録・控除設定・設定変更を行い、Rule基準額の手動変更理由を保存し、無効化した控除が新しい日報へ表示されない
13. 顧客を画面登録し、取引管理で対象月・請求額・入金状態を確認できる
14. 控除マスターから年度別住民税を入力・検証・確定し、月別確定値を再取得できる
15. 日払い明細PDFを実生成・保存し、ダウンロードしたファイルのContent-Type、ファイル名、サイズ、`%PDF-`シグネチャを確認する
16. 労務費一覧表Excelを月次履歴から実生成・保存し、ダウンロードしたファイルのContent-Type、サイズ、ZIP（XLSX）シグネチャを確認する
17. 月間労務表を従業員個別および全選択で生成し、選択値ごとの保存先とWorkbookを確認する
18. MANAGERは従業員情報を開けるがシステムRule管理は拒否され、OPERATORは従業員情報も拒否される
19. 従業員画面の専用ツールバーに「個別日別給与明細」と「従業員CSV出力」が表示され、支払日・従業員・削除済み含有を指定できる
20. 従業員CSVを帳票基盤で生成・履歴保存・ダウンロードし、UTF-8 BOM、日本語ヘッダー、固定従業員コードを確認する

日次管理・台帳管理では、同一オリジンへのHTTP 5xx応答が発生していないことも確認する。

業務データは認証セットアップ時にAPIから冪等に作成する。同じテストを繰り返しても、同じ従業員・同じ日付の日報を更新するため重複しない。

画面登録テストは、削除済み社員を含め社員コードを再利用しないDB制約に合わせ、実行ごとに固有の社員コードを採番する。確認後は通常の従業員削除APIを通し、一覧へテスト社員を残さない。

### 固定業務データ

| 項目 | 値 |
|---|---|
| 従業員コード | `E2E-EMP-001` |
| 顧客 | `E2E 月間集計検証顧客` |
| 現場 | `E2E 東京検証現場` |
| 職種 | `E2E_GENERAL_WORK / E2E 一般作業員` |
| 請求単位 | 日単価（`DAILY`） |
| 基本日単価 | 22,000円 |
| 残業請求単価 | 2,750円／時間 |
| 対象日 | `2026-08-10` |
| 対象月 | `2026-08` |
| 給与形態 | 時給制 |
| 時給 | 1,500円 |
| 通常時間 | 8時間 |
| 残業時間 | 2時間 |
| 通常給 | 12,000円 |
| 残業給 | 3,000円 |
| 総支給・差引見込 | 15,000円 |
| 住民税（2026年8月） | 11,000円 |
| 寮費 | 450円×3日＝1,350円 |
| 携帯電話貸出料 | 200円×5日＝1,000円 |
| 控除合計 | 2,350円 |
| 控除反映後の差引見込 | 12,650円 |

月次帳票のファイル生成には、日報計算用の時給社員とは別に次の固定データを使用する。

| 項目 | 値 |
|---|---|
| 従業員コード | `E2E-MONTHLY-001` |
| 従業員名 | `E2E 月次帳票検証社員` |
| 給与形態 | 月給制 |
| 月給 | 300,000円 |
| 対象月 | `2026-08` |
| 給与支払日 | 翌月15日 |
| 会社名 | `E2E ローカル検証会社` |

この月次帳票fixtureはファイル生成経路の検証用であり、仮の法定税率を登録しない。税率・税額・控除計算の正確性は、公式データを投入するTestcontainersテストで別に検証する。

住民税は年度データの画面入力・下書き検証・確定まで実行する。月次給与Viewと締め履歴への控除反映は、同じサービスと本番SQL資産を使うTestcontainersで確認する。

寮費と携帯電話貸出料は、どちらも「日数残高を持つ控除」として従業員へ適用する。携帯電話貸出料は固定日額を支払日数へ掛け、金額変更時はRule基準額と変更理由を別に保存する。

月間集計表は、固定顧客・現場の日単価契約を日報へスナップショット保存して生成する。単価マスターを後から変更しても、既存日報は入力時点の単価を保持する。

## 3. 前提条件

- Docker Desktopが起動済み
- Node.js 22.12以降
- ローカルDocker環境が起動済み
- テスト対象URLは原則`http://localhost:5173`

初回だけChromiumをインストールする。

```bash
cd /Users/tatsukiakiyama/Documents/ProjectAdmin/public-repository/frontend
npm run test:e2e:install
```

## 4. ローカル実行

プロジェクトルートでローカル環境を起動する。

```bash
cd /Users/tatsukiakiyama/Documents/ProjectAdmin/public-repository
npm run docker:dev
```

Docker Composeはバックエンド起動後、`runtime-schema`サービスで本番と同じSQL資産を順番に適用する。SQL適用が正常終了してからフロントエンドを起動するため、新規DBでも控除マスター・残高ポリシー・帳票View・ストアドが揃う。

```text
MySQL・MongoDB・Redis
  → Backend（Hibernateテーブル確認）
  → Runtime Schema SQL（完了型サービス）
  → Frontend
```

Runtime Schemaは再実行可能であり、手当・控除残高ポリシーは次の複合キーで重複を防止する。

```text
tenant_id + target_type + target_code
```

SQLは`utf8mb4`で適用し、日本語マスター名の文字化けを防止する。

ローカルDockerだけは、Runtime Schemaの最後に次のローカル専用SQLも適用する。

```text
sql/local/repair_deduction_master_encoding.sql
sql/local/demo_monthly_summary_fixture.sql
sql/local/demo_monthly_payroll_fixture.sql
```

`repair_deduction_master_encoding.sql`は、過去に文字コード指定なしで作成されたローカルDBボリュームを修復するための互換SQLである。法定控除6件の名称・説明だけを正しい日本語へ更新し、金額、計算ルール、利用者が設定した業務値は変更しない。

`demo_monthly_summary_fixture.sql`は本番環境のRuntime Schema Manifestへ含めない。顧客名・現場名・社員コード・対象日をキーに既存データを再利用するため、Dockerを再起動しても同じデータを重複登録しない。

`demo_monthly_payroll_fixture.sql`は、月次PDF・Excel出力に必要な月給社員、対象月、会社情報、給与締日設定をローカルだけに用意する。既存の会社・締日設定がある場合は追加せず、税率マスターへ仮データを登録しない。労務費一覧表は締め後の実運用経路に合わせ、固定Version `900001` の確定履歴から`RETRY`で再出力する。

画面で月間集計表を確認する場合：

```text
1. http://localhost:5173 へログイン
2. 締め処理 → 台帳
3. 年度「2026年度」、対象月「2026年8月」を選択
4. 月間集計表の「生成・確認」を押す
5. E2E 月間集計検証顧客／E2E 東京検証現場の値を確認
```

全サービスがhealthyになった後、E2Eテストを実行する。

```bash
npm run test:e2e
```

画面を見ながら調査する場合：

```bash
cd frontend
npm run test:e2e:ui
```

HTMLレポートを開く場合：

```bash
cd frontend
npm run test:e2e:report
```

## 5. E2E専用ユーザー

ローカルDocker環境では、既存の管理者ユーザーを変更せず、次のE2E専用SYS_ADMINを起動時に作成・更新する。

```text
username: playwright_local
password: ローカルDocker専用値
tenant: default
```

この初期化は次の両方を満たす場合だけ有効になる。

- Spring Profileが`local`
- `APP_E2E_USER_ENABLED=true`

AWSの`aws`プロファイルでは作成しない。既存の個人用ローカル管理者パスワードにも依存しない。

認証情報を明示的に変更して実行する場合：

```bash
E2E_USERNAME='...' \
E2E_PASSWORD='...' \
npm run test:e2e
```

## 6. リモート環境の誤操作防止

Playwrightは既定で次のホストだけを許可する。

- `localhost`
- `127.0.0.1`
- `::1`

AWS DEV等を対象にする場合は、承認済みの非本番環境であることを確認したうえで、明示的に許可する。

```bash
E2E_BASE_URL='https://approved-dev.example.com' \
E2E_ALLOW_REMOTE='true' \
E2E_USERNAME='...' \
E2E_PASSWORD='...' \
npm run test:e2e
```

月次締め、再締め、削除、メール送信等の状態変更テストは、共有AWS DEVで常時実行しない。Testcontainersまたは使い捨てテスト環境で実行する。

## 7. 失敗時の証跡

テスト失敗時に次を保存する。

- スクリーンショット
- 操作動画
- Playwright trace
- HTMLレポート

保存先：

```text
frontend/test-results/e2e/
frontend/playwright-report/
```

これらはGit管理しない。

## 8. GitHub Actions

CIでは通常テストとTestcontainers統合テストが成功した後、次を実行する。

```text
1. Chromiumと必要ライブラリをインストール
2. Docker Composeで使い捨てローカル環境を起動
3. Playwrightスモークテストを実行
4. 失敗時はアプリケーションログを表示
5. コンテナとテスト用Volumeを削除
```

CIのDBは毎回新規作成されるため、前回のユーザーや業務データへ依存しない。

## 9. 月次締め・再締めの検証方針

月次画面の表示はPlaywrightで確認する。一方、締め処理は帳票・台帳・請求取引・履歴Versionを生成するため、通常のローカルDBや共有AWS DEVに対するE2Eでは自動実行しない。

現在は次のTestcontainersテストで、使い捨てDBに対して検証する。

| テスト | 確認内容 |
|---|---|
| `PayrollBusinessFlowContainerIntegrationTest` | 従業員・契約、日報給与計算、住民税確定、月次集計の数値整合性 |
| `DailyReportTrackedDeductionContainerIntegrationTest` | 寮費・携帯料金の日額×日数、残日数、基準額、手動変更、変更理由 |
| `PayrollItemBalanceContainerIntegrationTest` | 月途中開始、未払い繰越、編集時の自己明細除外、負数防止 |
| `MonthlyClosingContainerIntegrationTest` | 初回締め、再締め、履歴Versionの不変性、請求取引更新 |
| `ResidentTaxEditorContainerIntegrationTest` | 締め済み月の税額変更と再締め確認 |
| `ReportFileLifecycleContainerIntegrationTest` | 締めVersionごとの帳票ファイル履歴 |

実行例：

```bash
cd /Users/tatsukiakiyama/Documents/ProjectAdmin/public-repository/backend
./gradlew --no-daemon integrationTest \
  --tests com.project.backend.testsupport.PayrollBusinessFlowContainerIntegrationTest \
  --tests com.project.backend.features.operation.monthly.service.MonthlyClosingContainerIntegrationTest
```

## 10. 次に追加するシナリオ

優先順は次のとおり。

1. 帳票ツールバーからのPDFプレビュー・CSVダウンロードのブラウザ操作
2. AWS DEVへ反映後の読み取り中心スモークテスト

保存PDF・Excel・従業員CSVと従業員選択型台帳はPlaywrightで確認済み。CSVの初回締め・再締めVersion保持は`ReportFileLifecycleContainerIntegrationTest`で確認する。

- UTF-8 BOM付きCSVの生成内容
- LOCALストレージへの保存
- 初回締め・再締めで異なるファイルを保持
- 帳票履歴と締めVersionの対応

数値境界、月末月初、年度跨ぎ、閏年、繰越はTestcontainers側で網羅する。Playwrightでは利用者が通る代表的な操作経路を確認する。

## 11. 完了条件

- ローカルで20件の認証・権限・画面・固定業務フローテストが成功する
- Testcontainersで給与計算・住民税・月次集計の数値が一致する
- Testcontainersで寮費・携帯料金の金額・数量・残高・変更理由が一致する
- Testcontainersで初回締め・再締めのVersionと請求取引が一致する
- 日次HTML帳票に対象日の固定業務データが表示される
- 日単価契約を使った月間集計表が生成され、顧客・現場を含む保存済みWorkbookを画面表示できる
- 日払い明細PDFと労務費一覧表Excelを実際に保存・取得し、ファイル形式とサイズが一致する
- 個別日払い明細PDFは支払日と従業員IDを指定して生成できる
- 従業員CSVを帳票履歴に保存・取得し、UTF-8 BOMと日本語ヘッダーを確認できる
- 月間労務表を従業員個別・全選択で生成し、選択値ごとのWorkbookが保存される
- CSVの生成・保存・再締めVersion保持がTestcontainersで成功する
- E2E専用ユーザーが既存管理者へ影響しない
- リモート環境への誤実行を既定で拒否する
- 失敗時にスクリーンショット・動画・traceを取得できる
- GitHub ActionsでPRごとに再現できる
