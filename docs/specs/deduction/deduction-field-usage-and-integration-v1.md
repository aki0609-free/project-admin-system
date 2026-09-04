# 控除マスター 入力項目の利用先・システム連携 V1

## 1. 判定区分

| 区分 | 意味 |
|---|---|
| 利用中 | 現行処理が判定・計算・表示に参照する |
| 条件付き | 特定のPolicy・入力経路でのみ使う |
| 保存・表示のみ | DB保存と管理画面表示はあるが業務計算では未参照 |
| 未連携 | 意図は明確だが現行実装が参照していない |

## 2. 控除本体

保存先：`deduction_masters`

| 画面項目 | API / DB | 区分 | 実際の用途 |
|---|---|---|---|
| 控除コード | `deductionCode` / `deduction_code` | 利用中 | 一意識別、Rule facts、帳票項目コード、作成後変更不可 |
| 控除名 | `deductionName` / `deduction_name` | 利用中 | 日報、従業員設定、月次明細の表示名 |
| 控除種別 | `deductionType` / `deduction_type` | 条件付き | `LEGAL`は月次Viewで法定控除として分類。他区分は主に管理分類 |
| 計算区分 | `calculationType` / `calculation_type` | 利用中 | `MANUAL`、`FIXED`、`AUTO`の計算経路を選択 |
| 控除単位 | `deductionUnit` / `deduction_unit` | 条件付き | `DAILY/BOTH`は日報候補抽出に使用。`MONTHLY/PAYROLL`は現行の汎用月次計算経路が未完成 |
| 詳細参照タイプ | `detailViewType` / `detail_view_type` | 利用中 | 税・保険詳細タブと参照providerを切替 |
| Rule名 | `ruleName` / `rule_name` | 利用中 | `AUTO`時にRule基盤を呼ぶ。DEDUCTIONまたはGENERAL Ruleのみ許可 |
| 既定額 | `defaultAmount` / `default_amount` | 利用中 | `FIXED`時の計算値。Policyパラメーターの既定値とは別 |
| 手入力許可 | `allowManualInput` / `allow_manual_input` | 利用中 | `MANUAL`必須。AUTO/FIXEDの計算結果を日報で変更できるか決定 |
| 最小額 | `minAmount` / `min_amount` | 利用中 | 自動・固定・手動変更後の下限clamp |
| 最大額 | `maxAmount` / `max_amount` | 利用中 | 自動・固定・手動変更後の上限clamp |
| 日次明細表示 | `showOnDailyStatement` / `show_on_daily_statement` | 利用中 | 日報候補抽出条件 |
| 旧月次明細表示 | `showOnMonthlyStatement` / `show_on_monthly_statement` | 互換のみ | 管理画面・MONTHLY providerから除外。表示用Viewを正とする |
| 旧月次精算対象 | `carryToMonthlySettlement` / `carry_to_monthly_settlement` | 互換のみ | 管理画面から除外。計算用Viewを正とする |
| 表示順 | `displayOrder` / `display_order` | 利用中 | 一覧、日報候補、月次明細の並び順 |
| 有効 | `enabled` / `enabled` | 利用中 | 候補、従業員設定、取引登録の可否 |
| 備考 | `note` / `note` | 保存・表示のみ | 管理メモ |

## 3. 計算値の決まり方

```text
MANUAL -> 入力額（未入力なら0）
FIXED  -> defaultAmount
AUTO   -> ruleNameのRuleへ共通parameter + 従業員別parameterを渡す
  -> minAmount / maxAmountで補正
  -> 円単位HALF_UP
  -> allowManualInput=trueなら理由付き手動変更可能
```

Ruleへは少なくとも`targetType`、`targetMasterId`、`targetCode`が追加される。共通parameterと項目固有parameterが同名の場合、項目固有値が優先される。

## 4. 適用・連携Policy

保存先：`payroll_item_balance_policy`

| 項目 | 値 | 区分 | 用途 |
|---|---|---|---|
| 適用対象 | `ALL_EMPLOYEES` | 利用中 | 全従業員へ適用。従業員個別設定画面には出さない |
| 適用対象 | `EMPLOYEE_ENROLLMENT` | 利用中 | 有効な従業員別Enrollmentがある場合だけ適用 |
| 入力元 | `DAILY_REPORT` | 利用中 | 日報の控除項目として入力・計算 |
| 入力元 | `TRANSACTION` | 利用中 | 携帯明細等を汎用控除取引として記録。従業員別適用が必須 |
| 残高追跡 | `balanceTracking` | 利用中 | 残日数・残額等のsnapshot計算を有効化 |
| 残高単位 | `AMOUNT/DAYS/HOURS/COUNT` | 条件付き | 残高の意味・表示単位。内部の増減計算自体は数量で共通 |
| 加算頻度 | `accrualFrequency` | 保存・表示のみ | APIには存在するが、現行残高計算は頻度を参照しない |
| 加算Rule | `accrualRuleName` | 利用中 | `CALENDAR_DAYS_IN_ENROLLMENT`または`MANUAL_TRANSACTION`を残高計算が解釈 |
| 残高繰越 | `carryForward` | 未連携 | 保存されるが残高Queryは参照せず、現状はEnrollment期間全体を通算する |
| 残高超過許可 | `advanceConsumption` | 未連携 | 保存されるが超過可否判定は未実装。残高表示は0未満へ下がらない |

