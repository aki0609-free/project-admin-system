# Rule基盤 V1棚卸し・安定化方針

## 1. 文書の目的

ProjectAdminSystem V1におけるRule基盤、手当・控除、日報保存の現行接続を整理する。

本書では、将来構想と現在動作している実装を混同しないよう、次の3種類に分ける。

- 現行実装：現在のコードとローカルDBで確認できたもの
- V1対応：リリースまでに安定化するもの
- V2候補：V1では実装せず、将来拡張として残すもの

調査日：2026-08-22

## 2. 参照した設計資料

GitHubの次のブランチにあるRule詳細設計を参照した。

- `agent/v1-common-architecture-spec`
- `agent/v1-common-architecture-spec-p0-plan`

設計資料の基準コードはmainの`12c91a7`であり、現在の`refactor/v1-domain-stabilization`より古い。

そのため、資料の指摘をそのまま実装せず、現在のコードとDBを再確認して採否を決める。

## 3. ローカルDBのRule棚卸し結果

### 3.1 登録Rule

| Rule名 | 種別 | DSL | 用途 |
| --- | --- | --- | --- |
| DAILY_NORMAL_PAY | PAYROLL | JEXL | 日次通常給与 |
| DAILY_OVERTIME_PAY | PAYROLL | JEXL | 日次時間外手当 |
| DAILY_NIGHT_PAY | PAYROLL | JEXL | 日次深夜手当 |
| DAILY_HOLIDAY_PAY | PAYROLL | JEXL | 日次休日手当 |
| DORMITORY_DAILY_FEE | DEDUCTION | JEXL | 寮費日次徴収 |

現在のローカルDBでは、登録されている5件はすべてJEXLである。

- MVEL Rule：0件
- JAVA_BEAN Rule：0件
- Rule DataSource：0件
- Rule DataSource Catalog：0件
- Rule DataSource Catalog Column：0件

V1では既存のMVEL/JAVA_BEAN実行コードは互換性のため維持するが、新規運用はJEXLを標準とする。

### 3.2 日次給与Rule設定

| 給与構成要素 | Rule名 |
| --- | --- |
| NORMAL_PAY | DAILY_NORMAL_PAY |
| OVERTIME_PAY | DAILY_OVERTIME_PAY |
| NIGHT_PAY | DAILY_NIGHT_PAY |
| HOLIDAY_PAY | DAILY_HOLIDAY_PAY |

`daily_pay_rule_setting`が給与構成要素とRuleを関連付ける。

### 3.3 Ruleに直接関連付く手当・控除

現在、手当・控除マスターでRule名が設定されているのは寮費だけである。

| 種別 | コード | 計算区分 | Rule名 | 単位 | 手動変更 |
| --- | --- | --- | --- | --- | --- |
| 控除 | DORMITORY_FEE | AUTO | DORMITORY_DAILY_FEE | BOTH | 許可 |

法定控除マスターにはAUTOが設定されているが、Rule名は設定されていない。月次給与は現在、Rule基盤から直接Payslipへ保存する方式ではなく、月次View・ストアド・historyTableを利用するためである。

`PayrollItemMonthlyInputService`を本番の月次確定処理へ接続する場合は、法定控除AUTOとRule名未設定の扱いを先に決める必要がある。

## 4. 従業員別適用ポリシー

| コード | 適用対象 | 入力元 | 残高管理 |
| --- | --- | --- | --- |
| DORMITORY_FEE | EMPLOYEE_ENROLLMENT | DAILY_REPORT | あり |
| MOBILE_RENTAL | EMPLOYEE_ENROLLMENT | TRANSACTION | なし |
| WIFI_FEE | EMPLOYEE_ENROLLMENT | TRANSACTION | なし |

`EMPLOYEE_ENROLLMENT`は従業員テーブルへ物理カラムを追加する指定ではない。

以下の共通基盤を通して、対象従業員とパラメータを管理する。

```text
PayrollItemBalancePolicy
  ↓
PayrollItemParameterDefinition
  ↓
EmployeePayrollItemSettingService
  ↓
従業員別の有効/無効・パラメータ
```

## 5. 日報Rule計算から保存までの現行フロー

```text
DailyReportCommandService
  ↓
DailyReportInputItemService
  ├─ 従業員・契約・対象日を取得
  ├─ 従業員別適用対象を判定
  ├─ 項目固有Ruleパラメータを解決
  └─ 手動変更額・数量を抽出
  ↓
PayrollItemDailyInputService
  ↓
PayrollItemCalculationService
  ↓
PayrollItemValueService
  ├─ MANUAL
  ├─ FIXED
  └─ AUTO → RuleExecutionService
  ↓
DailyReportAllowance / DailyReportDeduction
  ├─ calculatedAmount
  ├─ amount
  ├─ manualOverrideFlag
  ├─ overrideReason
  ├─ quantity
  └─ balanceUnit
```

