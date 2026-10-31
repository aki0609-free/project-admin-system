# V1主要業務フロー・既存テスト対応表

## 1. 目的

V1で稼働している主要業務について、業務の入口から最終成果物までの流れと、現在存在する自動テストの対応関係を明確にする。

この文書は次の目的で利用する。

- リファクタリング前に、現行動作をどのテストが保護しているか確認する。
- テストが存在する領域と、実務シナリオとして不足している領域を区別する。
- 不具合修正や整理を行う際に、先に追加すべき回帰テストを判断する。
- V1以降にテストを追加・削除した場合の索引として更新する。

基準日は **2026年10月31日**、基準タグは `v1.0.0`、対象ブランチは `refactor/post-v1-stabilization` とする。

## 2. 評価基準

| 評価 | 意味 |
| --- | --- |
| A: 主要経路あり | 単体テストに加え、結合テストまたはE2Eで主要な正常系を確認している。 |
| B: 部分的 | 個別ロジックのテストはあるが、業務の入口から成果物までの連続性、または重要な分岐が不足している。 |
| C: 要整備 | 画面の表示確認や一部サービスのテストに限られ、金額・日付・永続化・成果物を十分に保証していない。 |
| 対象外 | V1の対象外、または現在の運用では使用しない。 |

この評価はコードカバレッジ率ではなく、**金額、日付、確定データ、再実行、帳票・台帳への反映**を基準にしている。

## 3. 全体フロー

```text
会社・業務設定 ─┬─ 顧客／現場／請求設定 ─┬─ 現場配置・配車 ─┐
                ├─ 従業員／契約／給与設定 ┤                 │
                └─ 手当・控除／税率設定 ──┘                 ▼
                                                        日報入力
                                                           │
                      ┌────────────────────────────────────┤
                      ▼                                    ▼
                日次支払・明細                       自社／顧客月次締め
                                                           │
                      ┌──────────────────┬─────────────────┤
                      ▼                  ▼                 ▼
                月次給与帳票       請求書・注文書       取引管理
                      │                  │                 │
                      └──────────────────┴────────┬────────┘
                                                  ▼
                                            各種台帳・出力
```

## 4. 主要業務フローと既存テスト

### 4.1 基盤・マスター

| ID | 業務フロー | 現在確認できる主なテスト | 評価 | 不足している実務ケース |
| --- | --- | --- | --- | --- |
| BAS-01 | ログイン → テナント識別 → 権限制御 | `JwtAuthenticationFilterTest`、`TenantFilterTest`、`frontend/e2e/auth.spec.ts`、`frontend/e2e/role-access.spec.ts` | B | 複数テナントの同一業務データを用いた完全な漏えい防止E2E、AWS設定での認証失敗時表示 |
| BAS-02 | 会社情報の登録・更新 → 帳票で利用 | `CompanyProfileCommandServiceTest`、`CompanyProfileQueryServiceTest`、`frontend/e2e/company-profile-ui.spec.ts`、会社情報の入力検証テスト | B | 未登録時の表示、S3ロゴ取得失敗、登録後の請求書・注文書・給与帳票への反映を一連で確認するテスト |
| BAS-03 | 業務設定・休日設定・給与方針 → 日付・締め計算へ反映 | `BusinessSettingServiceTest`、`BusinessCalendarServiceTest`、`PayrollPolicySettingServiceTest`、`frontend/e2e/business-settings-ui.spec.ts` | B | 日曜日・お盆・土曜通常日を設定画面から変更し、日報初期日付と月払い前倒しへ反映するE2E |
| BAS-04 | 税・社会保険等の計算マスター登録 → 月次計算 | 各Import結合テスト、給与項目ルール・計算テスト | B | 対象月のマスター未登録／期間切れ／重複時の締めエラー表示と、登録後の再試行成功 |

### 4.2 顧客・従業員・給与項目

