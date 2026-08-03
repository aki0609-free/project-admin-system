# 月間労務表 Spreadsheet台帳 V1

## 1. 目的

月間労務表を、対象月・従業員単位のSpreadsheet台帳として生成する。

- 1従業員につき1JSONファイル・1シート
- A3横・1従業員1ページを想定
- 個別選択、複数選択、全員選択に対応
- 締め前は再生成可能
- 締め後は確定した台帳を参照・印刷
- 生成ファイルはS3の生成帳票領域へ保存
- 原本の給与・控除・寮費等の3ページを、A3横1ページの統合表として配置
- 会社名は会社プロフィール、従業員番号・氏名は従業員マスターから表示

## 2. 処理フロー

1. 対象月を選択する。
2. `MONTHLY_LABOR_LEDGER`から従業員一覧を取得する。
3. 従業員を1名以上選択する。全件選択も可能。
4. 従業員ごとに`vw_monthly_labor_ledger`を検索する。
5. `MONTHLY_LABOR_V1` Rendererが1か月分を1シートへ配置する。
6. 従業員別JSONをS3へ保存する。
7. 画面では、複数の従業員シートを1Workbookにまとめて確認・印刷する。

## 3. S3保存先

```text
documents/generated-reports/
  ledgers/{tenantId}/MONTHLY_LABOR/{yyyy-MM}/
    selections/{employeeId}/
      MONTHLY_LABOR-{yyyy-MM}.json
```

同じ対象月・従業員を締め前に再生成した場合は、同じキーを更新する。

## 4. 日報給与のRule計算

給与本体は手当として扱わず、次の4区分を日報へ個別保存する。

| component_type | 保存先 | 用途 |
|---|---|---|
| `NORMAL_PAY` | `daily_report.normal_pay_amount` | 通常給金 |
| `OVERTIME_PAY` | `daily_report.overtime_pay_amount` | 早出・残業 |
| `NIGHT_PAY` | `daily_report.night_pay_amount` | 深夜 |
| `HOLIDAY_PAY` | `daily_report.holiday_pay_amount` | 法定休日 |

`daily_pay_rule_setting`で各区分とRule名を対応付ける。計算式本体は既存のRule管理機能で管理する。

Ruleへ渡す主なパラメータ：

- `employeeId`
- `targetDate`、`workDate`、`paymentDate`
- `workHours`、`overtimeHours`、`nightWorkHours`、`holidayWorkHours`
- `customerId`、`customerSiteId`
- `jobCode`、`siteRoleCode`
- `mileage`
- `salaryType`
- `hourlyWage`、`dailyWage`、`weeklyWage`、`monthlySalary`
- `standardWorkingHours`
- `calculationHourlyRate`
- `componentType`

対応するRule設定がない区分は、既存の給与区分による計算へフォールバックする。月給者の通常給日次配賦は、`月給 × 12 ÷（週所定労働時間 × 52）× 通常時間`で計算する。月次給与の基本給そのものは契約月給を使用し、日次配賦額の合計で置き換えない。

土日だけを理由に休日労働へ強制変換しない。会社カレンダー導入までは休日時間を明示入力し、導入後は法定休日・所定休日区分に従って自動判定する。

法定休日は残業へ合算せず、`HOLIDAY_PAY`として独立保存する。深夜と重複した場合も、休日分と深夜加算分を別々に確認できる。

## 5. 控除・支払項目

- 前払い：`daily_payments.actual_amount`
- 当日支給額：日報保存時点の`estimated_net_pay_amount`
- 税額他：控除マスターの`deduction_type = LEGAL`
- 寮費：日報控除明細の`DORMITORY_FEE`
- 積立：`daily_report.saving_amount`
- 返済：`daily_report.loan_repayment_amount`
- 車両代：日報手当明細の`DRIVER_ALLOWANCE`

寮費は従業員から日次徴収するため、日報で日別金額を保存する。会社側の月次支払いではその月の合計を使用する。

## 6. 前月繰越

V1では使用しない。月間労務表の生成・締め処理に必須ではないため、ViewおよびRendererへ項目を設けない。

## 7. DB適用資産

```text
backend/src/main/resources/sql/daily_report/pay_component_rule_foundation_v1.sql
backend/src/main/resources/sql/system/excelbook/monthly_labor_v1.sql
```

適用スクリプト：

```text
infrastructure/scripts/database/apply_runtime_schema_upgrade.sh
```

## 8. Rendererの変更方針

月間労務表固有のセル配置は`MonthlyLaborSpreadsheetRenderer`だけに置く。共通生成サービスへ帳票コードの条件分岐を追加しない。

大きなレイアウト変更は、既存の`MONTHLY_LABOR_V1`を直接破壊せず、`MONTHLY_LABOR_V2`を追加してマスターの`renderer_key`を切り替える。

## 9. 本番適用前の確認事項

- 通常給金・早出残業・深夜・法定休日のRuleを作成する。
- `daily_pay_rule_setting`へRule対応を登録する。
- 寮費Ruleまたは日報手入力方法を確定する。
- 代表的な従業員1名で日別金額を照合する。
- A3横のブラウザ印刷で1ページに収まることを確認する。
- 全員印刷時のブラウザ負荷と印刷順を確認する。
