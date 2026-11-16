# raw-sql-audit 解析結果
- 対象: native query リポジトリ6本 + JdbcTemplate 利用19本（計25ファイル。一覧は §2）
- 解析日: 2026-11-15（作業ツリー基準）
- 方針: 単一テナント運用のため**業務正確性を優先**。tenant_id 条件は参考（§6）。
- 解析方法: Claude Code の読み取り専用サブエージェントによる静的解析（指定25ファイルのみ。View・Builder・Validator は未読）。§3 の「高」2件はメインセッションでコードを直接確認済み。
- パス表記: `BE/` = `backend/src/main/java/com/project/backend`
- 関連: [tenant-boundary.md](tenant-boundary.md)（§4/§8 の対象一覧）、[monthly-closing.md](monthly-closing.md)、[daily-report.md](daily-report.md)

## 1. 概要
- **重大（業務金額）**: `CustomerBillingSummaryService` では、`billing_unit IS NULL` の日報は基本料金が0円になり、未整備件数（not_ready_count）にも数えられません。そのため請求額が少なく出たまま「準備完了」と表示されます（`NOT IN` は NULL を判定できないため）。単価が NULL の場合も COALESCE で0円になり、警告は出ません。**（メインセッションで確認済み: CustomerBillingSummaryService.java:41-49,64-88）**
- **中（金額の不一致）**: 顧客締め画面の請求額（`daily_report` から直接計算し、成分ごとに CEILING）と、請求書プレビューの金額（View の `line_amount` を Java 側で合算）は計算経路が別です。両者がずれる可能性があります。
- **中（算定）**: 日次の法定控除概算で、次の問題があります。
  - 保険料率と所得税表を「最初の行」で採っており、論理削除も除外していません。
  - 保険料の端数処理が HALF_UP（法定は「50銭以下切捨て」）です。
  - LEFT JOIN で行が増えたとき、どの行を採るかが不定です。
- **中（運用停止リスク）**: Notice の日付列方式は期間で絞っていないため、データが増えると1000件の上限を超えて例外になり、通知が止まります。台帳（ExcelBook）は WHERE テンプレートが未設定だと対象月で絞られません。
- SQL インジェクションは、大半が識別子の正規表現チェックと許可リスト（catalog）で防がれています。ただし任意SQLを素通しする実行器が3つあり（Backup / ReportSql / OutputTable.query）、`GenericImportWriter` には検証がありません。SQL の出所が信頼できる設定に限られていることが前提です。

## 2. ファイル別サマリ
凡例: B1=論理削除 / B2=状態条件 / B3=期間・日付 / B4=計算・集計 / B5=SQL安全性 / T=tenant_id 条件。○=問題なし、△=条件付き・要確認、×=問題あり、－=該当なし