| ID | 業務フロー | 現在確認できる主なテスト | 評価 | 不足している実務ケース |
| --- | --- | --- | --- | --- |
| MST-01 | 顧客登録 → 契約・請求設定 → 現場別単価 | `CustomerMapperTest`、`CustomerCommandServiceTest`、`CustomerQueryServiceTest`、`CustomerSiteBillingRateCommandServiceTest`、`frontend/e2e/customer-information-ui.spec.ts` | B | 締日、支払月区分、請求書パターン1～3、顧客距離単価を組み合わせた締め結果の確認 |
| MST-02 | 従業員登録 → 契約・給与・税区分 → 利用開始 | `EmployeeAdminServiceTest`、`EmployeeWorkEligibilityPolicyTest`、`PostV1CriticalBusinessFlowsContainerIntegrationTest`、各入力検証、`employee-registration.spec.ts`、`employee-edit-form-ui.spec.ts`、`employee-resignation-ui.spec.ts` | B | 3支払区分の共通fixtureは追加済み。退職境界と画面操作を含むE2Eは未整備 |
| MST-03 | 手当・控除マスター → 従業員紐付け → 日報の動的項目 | 給与項目ポリシー／計算／取引テスト、`DailyReportInputItemServiceTest`、`DailyReportRuleCalculationContainerIntegrationTest`、`payroll-item-master-linkage-ui.spec.ts` | A | 動的項目数が帳票上限付近の場合、無効化済み項目、同名項目、月途中の設定変更 |
| MST-04 | 貯蓄・借入登録 → 日報で積立・返済・引出 → 残額更新 | `EmployeeSavingServiceTest`、`EmployeeLoanServiceTest`、`EmployeeFinanceBalanceCommandServiceTest`、`EmployeeFinanceTransactionServiceTest`、`employee-loan-savings-ui.spec.ts` | B | 過去日報修正・削除後の差分反映、締め跨ぎ、同日複数日報、残額0円到達後の再編集 |
| MST-05 | 住民税編集・CSV取込 → 月別控除へ反映 | `ResidentTaxEditorContainerIntegrationTest`、`ResidentTaxImportContainerIntegrationTest`、`frontend/e2e/resident-tax-editor.spec.ts` | A | AWS上の実ファイル形式差、締め済み月を再編集した後の再締め帳票確認 |

### 4.3 現場配置・日報・日次支払

| ID | 業務フロー | 現在確認できる主なテスト | 評価 | 不足している実務ケース |
| --- | --- | --- | --- | --- |
| DAY-01 | 現場配置・配車 → 日報へ顧客・現場・車両情報を引継ぎ | `DailyPreparationServiceTest`、`DailyReportPreparationDefaultServiceTest`、`DailyReportPlacementServiceTest`、配置画面のfrontend unit tests | B | 配置変更後に日報を再保存した場合の運転手当・顧客請求・その他金額の再計算 |
| DAY-02 | 従業員・勤務日選択 → 支払日初期値算出 | `DailyReportDateSuggestionServiceTest`、`paymentDateSuggestion.test.ts` | A | 画面で手入力した支払日を従業員変更や再計算で上書きしないこと、月境界のE2E |
| DAY-03 | 開始・終了・休憩入力 → 通常・残業・深夜・休日時間 | `DailyReportWorkTimeCalculatorTest`、`DailyReportWorkTimePolicyTest`、`dailyReportTimeCalculator.test.ts` | A | 深夜帯の休憩手入力、日跨ぎ、手入力値を保存・再表示する画面E2E |
| DAY-04 | 日報基本情報 → 顧客請求・距離請求・運転手当計算 | `DailyReportBillingRateServiceTest`、`DailyReportCustomerSiteResolverTest`、`useDailyReportBilling.test.ts` | A | 顧客ごとの単価変更、会社手配／社員手配／同乗者、その他金額を請求締めまで追跡する結合テスト |
| DAY-05 | 日報 → 手当・控除・法定準備金の計算と手入力上書き | `DailyReportInputItemServiceTest`、`DailyReportTrackedDeductionContainerIntegrationTest`、`DailyReportPayrollItemAmountValidationTest`、`payroll-business-flow.spec.ts` | A | 税合計÷20の初期値を実データで確認、負数・返金、残額・累計額、10控除／7手当相当の帳票表示 |
| DAY-06 | 日報保存 → 貯蓄・借入・引出の残高更新 | 従業員金融系テスト、`PayrollBusinessFlowContainerIntegrationTest` | B | 保存失敗時のロールバック、同一日報の再保存による二重計上防止、削除・復元 |
| DAY-07 | 日報 → 支払明細書プレビュー・確定PDF | `DailyPaymentServiceTest`、`DailyPaySlipJasperTemplateTest`、`DailyPreviewHtmlTemplateTest`、`report-ledger-file.spec.ts` | B | 日払い／週払いの集計期間、月払い除外、全動的項目、署名・住所・累計値を含む実データPDF回帰 |
| DAY-08 | 日報 → 日別／月間集計で最新値を確認 | `DailyReportMonthlyAttendanceQueryServiceTest`、`PayrollBusinessFlowContainerIntegrationTest`、`payroll-business-flow.spec.ts` | B | 遅延入力、支払日が同じ複数日報、過去日報修正後の各集計の整合性 |