`TRANSACTION`は`EMPLOYEE_ENROLLMENT`以外では保存時に拒否される。

## 5. 動的パラメーター定義

保存先：`payroll_item_parameter_definition`

| 項目 | 区分 | 用途・制約 |
|---|---|---|
| パラメーターキー | 利用中 | 従業員設定JSONとRule parameterのキー。英字開始、英数字・`_`、既存キー変更不可 |
| 表示名 | 利用中 | 従業員設定・日報でのラベル |
| 入力形式 | 利用中 | `TEXT/NUMBER/SELECT/BOOLEAN/DATE`の検証・画面component切替 |
| 必須 | 利用中 | 従業員Enrollment保存時に値必須を検証 |
| 既定値 | 利用中 | 未設定時の従業員設定初期値。控除本体の`defaultAmount`とは別 |
| 選択肢 | 利用中 | SELECTのlabel/valueと任意の`calculationValue` |
| Ruleへ渡す | 利用中 | trueの項目だけをRule parameterへ変換 |
| 日報に表示 | 未連携 | 定義値は保存され従業員設定responseへ返るが、現行日報画面・日報入力Serviceがこのフラグでparameter表示を切り替える参照は確認できない |
| 入力元切替 | 利用中 | SELECT値により`DAILY_REPORT`と`TRANSACTION`を従業員ごとに切替。1項目だけ許可 |
| Rule値resolver | 利用中 | `SELECT_OPTION_CALCULATION_VALUE:<sourceKey>`で選択肢の計算値をNUMBER parameterへ解決 |
| 表示順 | 利用中 | 従業員設定・日報の項目順 |
| 有効 | 利用中 | 削除された定義はsoft deleteし候補から除外 |

### 例：寮費を固有処理なしで構成する場合

```text
roomType: SELECT
  SINGLE -> calculationValue=1000
  SHARED -> calculationValue=700

dailyFee: NUMBER, ruleParameter=true
  resolver=SELECT_OPTION_CALCULATION_VALUE:roomType

paymentMode: SELECT, inputSourceOverride=true
  DAILY -> DAILY_REPORT
  MONTHLY -> TRANSACTION
```

Ruleは`dailyFee`を受け取って金額を計算でき、システム本体へ`DORMITORY_FEE`固有分岐を追加する必要はない。

## 6. 従業員別適用

`EMPLOYEE_ENROLLMENT`の項目だけが、従業員管理の手当・控除設定へ自動表示される。控除マスターの作成だけでは全従業員へ即時追加されない。

```text
控除マスター
  + Policy.applicationScope=EMPLOYEE_ENROLLMENT
  + enabled=true
  -> 従業員画面の設定候補
  -> Enrollmentの有効期間
  -> parameter values
  -> 日報または汎用取引へ接続
```

## 7. 日報・残高・月次

| 処理 | 参照内容 |
|---|---|
| 日報候補 | enabled、日次表示、単位、Policy、Enrollment、入力元 |
| 日報計算 | calculationType、Rule、既定額、上下限、手動変更可否、Rule parameter |
| 残高 | Policy、Enrollment期間、日報消化数量、確定取引数量、加算Rule |
| 月次給与 | 日報控除と確定取引をViewで集計。法定区分は別計算へ分類 |
| 月次帳票 | Viewの確定結果をhistory/output tableへ保存し、給与明細等へ出力 |

月次計算は`vw_monthly_pay_slip_calculation_item_source`、帳票表示は`vw_monthly_pay_slip_statement_item_source`を正とする。旧`carryToMonthlySettlement`と`showOnMonthlyStatement`は既存DB資産移行のためだけに保持する。

## 8. 詳細タブと計算の関係

詳細タブは税率・標準報酬・住民税等の参照・編集を提供するが、`detailViewType`だけで控除額を自動計算するものではない。実際の法定控除計算は月次給与View、税・保険マスター、従業員給与設定により行われる。