| # | ファイル | 用途 | 参照テーブル | B1 | B2 | B3 | B4 | B5 | T |
|---|---|---|---|---|---|---|---|---|---|
|1|BE/app/security/permission/repository/RoleRepository|ロール権限の紐付け削除|role_permission|－|－|－|－|○ 固定SQL|不要 :28|
|2|BE/features/dailyreport/repository/DailyReportDeductionRepository|控除数量の期間合計|daily_report_deductions, daily_report|△ 日報のみ除外|△ 承認状態を見ない|○ work_date の BETWEEN（両端含む）|○ COALESCE あり|○|あり :24-25|
|3|BE/features/dailyreport/repository/DailyReportAllowanceRepository|手当数量の期間合計|daily_report_allowances, daily_report|△ 同上|△ 同上|○|○|○|あり :24-25|
|4|BE/features/user/repository/UserRepository|ユーザーロールの紐付け削除（native）|user_role|－|－|－|－|○|なし :71|
|5|BE/features/system/mail/repository/MailRecipientGroupRepository|削除済みも含めキーで取得|mail_recipient_group|－ 意図的に含む|－|－|△ ORDER BY なしの LIMIT 1|○|なし :13|
|6|BE/features/master/payrollitem/transaction/EmployeePayrollItemTransactionRepository|確定数量の合計|employee_payroll_item_transaction|○|○ CONFIRMED|△ transaction_date 基準|○|○|あり :35|
|7|BE/features/system/backup/service/fetcher/BackupDataFetcher|任意SQLの実行|呼出元依存|－|－|－|－|△ 素通し|呼出元依存|
|8|BE/features/system/report/service/api/ReportSqlExecutorService|任意の更新SQL実行|呼出元依存|－|－|－|－|△ 素通し（DML）|呼出元依存|
|9|BE/app/sql/service/ProcedureExecutorService|CALL proc(:executionId)|動的プロシージャ|－|－|－|－|○ 正規表現|－|
|10|BE/batch/writer/GenericImportWriter|動的 INSERT|動的テーブル|－|－|－|－|× 検証なし|なし|
|11|BE/features/system/excelbook/service/SnapshotMapQueryService|スナップショットの月別取得|動的テーブル|－ 削除条件なし|－|○ target_month|－|○ 正規表現|なし :28|
|12|BE/features/operation/monthly/service/MonthlyInvoiceTargetCustomerQueryService|請求対象の顧客|vw_monthly_invoice_latest_detail|View 依存|View 依存|○ BETWEEN|○ DISTINCT|○|あり :26|
|13|BE/features/system/report/service/api/OutputTableQueryService|出力テーブルの取得と任意SQL|動的|－|－|－|－|△ query() は素通し|なし :34|
|14|BE/features/operation/monthly/service/LegalDepositRefundService|法定準備金の返金を作成|vw_monthly_pay_slip_legal_deposit_refund|View 依存|○ ACTIVE→SUPERSEDED|○ target_month の初日|△ 0円・NULL行も登録|○|あり :28|
|15|BE/features/system/notice/service/resolver/DateColumnNoticeTargetResolver|通知対象（日付列）|動的|△ 設定依存|△ 設定依存|× 期間なし（全件）|－|△ WHERE は外部検証|あり :49|
|16|BE/features/system/notice/service/resolver/DayRuleNoticeTargetResolver|通知対象（日ルール）|動的|△ 設定依存|△ 設定依存|△ 当月のみ|－|△ 同上|あり :58|
|17|BE/app/sql/service/DynamicTableInsertService|メタ情報に基づく INSERT|動的と information_schema|－|－|－|－|○ 正規表現と列の許可リスト|なし（値に依存）|
|18|BE/features/system/imports/service/executor/ImportWriteExecutor|取込の DELETE/INSERT/UPDATE/EXISTS|catalog のテーブル|△ 物理削除|－|－|－|○ catalog（Builder 依存）|あり（tenantScoped 時）|
|19|BE/features/operation/monthly/service/CustomerBillingSummaryService|顧客請求の小計と税|daily_report, customer_billing_closing|○|○ APPROVED|△ 締め日当日を到達扱い|× NULL の扱い|○|あり :55|
|20|BE/features/system/rule/service/fetcher/GeneralDataFetcher|ルール用データの取得|catalog のテーブル|△ 設定依存|△ 設定依存|－|△ singleRow で2行を返しうる|△ WHERE テンプレート未検証|あり（:tenantId 必須）:71|
|21|BE/features/operation/reportpreview/config/OperationReportPreviewReadinessValidator|起動時の整合性チェック|information_schema, operation_report_preview, batch_job_definition|○|○ active_flag|－|○|○ 固定SQL|固定 'default' :85|
|22|BE/features/admin/business/service/PreviewReportSettingService|プレビュー定義の登録|information_schema|○|－|－|－|△ 存在確認のみ|あり（JPA）|
|23|BE/features/dailyreport/service/DailyStatutoryDeductionEstimateService|月額法定控除の概算|payroll_calculation_period ほか5表|△ 率表・税表は除外なし|△ verified のみ|△ 税表の境界|× 行の増殖、端数|○|一部なし :193,:213|
|24|BE/features/operation/reportpreview/service/OperationReportPreviewRowReaderService|プレビュー行の取得|動的（設定）|View 依存|View 依存|△ date_format|△ double 変換|○ 正規表現|あり :71ほか|
|25|BE/features/operation/book/service/ExcelBookDataSourceRowQueryService|台帳データの取得|catalog のテーブル|△ 設定依存|△ 設定依存|× 既定の WHERE に対象月なし|○|○ 許可リストと危険語チェック|あり（:tenantId 必須）:256|