### 4.4 自社月次締め・給与

| ID | 業務フロー | 現在確認できる主なテスト | 評価 | 不足している実務ケース |
| --- | --- | --- | --- | --- |
| PAY-01 | 対象月選択 → 自社月次締め → Version確定 | `MonthlyClosingCommandServiceTest`、`MonthlyClosingExecutionStateServiceTest`、`MonthlyClosingJobServiceTest`、`MonthlyClosingContainerIntegrationTest` | A | 日払い・週払い・月払いが混在する実務fixtureでの一括締め、画面E2E |
| PAY-02 | 締め失敗 → Versionを消費せず再実行 | `MonthlyClosingContainerIntegrationTest.failedAttemptKeepsHistoryAndCanRetrySameVersion`、Job／Executorテスト | A | Jasper・SQL・計算マスターの各失敗理由が画面に表示され、修復後に同Versionで成功するE2E |
| PAY-03 | 再締め → 新Version → 旧帳票履歴を保持 | `firstClosingAndReclosingCreateImmutableExecutionVersions`、実行状態・帳票ファイルテスト | A | 再締め前後の金額差分、保存PDF、HTML最新プレビューを同時に比較するテスト |
| PAY-04 | 締め期間の給与・税・保険・手当・控除集計 | `DailyPayComponentCalculationServiceTest`、`LegalDepositRefundServiceTest`、給与項目計算テスト、`PayrollBusinessFlowContainerIntegrationTest` | B | 課税対象額、通勤手当除外、社会保険計、年末調整正負、法定準備金返済正負を同一fixtureで検証 |
| PAY-05 | 月次給与明細・労務費一覧の生成 | `PostV1CriticalBusinessFlowsContainerIntegrationTest.dailyReportsSnapshotIntoMonthlyPaySlipsAndLaborCostList`、`MonthlyPaySlipJasperTemplateTest`、Excel renderer／report file lifecycle tests、`report-ledger-file.spec.ts` | B | 共通fixtureから両確定履歴への金額一致は追加済み。JRXML・HTML・Excel間、複数ページ、視覚差分は未整備 |
| PAY-06 | 給与支払表 → 支払日単位の金種・支給額 | `PostV1CriticalBusinessFlowsContainerIntegrationTest.paymentPreparationAggregatesByActualPaymentDateAndCycle`、日付計算、日次支払テスト | A | 3支払区分と金種内訳は追加済み。銀行振込混在、負数・0円、月次精算の追加ケースが必要 |

### 4.5 顧客締め・請求・取引

