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
3. Rule DataSourceをCatalog必須へ移行
4. Column Mappingなしの`SELECT *`禁止
5. 金額をBigDecimalで統一し、丸め境界をテスト
6. 過去計算でClockではなくtargetDateを使用
7. Ruleエラーから入力値・Fact全量を露出しない
8. 必須Ruleとマスターデータを整備した後、Javaフォールバックを廃止

## 10. V1で変更しないもの

- MVEL/JAVA_BEAN実行コードの削除
- RuleRevisionの新規DBモデル
- 汎用RuleExecutionSnapshotテーブル
- RuleFunctionsライブラリの全面導入
- 構造化SQL Builder
- Ruleパッケージ全体の再構築

これらはV2候補とする。

## 11. 未決事項

### 11.1 Rule未設定時の給与フォールバック

現在、日次給与本体には次のJavaフォールバックがある。

- 週40時間
- 時間外割増1.25
- 60時間超1.50
- 深夜割増0.25
- 休日割増1.35

V1では必須Rule・マスター・テストを準備した後、Rule未設定を設定エラーとして扱う方向で段階的に廃止する。

### 11.2 負数の手動変更

Rule計算結果は負数を拒否するが、手動変更額はマスターの最小値が未設定の場合に負数を取り得る。

調整手当・返金を正の手当として管理するか、負数を許可する項目を設けるか、業務仕様を確定してから変更する。

### 11.3 月次PayrollItemサービス

月次確定の本線はView・ストアド・historyTableである。

`PayrollItemMonthlyInputService`を帳票確定へ直接接続するかは未決定であり、V1では安易に接続しない。
