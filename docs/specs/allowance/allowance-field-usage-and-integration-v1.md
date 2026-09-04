# 手当マスター 入力項目の利用先・システム連携 V1

## 1. 手当本体

保存先：`allowance_masters`

| 画面項目 | API / DB | 状態 | 実際の用途 |
|---|---|---|---|
| 手当コード | `allowanceCode` / `allowance_code` | 利用中 | 一意識別、Rule facts、日報・帳票項目コード、変更不可 |
| 手当名 | `allowanceName` / `allowance_name` | 利用中 | 日報、従業員設定、月次明細の表示 |
| 手当種別 | `allowanceType` / `allowance_type` | 管理分類 | 一覧分類。現行計算の分岐では未参照 |
| 計算区分 | `calculationType` / `calculation_type` | 利用中 | MANUAL/FIXED/AUTOの経路選択 |
| 手当単位 | `allowanceUnit` / `allowance_unit` | 条件付き | DAILY/BOTHを日報候補抽出へ使用。汎用PAYROLL計算は未接続 |
| 詳細参照タイプ | `detailViewType` / `detail_view_type` | 予約項目 | 現在はNONEだけで、詳細providerなし |
| Rule名 | `ruleName` / `rule_name` | 利用中 | AUTO時にALLOWANCEまたはGENERAL Ruleを実行 |
| 既定額 | `defaultAmount` / `default_amount` | 利用中 | FIXED時の金額 |
| 手入力許可 | `allowManualInput` / `allow_manual_input` | 利用中 | MANUAL必須。AUTO/FIXEDの変更可否 |
| 最小・最大額 | `minAmount/maxAmount` | 利用中 | 計算・変更結果の上下限 |
| 課税対象 | `taxable` / `taxable` | 未連携 | 保存・表示されるが、現行月次給与Viewは手当別の課税除外に使用しない |
| 日次明細表示 | `showOnDailyStatement` | 利用中 | 日報候補抽出 |
| 旧月次明細表示 | `showOnMonthlyStatement` | 互換のみ | 管理画面・MONTHLY providerから除外。月次表示用Viewを正とする |
| 表示順 | `displayOrder` | 利用中 | 一覧、日報、月次明細の順序 |
| 有効 | `enabled` | 利用中 | 候補・従業員設定・取引登録可否 |
| 備考 | `note` | 保存・表示のみ | 管理メモ |

## 2. 計算

```text
MANUAL -> 入力金額
FIXED  -> defaultAmount
AUTO   -> Ruleへ共通parameterと従業員別parameterを渡す
  -> min/max補正
  -> 円単位HALF_UP
  -> allowManualInput=trueなら理由付き変更可能
```

`allowanceType`や`taxable`は、この共通計算snapshotには含まれない。

## 3. 適用Policy

控除と同じ`payroll_item_balance_policy`を使う。

| 設定 | 用途 |
|---|---|
| `ALL_EMPLOYEES` | 全従業員固定の勤務態度手当等。個別設定画面には出ない |
| `EMPLOYEE_ENROLLMENT` | 従業員ごとに付与する運転・管理手当等 |
| `DAILY_REPORT` | 日報で計算・入力 |
| `TRANSACTION` | 不定期に発生する明細型手当。従業員別適用が必須 |
| 残高追跡 | 支給残額・回数等を数量で追跡する場合に使用 |

残高繰越、残高超過許可、加算頻度には控除と同じ未連携制約がある。

## 4. 動的パラメーター

| 設定 | 用途 |
|---|---|
| key・表示名 | 従業員設定JSON、Rule入力、画面ラベル |
| inputType | TEXT/NUMBER/SELECT/BOOLEAN/DATEの検証 |
| required | Enrollment保存時の必須制御 |
| defaultValue | 従業員設定の初期値 |
| options | SELECT値、表示名、任意の計算値 |
| ruleParameter | Ruleへ渡す値を選択 |
| dailyDisplay | 保存・従業員設定responseへの返却までは実装。現行日報UIでの表示切替は未接続 |
| inputSourceOverride | 従業員別に日報/取引入力を切替 |
| ruleValueResolverKey | SELECTの計算値を別NUMBER parameterへ展開 |
| displayOrder | 従業員設定・日報の表示順 |

## 5. 従業員・日報連携

```mermaid
flowchart LR
    MASTER["手当マスター"]
    POLICY["EMPLOYEE_ENROLLMENT Policy"]
    EMP["従業員別適用・parameter"]
    DAILY["日報手当"]
    VIEW["月次給与View"]
    REPORT["月次給与明細"]

    MASTER --> POLICY --> EMP --> DAILY --> VIEW --> REPORT
```

`ALL_EMPLOYEES`はEnrollmentなしで日報候補になり、`EMPLOYEE_ENROLLMENT`は対象日に有効な適用が必要である。入力元がTRANSACTIONなら日報へは出ず、確定取引を月次で集計する。

## 6. 課税額との関係

現行月次Viewの課税対象額は、総支給額から社会保険等を控除して算出する。`allowance_masters.taxable=false`の手当を総支給から除外するjoin・条件は確認できない。

したがって現在の「課税対象」checkboxは、税計算結果を変えない。非課税手当を扱う場合は月次Viewで手当マスターへjoinし、課税支給額と総支給額を分離する必要がある。

## 7. 月次表示との関係

月次給与は日報・取引の実績を`vw_monthly_pay_slip_calculation_item_source`で計算し、`vw_monthly_pay_slip_statement_item_source`で帳票表示項目を定義する。旧`showOnMonthlyStatement`はDB互換用であり、管理画面と汎用MONTHLY providerから除外した。