| ID | 業務フロー | 現在確認できる主なテスト | 評価 | 不足している実務ケース |
| --- | --- | --- | --- | --- |
| BILL-01 | 顧客・対象月選択 → 締め期間算出 | `CustomerBillingPeriodServiceTest`、`CustomerBillingTargetServiceTest` | A | 月末締め以外、当月／翌月／翌々月支払、休日境界を画面操作から確認 |
| BILL-02 | 顧客締め → 請求書・注文書生成 → 顧客を締め済みにする | `CustomerBillingClosingExecutionServiceTest`、`CustomerBillingClosingJobServiceTest`、`CustomerBillingClosingCommandServiceTest` | A | 複数顧客の一括実行で一社のみ失敗、実ファイルを含む画面E2E |
| BILL-03 | 入金済み取引が存在する状態で締め・再締め | `MonthlyClosingCustomerTransactionServiceTest`、`reclosedInvoiceHistoryUpdatesOneCustomerTransactionAndKeepsOldHistory`、顧客取引テスト | B | 入金済み、部分入金、手数料、相殺、再締め金額変更を組み合わせたDB結合テスト |
| BILL-04 | 請求書パターン1・2・3の生成 | `MonthlyInvoiceJasperTemplateTest.compilesAndRendersAllCustomerInvoicePatterns` | B | 締日を基準にした31日マトリクス、職種・現場・工事別改ページ、合計値、レイアウト画像比較 |
| BILL-05 | 注文書生成 | `MonthlyOrderFormJasperTemplateTest` | B | 元請・下請情報、印・担当者欄、支払条件、注記を含む視覚差分と複数ページ |
| BILL-06 | HTML請求プレビューで最新明細確認 | HTML renderer／preview tests | C | 全パターン共通の最細粒度明細、不要な会社・口座情報の非表示、Jasper集計値との一致 |
| BILL-07 | 取引管理 → 入金登録 → 入金確認表へ反映 | `PostV1CriticalBusinessFlowsContainerIntegrationTest.customerClosingCreatesTransactionAndReceiptConfirmationRow`、`CustomerTransactionCommandServiceTest`、`customer-transaction.spec.ts`、`customer-transaction-ui.spec.ts`、Receipt ledger tests | B | 顧客締め履歴→取引→手数料込み入金→入金確認Viewは追加済み。当月／翌々月、部分入金、相殺、再締めの組合せは未整備 |

### 4.6 台帳・ファイル出力

| ID | 業務フロー | 現在確認できる主なテスト | 評価 | 不足している実務ケース |
| --- | --- | --- | --- | --- |
| LED-01 | 入金確認表の生成・編集・再表示 | `ReceiptConfirmationSpreadsheetRendererTest`、`ReceiptConfirmationSpreadsheetEditHandlerTest`、台帳Generation／Editing integration tests | A | 画面で編集・保存後の再読込、印刷、Excel出力を連続して確認するE2E |
| LED-02 | 労務費支払一覧の生成 | `LaborCostPaymentSpreadsheetRendererTest`、`SpreadsheetLedgerGenerationIntegrationTest` | B | 日払い／週払い／月払いの各シート、前借り・借入金返済・差引支給額、実データExcel比較 |
| LED-03 | 月間労務表の生成 | `MonthlyLaborSpreadsheetRendererTest`、`report-ledger-file.spec.ts` | B | 長いヘッダー、31日、休日、その他手当、A列から支給Aまでの部分印刷、Excel表示回帰 |
| LED-04 | 月間集計表の生成 | `MonthlySummarySpreadsheetRendererTest` | B | 社会保険控除額、延べ人数、合計後端数処理、結合セルの値、全列表示の実ファイル検証 |
| LED-05 | Spreadsheet → Excelダウンロード | `SpreadsheetLedgerExcelExportServiceTest`、`report-ledger-file.spec.ts` | B | ブラウザダウンロード結果を開いて数式・結合・罫線・シート名まで検証 |
| LED-06 | 範囲選択 → 部分印刷 | frontend spreadsheet presentation unit test、操作準備E2E | C | 選択範囲、未選択、複数範囲、縦横ページ、印刷プレビューの自動検証 |

### 4.7 取込・人事分析・運用管理