## 3. 業務正確性の指摘
| 重要度 | ファイル:行 | 問題 | 影響する業務 | 推奨 |
|---|---|---|---|---|
|高（確認済み）|BE/features/operation/monthly/service/CustomerBillingSummaryService.java:64-73,:80-88|`billing_unit` が NULL の行は CASE の ELSE 0 で基本料金が0円になる（残業・深夜・休日・交通費は加算される）。さらに `billing_unit NOT IN (...)` は NULL に対して真にならないため、not_ready_count にも数えられない。|顧客締め：請求漏れのまま「準備完了」になる|`billing_unit IS NULL OR billing_unit NOT IN (...)` を未整備に数える|
|高（確認済み）|同:41-49|単価（`billing_*_unit_price`）が NULL でも COALESCE で0円になり、未整備扱いにならない|請求額の過少|時間・数量が0より大きいのに単価が NULL の行を not_ready に加える|
|中|同:35-91 と BE/features/operation/reportpreview/service/OperationReportPreviewRowReaderService.java:173-179|締め画面は daily_report から直接計算し成分ごとに CEILING。請求書は View の line_amount を合算。税は両方10% HALF_UP。計算経路が二重。|締め画面と請求書の金額がずれる|計算を View に一本化するか、照合テストを用意する|
|中|BE/features/operation/monthly/service/CustomerBillingSummaryService.java:66|DAILY 単位の計算が `work_hours/8*単価`。work_hours に残業時間が含まれる場合、残業分が二重に請求される可能性がある|請求金額|work_hours の定義（残業を含むか）を確認|
|中|同:113-116|`Collectors.toMap` はキー重複で例外になる。再締めで顧客・月ごとに有効な closing 行が複数あると、締め画面が開けなくなる|月次の顧客締め|一意制約を確認するか、マージ関数を指定する（monthly-closing.md §5 では UK (tenant, month, customer) と記載）|
|中|BE/features/dailyreport/service/DailyStatutoryDeductionEstimateService.java:134-159|profile、resident_tax_monthly、period を条件だけで LEFT JOIN しており、ORDER BY も一意保証もない。複数行あると `resultSet.next()` で任意の1行を採る|日次の法定準備金概算|一意キーの保証、または最新行を選ぶサブクエリにする|
|中|同:190-203|`insurance_rate_master` に deleted_at の除外がなく、ORDER BY id と putIfAbsent で「最も古い行」を採る。都道府県（協会けんぽ）の区別もない|保険料の概算|有効行・最新行を採るようにし、地域条件を確認|
|中|同:252-254|保険料・雇用保険を HALF_UP で丸めている。法定の給与控除は「50銭以下切捨て」|概算控除の±1円誤差。月次給与明細と一致するか不明|月次給与明細のロジックと統一|
|中|同:211-218|税表を `BETWEEN min AND max`（両端含む）で引いている。max が NULL の最上位区分は一致せず0円になる。扶養人数が表の範囲外でも0円。deleted_at の除外もない|所得税の概算|「以上・未満」の判定、最上位区分と範囲外扶養数の扱いを決める|
|中|同:121-127|profile がないと社会保険・雇用保険・所得税のフラグが既定で TRUE になる|未設定の従業員にも雇用保険が控除される|profile 必須にするか、既定値を見直す|
|中|BE/features/system/notice/service/resolver/DateColumnNoticeTargetResolver.java:48-59|日付が NOT NULL というだけで全件を取得し、1001件目で例外になる|データ増加で通知が全停止する|対象期間（today±N 日）の条件を付ける|
|中|BE/features/operation/book/service/ExcelBookDataSourceRowQueryService.java:251-257|WHERE テンプレートが未設定のときの既定条件が `tenant_id = :tenantId` か `1 = 1` で、targetMonth を使わない|台帳に全期間のデータが混入。maxRows 超過で例外|テンプレートを必須にするか、既定条件に対象月を含める|
|低|BE/features/system/notice/service/resolver/DayRuleNoticeTargetResolver.java:70|対象月を `YearMonth.now()` に固定|月末に翌月初の期日を通知できない|翌月分も評価する|
|低|BE/features/dailyreport/repository/DailyReportDeductionRepository.java:20-30 / DailyReportAllowanceRepository.java:20-30|承認状態を条件にしていない（DRAFT・差戻しも合算）。明細側の削除列も見ていない|上限チェックや残高。月次明細（APPROVED のみ）と基準が違う。V1 では日報は常に APPROVED で保存されるため現状の実害は小（daily-report.md §2 Mapper）|上限チェック目的なら意図どおりか確認|
|低|BE/features/master/payrollitem/transaction/EmployeePayrollItemTransactionRepository.java:41|合計は transaction_date の期間で取る。一方、一覧（:17）は target_month 基準|月をまたぐ取引の帰属月がずれる|集計基準を統一|
|低|BE/features/operation/monthly/service/LegalDepositRefundService.java:50-69|refund_amount が0円や NULL の行もそのまま ACTIVE で登録。旧行を SUPERSEDED にする範囲は monthlyClosingId 単位|再締めで closing ID が変わると旧 ACTIVE 行が残り、二重返金になりうる（自社締めは同じ monthly_closings 行を再利用するため、通常は ID 不変と推定）|0円を除外。target_month 単位で置き換えるか確認|
|低|BE/features/operation/monthly/service/CustomerBillingSummaryService.java:187|`!today.isBefore(endDate)` のため締め日当日から締め可能|当日分の日報が未確定のまま締まる|`isAfter` にするか、仕様を確認|
|低|BE/features/operation/reportpreview/service/OperationReportPreviewRowReaderService.java:94,:112|`date_format(列,'%Y-%m')`。列が 'YYYY-MM' 形式の文字列だと NULL になり0件。インデックスも効かない|月次帳票が空になる可能性|列の型を確認し、範囲条件に変更|
|低|同:197-198|Number を double 経由で BigDecimal にしている|請求小計に浮動小数の誤差|`new BigDecimal(number.toString())` にする|
|低|BE/features/system/rule/service/fetcher/GeneralDataFetcher.java:84,:93|singleRow でも LIMIT 2 で取得し、2行あっても検出しない。ORDER BY もない|ルール判定が不定|2行なら例外にし、ORDER BY を付ける|
|低|BE/features/system/mail/repository/MailRecipientGroupRepository.java:13-14|削除済みを含む LIMIT 1 に ORDER BY がない|同じキーが複数あると、外部同期で誤った行を復活させうる|group_key 全体の一意性を確認|