日報については、Rule計算結果からDB保存まで接続済みである。

## 6. 手動変更の契約

AUTO/FIXEDの計算結果を画面で変更する場合は、次を保存する。

- `calculatedAmount`：Ruleまたは固定値による計算額
- `amount`：実際に適用した金額
- `manualOverrideFlag`：手動変更の有無
- `overrideReason`：変更理由

`manualOverrideFlag=true`の場合、変更理由は必須で500文字以内とする。

2026-08-22の確認で、手当だけは`manualOverride=false`でも画面から返された金額を手動変更額として渡していた。控除と同じ契約へ修正し、明示的な手動変更の場合だけ上書き額を渡すよう統一した。

手当・控除とも、同一リクエスト内のマスターID重複は手動変更の有無にかかわらず拒否する。

## 7. 月次確定との境界

V1の月次確定は以下を正とする。

```text
最新業務データ
  ↓
月次集計View
  ↓
ストアド
  ├─ outputTable
  └─ historyTable
  ↓
帳票生成・履歴表示・バックアップ
```

`historyTable`が締め時点の確定データである。

そのため、V1ではRule実行履歴をPayslipItemへ全面保存する新しいSnapshot基盤を追加しない。

再締め時は最新Viewから再取得し、historyTableのVersionを更新する既存方針を維持する。

## 8. 2026-08-22のテスト結果

### 8.1 単体テスト

Rule・給与項目・日報入力に関する19件が成功した。

主な保証範囲：

- JEXL基本計算
- JEXL strict mode
- Ruleパラメータ型変換
- required/defaultパラメータ
- Rule名変更禁止
- 参照中Rule削除禁止
- unsafe DSL/SQLの基本拒否
- TenantContextによるtenant値強制
- AUTO手当の明示的手動変更
- 明示されていない手動変更をRule計算へ渡さない
- 手当・控除マスターID重複拒否
- 従業員別Ruleパラメータと数量の受け渡し

### 8.2 Testcontainers統合テスト

次の4件が成功した。

1. Rule計算額と手動変更額・理由の日報保存
2. 寮費・携帯料金の数量計算、残数量、手動変更
3. 無効化した従業員別控除を日報プレビューから除外
4. 従業員・日報給与・住民税・月次集計の整合性

## 9. V1で実施する安定化

優先順：

1. Rule・日報・月次Viewの統合テスト拡充
2. Tenantの信頼元を認証情報へ統一
3. Rule DataSourceをCatalog必須へ移行（2026-08-22対応済み）
4. Column Mappingなしの`SELECT *`禁止（2026-08-22対応済み）
5. 金額をBigDecimalで統一し、丸め境界をテスト（2026-08-22対応済み）
6. 過去計算でClockではなくtargetDateを使用（2026-08-22対応済み）
7. Ruleエラーから入力値・Fact全量を露出しない（2026-08-22対応済み）
8. 必須Ruleとマスターデータを整備し、Javaフォールバックを廃止（2026-08-22対応済み）

### 9.1 Rule DataSourceのFail Closed化

Rule DataSourceは次の契約へ統一した。

```text
Rule DataSource
  ├─ catalogCode：必須
  ├─ sourceName：必須
  └─ Column Mapping：1件以上必須
```

物理テーブル名とWHERE条件は、Rule編集リクエストに含まれる値を使用しない。サーバーが有効なCatalogから解決する。

実行SQLは次の形だけを許可する。

```text
Catalog.physicalName
  + Catalog.whereClauseTemplate
  + Catalogで許可されたColumn Mapping
```

次は登録時と実行時の両方で拒否する。

- Catalogなしの旧式Data Source
- Column Mappingが0件のData Source
- Catalogで無効化された、または未登録のカラム
- Tenant対象Catalogで`:tenantId`条件がないもの
- TenantContextが確定していないTenant対象Catalog

`SELECT *`へのフォールバックは廃止した。既存DBに旧式Data Sourceがある場合は、対応Catalogと列Mappingを登録してから使用する。

2026-08-22時点のローカルDBではRule Data Source自体が0件のため、既存5 Ruleの動作移行は発生しない。

追加したFail Closedテスト：