| ID | 業務フロー | 現在確認できる主なテスト | 評価 | 不足している実務ケース |
| --- | --- | --- | --- | --- |
| IMP-01 | 所得税表取込 → 控除計算へ反映 | `IncomeTaxImportContainerIntegrationTest`、Import共通テスト、`system-import-ui.spec.ts` | A | 実配布ファイルの年度差、再取込、対象月締め・再締め帳票までの確認 |
| IMP-02 | 雇用保険等の料率取込 → 控除計算へ反映 | `InsuranceRateImportContainerIntegrationTest` | A | 期間境界、重複期間、複数料率、画面からの実ファイル取込E2E |
| IMP-03 | 住民税取込・手入力 → 月別控除 | Resident taxの取込／編集Container tests、E2E | A | 大量従業員、未登録従業員コード、年度切替、AWSファイル保存経路 |
| IMP-04 | 子ども・子育て支援金等の取込・確認 | Import共通基盤テスト | C | 対象固有の詳細テーブル／計算反映／確認画面を保証する専用結合テスト |
| HR-01 | 応募媒体登録 → 応募者登録 → 媒体実績再計算 | `ApplicantCommandServiceTest`、`ApplicationMediaCommandServiceTest`、`ApplicationMediaMetricsServiceTest`、`application-management-ui.spec.ts` | A | 複数媒体・採用取消・費用変更・大量件数の集計整合性 |
| OPS-01 | 帳票・テンプレート・文書管理 | report master／storage／template tests、`document-management-ui.spec.ts`、`system-report-ui.spec.ts` | B | Workingと実行資産の不一致、AWS S3未配置、Jasperバージョン差のデプロイ前検出 |
| OPS-02 | バックアップ・バッチ・メール・通知 | 各Service tests、`system-backup-ui.spec.ts`、`system-batch-ui.spec.ts`、`system-mail-ui.spec.ts`、notice E2E | B | 本番相当設定での復元演習、失敗通知、再実行、部分成功時の運用手順 |

## 5. 現時点の重要な結論

### 5.1 比較的保護されている領域

- 日報の支払日初期値、勤務時間、休日時間、距離請求の個別計算
- 動的な手当・控除のルール計算と手入力上書き
- 月次締めのVersion管理、失敗後の同Version再試行、再締め履歴
- 顧客締めの成功・帳票失敗・再締めのサービス制御
- 住民税、所得税、保険料率の主要な取込処理
- Jasperテンプレートのコンパイルと基本レンダリング
- 各Spreadsheet台帳の生成ロジック

### 5.2 最優先で補強すべき領域

1. **日払い・週払い・月払いの実務fixtureを使った一気通貫テスト**
   - 日報入力、支払日、日次支払、月次締め、給与明細、労務費一覧までを同じ期待値で検証する。
2. **顧客締めから取引・入金確認表までの一気通貫テスト**
   - 入金済み、部分入金、再締め、当月・翌月・翌々月を含める。
3. **給与支払表の支払日単位テスト**
   - 日払い、翌週月曜、翌月15日前倒し、銀行振込混在、金種を明示する。
4. **帳票・台帳の値とレイアウトの回帰テスト**
   - JRXMLのコンパイル成功だけでなく、PDFページ数・必須文言・項目順・主要座標または画像差分を検証する。
5. **ローカルとAWSの設定差異テスト**
   - 会社情報未登録、S3資産未配置、計算マスター不足を「取得失敗」と混同しない。

## 6. 推奨する追加テストの実装順