## 4. SQL安全性（動的SQL・インジェクション）の指摘
- **×** BE/batch/writer/GenericImportWriter.java:39-44: `tableName` と `columnNames` を検証なしで連結している。生成元（Job 設定）が信頼できるかを要確認。正規表現チェックか catalog 照合を追加すべき。
- **△** BE/features/system/backup/service/fetcher/BackupDataFetcher.java:22、BE/features/system/report/service/api/ReportSqlExecutorService.java:22（更新系）、BE/features/system/report/service/api/OutputTableQueryService.java:54-60: SQL 文字列をそのまま実行する。SQL が管理者設定や固定ファイルに限られ、利用者入力が入らないことが前提。
- **△** BE/features/system/rule/service/fetcher/GeneralDataFetcher.java:49-52: catalog の WHERE テンプレートを未検証で連結している（ExcelBook にある危険語チェックがない）。
- **△** DateColumn/DayRuleNoticeTargetResolver の :52-56 / :61-66: WHERE 句の検証は NoticeSqlSafety に委ねている（未確認）。識別子は正規表現で検証済み。
- **△** BE/features/admin/business/service/PreviewReportSettingService.java:165-190: 保存時は information_schema で存在を確認するだけ。スキーマ内の任意のテーブル（users など）を帳票の元データに指定でき、情報漏えいのおそれがある。orderBy の形式も保存時は未検証（実行時のみ RowReader :287 で検証）。また存在確認は trim 前の tableName、保存は trim 後で食い違う（:58 と :126）。
- **△** RowReader :33 の SAFE_ORDER_BY は英数字と空白を許すため、`id limit 1` のような句を注入できる（行の切り詰めなど）。管理者設定に限られるので低リスク。
- **○** ProcedureExecutorService :17,:40、SnapshotMapQueryService :49、DynamicTableInsertService :23（列は information_schema の許可リスト）、ExcelBookDataSourceRowQueryService :26-36,:259-285（許可リスト・危険語・パラメータ制限）、ImportWriteExecutor（catalog の findRequired。実際の組立は ImportSqlBuilder で未確認）。