1. CatalogなしData Sourceを登録時に拒否
2. Column MappingなしData Sourceを登録時に拒否
3. CatalogなしData Sourceを実行時に拒否
4. Column MappingなしData Sourceを実行時に拒否
5. Ruleパラメータで渡された偽のtenantIdを無視し、TenantContextの値を強制

### 9.2 給与金額の型と丸め境界

V1では新しい金額Registryは導入せず、`PayrollMoneyPolicy`へ次の責務だけを集約した。

- Rule結果を`BigDecimal`へ変換
- 整数型を`double`へ変換せず、桁を維持
- `NaN`・無限値・数値でない文字列を拒否
- 給与構成額の最終結果を1円単位で`HALF_UP`（四捨五入）

```text
Ruleの計算途中
  BigDecimalの小数精度を維持
      ↓
通常給・時間外・深夜・休日・手当・控除の各構成額
      ↓
PayrollMoneyPolicy.roundToYen
      ↓
日報・給与項目へ円単位で保存
```

丸めはRule式の途中や時給換算途中には行わず、各構成額の確定境界で一度だけ行う。

Fuyo V1の確定仕様は次のとおり。

| 対象 | 型・丸め |
| --- | --- |
| 時間・率・基礎時給の計算途中 | `BigDecimal`で精度を維持 |
| Rule結果 | `BigDecimal`として受け取る |
| 給与構成額 | 1円単位、`HALF_UP` |
| 月次集計 | 日報へ保存済みの円単位構成額を合計 |

境界テストでは`100.49 → 100`、`100.50 → 101`、日報Rule結果`1000.50 → 1001`を保証する。

### 9.3 過去計算の基準日

V1では、業務計算の基準日をサーバーの現在日ではなく、処理対象の`targetDate`へ統一する。

```text
日報入力
  workDate
     ↓
Rule context.targetDate
  ├─ 対象日時点の従業員別手当・控除設定
  ├─ 対象日時点の残高
  └─ 対象年・年度の税率／税額表
```

控除詳細APIは、次の任意クエリを受け取る。

```http
GET /api/master/deductions/{id}?targetDate=2026-05-31
```

管理画面には「参照基準日」を設け、その日付に対応する次のデータを表示する。

- 所得税：基準日の暦年
- 健康保険・雇用保険：基準日の暦年
- 住民税：6月から翌年5月までを同一年度として参照

`targetDate`を省略した画面・既存API呼出しに限り、注入された`Clock`の現在日を既定値とする。Rule実行や日報計算で現在日へ暗黙フォールバックしない。

従業員別手当・控除設定は`effectiveFrom`・`effectiveTo`で版管理する。

- 設定開始：指定日を`effectiveFrom`として保存
- 設定変更：旧版を指定日の前日で終了し、指定日から新版を保存
- 設定解除：指定日の前日を旧版の`effectiveTo`にする
- 適用開始日と同日に解除：不正な期間を作らず、その版を論理削除

これにより、後日設定値を変更しても過去の日報再計算は当時有効だった設定を参照できる。

V1の既知制約：標準報酬月額テーブル自体には年度・適用期間がないため、詳細表示は基準日で切り替わらない。年度別管理が必要になった時点で税マスター側へ適用期間を追加する。寮費基準マスター等も同様に、マスター値そのものの期間管理は別途仕様確定後に行う。日報・締め履歴へ保存済みの計算結果は確定時点の値として維持する。

### 9.4 Rule実行エラーの秘匿化

Rule実行は、次の4段階で失敗箇所を識別する。

1. `RULE_LOADING`
2. `PARAMETER_RESOLUTION`
3. `FACT_BUILDING`
4. `DSL_EXECUTION`

失敗時は元例外をそのまま外部へ返さず、`RuleExecutionException`へ変換する。元例外は入力値、従業員情報、SQLパラメータ、Fact、DSL断片を含む可能性があるため、causeとしても保持しない。

SYS_ADMINのRuleテスト実行APIは次を返す。

```text
HTTP 422
code    = RULE_EXECUTION_FAILED
message = Ruleの実行に失敗しました。追跡IDを管理者へ連絡してください。
traceId = リクエスト追跡ID
```

通常ログへ記録する項目は次だけとする。

- 安全な形式と確認できた`ruleName`
- 実行失敗段階
- 例外クラス名
- 共通リクエストログの`traceId`

ログへ記録しない項目：

- パラメータ名に対応する入力値
- Factとデータソース取得行
- SQLとバインド値
- DSL全文・エンジンの元例外メッセージ
- メールアドレス、口座情報、給与額等の業務データ