| 優先度 | 追加するテスト | 対応するフロー |
| --- | --- | --- |
| P0（初回完了） | 3従業員（日払い・週払い・月払い）× 1か月の固定fixture | DAY-02～08、PAY-01～06 |
| P0（初回完了） | 日報 → 自社締め → 月次給与明細／労務費一覧の確定履歴Container結合テスト | PAY-01～05 |
| P0（初回完了） | 顧客締め履歴 → 取引 → 入金確認ViewのContainer結合テスト | BILL-01～07、LED-01 |
| P0（初回完了） | 給与支払表の支払日別集計テスト | PAY-06 |
| P1 | 請求書3パターン・注文書・月次給与明細のPDF回帰テスト | BILL-04～06、PAY-05 |
| P1 | 台帳4種の実ファイルExcel回帰テストと部分印刷E2E | LED-01～06 |
| P1 | 会社情報未登録・S3未配置・計算マスター不足の環境差テスト | BAS-02、BAS-04、OPS-01 |
| P1 | 手当・控除・貯蓄・借入の修正／削除／再保存テスト | MST-03～04、DAY-05～06 |
| P2 | 子育て支援金など取込対象別の専用結合テスト | IMP-04 |
| P2 | 応募媒体の実績再計算と分析画面の複合E2E | HR-01 |

## 7. テストスイートの実行方法

### バックエンド単体テスト

```bash
cd backend
./gradlew test
```

`test` は `integration` タグを除外して実行する。

### DB・Testcontainers結合テスト

```bash
cd backend
./gradlew integrationTest
```

### フロントエンド単体テスト

```bash
cd frontend
npm run test:unit -- --run
```

### Playwright E2E

```bash
npm run test:e2e
```

### 帳票Working資産と実行資産の同期確認

```bash
npm run reports:workspace:check
```

## 8. 更新ルール

- 業務仕様を変更した場合は、実装より先に該当行の「不足している実務ケース」を更新する。
- 新しいテストを追加した場合は、対象IDへテスト名を追記し、評価を見直す。
- テストを削除・統合した場合は、参照切れを残さない。
- 帳票・台帳のレイアウト変更は、値のテストと表示のテストを分けて記録する。
- 不具合修正の回帰テストは、対応するフローIDをテスト名またはコミット説明へ記載する。
- 四半期ごと、または大きな締め仕様変更後に全表を再確認する。

## 9. 関連文書

- [V1後リファクタリング指針](post-v1-refactoring-guidelines.md)
- [Playwright E2Eテスト基盤](playwright-e2e-test-foundation.md)
- [Testcontainers結合テスト基盤](testcontainers-integration-test-foundation.md)
- [月次締め帳票出力基盤](monthly-closing-output-foundation.md)
- [動的手当・控除ルール](daily-report-dynamic-payroll-items-rule-v1.md)
- [税データ取込](tax-data-import-v1.md)
- [Syncfusion Spreadsheet基盤](syncfusion-spreadsheet-foundation.md)

## 10. 2026年10月31日 初回P0テスト追加

共通fixture `PostV1PayrollScenarioFixture` に、2026年7月分の次の固定データを定義した。

- 日払い・日給者: 1日報、12,000円、7月4日支払
- 週払い・日給者: 2日報、合計26,000円、7月13日支払
- 月払い・日給者: 2日報、合計28,000円、8月14日支払
- 月間総支給額: 66,000円

`PostV1CriticalBusinessFlowsContainerIntegrationTest` に次の4ケースを追加した。

1. `threePaymentCyclesShareOneJulyFixtureWithExpectedPaymentDates`
   - 3支払区分、勤務日、支払日、日報件数、月間総額を確認する。
2. `dailyReportsSnapshotIntoMonthlyPaySlipsAndLaborCostList`
   - 自社締めVersionを開始し、日報から月次給与明細と労務費一覧の確定履歴を実ストアドで作成する。両帳票が3名・66,000円で一致し、締めがVersion 1で完了することを確認する。
3. `customerClosingCreatesTransactionAndReceiptConfirmationRow`
   - 確定請求履歴から顧客取引を同期し、手数料込みの入金登録後に入金確認Viewが残額0円・入金済みになることを確認する。
4. `paymentPreparationAggregatesByActualPaymentDateAndCycle`
   - 実際の支払日ごとに日払い・週払い・月払いが集計され、月払い28,000円の金種が正しいことを確認する。

実行コマンド:

```bash
cd backend
./gradlew integrationTest \
  --tests com.project.backend.testsupport.PostV1CriticalBusinessFlowsContainerIntegrationTest
```

