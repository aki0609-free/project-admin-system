# HANDOFF（Codex ⇄ Claude Code）

## 役割分担
- **Codex**: メイン実装担当
- **Claude Code**: 既存仕様書・ソースの段階的解析とドキュメント化（`docs/claude-analysis/`）。ソース編集は行わない。

## 最新進捗（Claude Code）
- 2026-11-15: 解析基盤をセットアップ（CLAUDE.md, `.claude/agents/code-analyzer.md`, `docs/claude-analysis/_INDEX.md`）。次は 1-1 employee（auth は現在ブランチで修正中のため後回し）。
- 2026-11-15: 1-1 employee 解析完了 → `docs/claude-analysis/employee.md`（仕様書との乖離22件、要確認11件）。次は 1-5 payroll（master/payrollitem 依存の確認）を推奨。
- 2026-11-15: 1-5 payroll 解析完了 → `docs/claude-analysis/payroll.md`（乖離10件、要確認8件、employee 持ち越し Q1〜Q4 回答）。次は 1-3 daily-report を推奨。
- 2026-11-15: 1-3 daily-report 解析完了 → `docs/claude-analysis/daily-report.md`（乖離12件、要確認9件、持ち越し Q1〜Q5 回答）。次は 1-7 operation（月次締めのロック確認）を推奨。
- 2026-11-15: 1-7a monthly-closing 解析完了 → `docs/claude-analysis/monthly-closing.md`（乖離10件、要確認12件、持ち越し Q1〜Q4 回答）。operation は 1-7a〜e に分割済み。次は 0-2 tenant-boundary を推奨。
- 2026-11-15: 0-2 tenant-boundary 解析完了 → `docs/claude-analysis/tenant-boundary.md`（過去解析の未確認7件を検証、要確認10件）。**TenantFilter の Hibernate Filter 有効化が実際には効いていない可能性（Q1）を実環境で要確認**。
- 2026-11-15: ユーザー方針「現在は単一テナント運用。業務フローの正確性を優先」→ テナント関連は低優先に変更。0-2b raw-sql-audit 解析完了 → `docs/claude-analysis/raw-sql-audit.md`（業務指摘 高2・中10・低9）。次は 1-7e report-preview（請求額の計算経路の二重化を確認）を推奨。

## Codex への申し送り
- **[raw-sql-audit・高・確認済み]** 顧客請求締めの概算（CustomerBillingSummaryService.java:41-49,64-88）で、`billing_unit` が NULL の日報は基本料金0円かつ not_ready_count にも数えられない（`NOT IN` が NULL を判定しない）。単価 NULL も COALESCE で0円・警告なし。請求漏れのまま「準備完了」と表示される。
- [raw-sql-audit・中] 締め画面の請求額（daily_report 直計算）と請求書（View の line_amount 合算）の計算経路が二重。日次の法定控除概算（DailyStatutoryDeductionEstimateService）は率表・税表の行選択、端数処理（HALF_UP）、税表の境界に問題。通知の日付列方式は期間で絞らず、1001件目で例外になり通知が止まる。
- [employee] 社員コード一意制約 `(tenant_id, employee_code)` に deleted_at が無く、論理削除後の再登録で制約違反の可能性（Employee.java:16-21）。
- [employee] 退職 `note` が Service で破棄されている／RESIGNED への再 resign で退職日上書き可（Employee.java:114-126）。
- [employee] 削除時に payroll_item_enrollment / transaction が残り、参照チェックもなし。
- [payroll] 仕様書は carryForward / advanceConsumption を「未参照」とするが実装は参照済み（PayrollItemBalanceQueryService.java:205-217）。仕様書側の更新が必要。
- [payroll] enrollment の INSERT/参照に tenantId 条件なし（EmployeePayrollItemEnrollmentRepository.java:12-26）。
- [payroll] 取引の source_reference 一意制約に deleted_at なし → 論理削除後の再登録で制約違反の可能性（DDL で確認済み: payroll_item_transaction_foundation_v1.sql:84-86）。
- [daily-report] SQL 適用順次第で寮費が AUTO（DORMITORY_DAILY_FEE）に戻り、金額×金額の不整合になる恐れ（dormitory_foundation_v1.sql:247-256 vs payroll_item_balance_foundation_v1.sql:434-441）。→ manifest 順では MANUAL で確定（monthly-closing 解析で確認）。手動で個別再適用した場合のみ注意。
- [daily-report] 日報の削除・更新時の貸付の戻し先が「最新の承認済み貸付」で、元の返済先とずれる可能性。積立を無効化した後は日報を更新・削除できなくなる（EmployeeFinanceBalanceCommandService.java:61-70,124-133）。
- [daily-report] 月次締め済み期間の日報編集・削除のロックが dailyreport 内に無い。
- [monthly-closing] 締め済み期間のロックはシステム全体に無い（tax の2サービスのみ締め状態を参照）。未使用の `MonthlyClosingRepository.existsByTargetMonthAndDeletedAtIsNull` あり。給与明細 view は live データ参照のため、締め後の修正で確定値と乖離。
- [monthly-closing] 異常終了で PROCESSING のまま残ると close/reclose が恒久的に不可（MonthlyClosingCommandService.java:131-133）。
- [tenant-boundary・低優先（現在は単一テナント運用のため。複数テナント化の前に要対応）] TenantFilter はトランザクション/OSIV より前で `entityManager.unwrap(Session.class).enableFilter(...)` を呼んでおり、一時 Session に対する有効化で終わっている可能性。その場合、`@EnableHibernateFilters` の無い全サービスで派生クエリのテナント絞込が効かない。SQL ログで `tenant_id=?` の有無を確認してほしい（TenantFilter.java:100-104、テストはモックのみ）。
- [tenant-boundary] findById / REQUIRES_NEW / UPDATE は Filter 対象外。`tenant_id` に updatable=false なし。BaseEntity 非継承 Entity 8件（RefreshToken, Role, Permission, AsyncJob, FeatureFlag, 税マスタ3件）。
- [monthly-closing] 必須出力の失敗判定が実質無効（completeItems が先に全件 COMPLETED）。失敗 Version 番号の再利用は仕様書と不一致。
## Claude Code への申し送り（Codex 記入欄）
- （解析してほしい対象・優先度があればここに追記）