外部から指定された`ruleName`が識別子形式に合わない場合、ログと例外内部では`<invalid>`へ置換する。型変換と識別子検証のエラー文からも拒否値そのものを除外した。

成功時のFact表示は、`SYS_ADMIN`限定の明示的なRuleテスト実行機能としてV1では維持する。通常ユーザー向けAPIや失敗レスポンスでは返さない。

## 10. V1で変更しないもの

- MVEL/JAVA_BEAN実行コードの削除
- RuleRevisionの新規DBモデル
- 汎用RuleExecutionSnapshotテーブル
- RuleFunctionsライブラリの全面導入
- 構造化SQL Builder
- Ruleパッケージ全体の再構築

これらはV2候補とする。

## 11. 追加確定事項

### 11.1 日報給与Ruleの必須化（2026-08-22対応済み）

日次給与本体の金額をJavaで代替計算するフォールバックを廃止した。次の4つの有効なRule設定を必須とする。

- `NORMAL_PAY` → `DAILY_NORMAL_PAY`
- `OVERTIME_PAY` → `DAILY_OVERTIME_PAY`
- `NIGHT_PAY` → `DAILY_NIGHT_PAY`
- `HOLIDAY_PAY` → `DAILY_HOLIDAY_PAY`

設定が1件でも不足、空欄または無効の場合、いずれの給与Ruleも実行せず設定エラーとして日報計算を中止する。これにより、環境ごとに異なるJavaの暗黙計算で給与額が確定することを防止する。

法定割増率と時間区分はRuleへ渡す初期パラメータとして維持する。金額の最終決定は必ずRuleが行うため、これらは金額フォールバックではない。

4つの初期Rule、パラメータ、`daily_pay_rule_setting`は`pay_component_rule_foundation_v1.sql`で新規環境へ配置する。Testcontainersでは、ランタイム資産一覧から同SQLを適用し、実際のRule実行結果が通常8,000円、時間外2,500円、深夜250円、休日0円になることまで確認する。

### 11.2 負数の手動変更（2026-08-22対応済み）

V1では、手当・控除の金額は計算方法やマスターの下限設定にかかわらず0円以上とする。

- `MANUAL`の手入力額：0円以上
- `AUTO/FIXED`の手動変更額：0円以上
- Rule・固定値から得た計算額：0円以上
- 日報保存リクエストの金額・計算額：0円以上

負数を下限金額の0円へ暗黙補正しない。負数が入力された場合は計算・保存を中止して入力エラーを返す。保存前に全行を検証するため、不正な1行によって既存の日報手当・控除が先に削除されることもない。

返金、減額調整、相殺は控除金額を負数にせず、次のいずれかで正の金額として表現する。

1. 専用の返金手当（例：法定預り返金）
2. 用途を明示した調整手当
3. 元取引の取消・訂正処理

これにより、支給合計は手当の正額、控除合計は控除の正額という帳票・月次Viewの前提を維持する。

### 11.3 月次PayrollItemの計算責務（2026-08-22対応済み）

V1の月次給与確定はJavaで手当・控除を再計算せず、次の経路へ一本化する。

```text
日報へ保存済みの給与・手当・控除
月次明細型取引（CONFIRMEDのみ）
税・保険・従業員・会社マスター
          ↓
vw_monthly_pay_slip_latest
          ↓ INITIAL / RECLOSE
sp_monthly_pay_slip_snapshot
          ↓
monthly_pay_slip_history / history_item
          ↓
monthly_pay_slip_render_output / output_item
          ↓
JasperReports・プレビュー・メール
```

未使用だった`PayrollItemMonthlyInputService`は、Viewと別にJavaで月次金額を計算できてしまうため削除した。`PayrollItemCalculationService`とRule計算は日報入力時の金額確定に使用し、月次締めからは呼び出さない。

実行モードごとの責務は次のとおり。

| 実行モード | 参照元 | 動作 |
|---|---|---|
| `INITIAL` | 最新View | 締めバージョン1の履歴を新規作成 |
| `RECLOSE` | 最新View | 修正後の最新値から新しい締めバージョンを作成 |
| `RETRY` | 既存history | Viewを再計算せず、同じ確定履歴から描画用outputを再作成 |

Testcontainersでは、初回締め後に住民税の最新値を変更し、次を保証する。

1. 最新Viewは変更後の13,000円を返す
2. `RETRY`は締めバージョン1の12,000円を維持する
3. `RECLOSE`は締めバージョン2へ13,000円を保存する

これにより「Viewは常に最新」「historyは締め時点の確定値」「再試行は確定値を変えない」「再締めだけが新しい版を作る」という仕様を固定する。