## 5. 参照している外部SQLファイル・プロシージャ一覧
- View: `vw_monthly_invoice_latest_detail`、`vw_monthly_pay_slip_legal_deposit_refund`、`vw_daily_labor_cost_preview`、`vw_daily_payment_preparation_preview`
- 帳票コード（operation_report_preview）: `DAILY_LABOR_COST_PREVIEW`、`DAILY_PAYMENT_PREPARATION`、`MONTHLY_INVOICE`。いずれも table_name は設定から動的に決まる。
- 動的プロシージャ: ProcedureExecutorService の `CALL <procedureName>(:executionId)`。名前は呼出元が決める。
- 動的SQLの組立・検証クラス（未開封）: `ImportSqlBuilder`、`NoticeSqlSafety`、`OutputTableValidator`、`RuleDataSourceCatalogService`、`ImportTargetCatalogService`
- 設定に格納されたSQL・テーブル名: rule_data_source_catalog の whereClauseTemplate と physicalName、excel book catalog の whereClauseTemplate と physicalName、notice_rule の whereClause と targetTableName、import_target_catalog、Backup・Report 用の SQL 定義（呼出元）

## 6. tenant_id 条件の一覧（参考・低優先）
- あり: #2・#3 :24-25、#6 :35、#12 :26、#14 :28、#15 :49、#16 :58、#19 :55、#23 :157（JOIN 側も tenant 連動）、#24 :71/:91/:110/:132/:153、#20・#25（catalog が tenantScoped なら `:tenantId` 必須）、#18（tenantScoped 時のみ）
- なし: #4 :71、#5 :13、#10、#11 :28、#13 :34、#17、#23 の率表 :193 と税表 :213（共通マスタなら不要）
- 固定値: #21 :85 `tenant_id = 'default'`
- 不要: #1、#9

## 7. 未解決の疑問・要確認事項
1. `vw_monthly_invoice_latest_detail` は deleted_at IS NULL と approval_status='APPROVED' で絞っているか。締め画面の金額（#19）と同じ CEILING・単位ロジックか。
2. `vw_monthly_pay_slip_legal_deposit_refund` は給与明細と同じ条件（日報の deleted_at IS NULL かつ APPROVED）か。返金0円の行を含むか。
3. 日報・手当控除の数量合計（#2・#3）が DRAFT を含むのは、上限チェックとして意図どおりか。
4. 日報の work_hours に残業・深夜・休日の時間が含まれるか（DAILY・HOURLY の二重請求の判定に必要）。
5. customer_billing_closing は（customer_id, target_month, 未削除）で一意か。再締め時に monthlyClosingId は変わるか（#14）。
6. 月次給与明細の保険料の端数処理と、税表の境界（max_salary が「含む」か「未満」か）。#23 の概算と一致させるべきか。
7. insurance_rate_master に地域（都道府県）・論理削除・改定履歴の列があるか。
8. GenericImportWriter の tableName と columnNames の生成元（Job 定義が固定か、利用者が設定できるか）。
9. Backup・ReportSql・OutputTable.query に渡す SQL の出所（固定ファイルか管理画面か）と、管理画面の権限。
10. operation_report_preview の target_month 列の型（DATE か VARCHAR か）。date_format が効くかに関わる。
11. NoticeSqlSafety と ImportSqlBuilder が論理削除の条件を自動で付けるか（通知・取込の UPDATE で削除済み行が対象になるか）。
