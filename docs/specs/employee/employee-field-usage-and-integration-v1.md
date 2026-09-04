# 従業員管理 入力項目の利用先・システム連携 V1

ドメイン：従業員管理

## 1. 判定区分

| 区分 | 意味 |
|---|---|
| 利用中 | 計算、日報、帳票、別画面等で参照する |
| 画面内利用 | 従業員管理内の表示・検索・検証で使う |
| 保存のみ | DB保存はするが他業務での参照を確認できない |
| 派生値 | 専用処理またはサーバーが決め、通常フォームでは直接変更しない |
| 旧互換 | 移行互換のため残すが正式な判定元ではない |

## 2. 基本情報

保存先：`employee`

| 画面項目 | API項目 | DBカラム | 区分 | 主な利用先 |
|---|---|---|---|---|
| 社員コード | `employeeCode` | `employee_code` | 利用中 | 全画面の従業員識別、CSV、帳票。作成後変更不可 |
| 氏名 | `employeeName` | `employee_name` | 利用中 | 日報、給与明細、台帳、帳票、各選択肢 |
| フリガナ | `employeeNameKana` | `employee_name_kana` | 画面内利用 | 一覧表示・検索、CSV |
| 性別 | `gender` | `gender` | 利用中 | 従業員管理、CSV。現行給与計算の分岐では未使用 |
| 生年月日 | `birthDate` | `birth_date` | 画面内利用 | 一覧・編集・従業員CSV。現行給与Viewで年齢判定へ直接使う処理は確認できない |
| 入社日 | `hireDate` | `hire_date` | 利用中 | 初回給与項目設定の適用開始日、在籍期間、帳票・CSV |
| 退職日 | `resignDate` | `resign_date` | 派生値 | 退職専用処理、在籍・帳票期間判定 |
| 雇用区分 | `employmentType` | `employment_type` | 利用中 | 一覧・CSV・従業員属性。給与額の直接分岐は契約情報が中心 |
| 在籍状態 | `employmentStatus` | `employment_status` | 利用中 | 通常編集可否、退職状態、一覧・絞込 |
| 電話番号 | `phone` | `phone` | 画面内利用 | 従業員連絡先・CSV |
| メール | `email` | `email` | 未連携 | 連絡先・CSV。メール宛先グループへ自動登録しない |
| 郵便番号 | `postalCode` | `postal_code` | 画面内利用 | 従業員連絡先・CSV |
| 住所 | `address` | `address` | 画面内利用 | 従業員連絡先・CSV |
| 有効 | `activeFlag` | `active_flag` | 派生値 | 退職時false、退職取消時true。通常編集不可 |

## 3. 給与・税金

保存先：`employee_payroll_profile`

| 画面項目 | DBカラム | 区分 | 主な利用先 |
|---|---|---|---|
| 税区分 | `tax_category` | 利用中 | 所得税計算・月次給与明細 |
| 扶養人数 | `tax_dependent_count` | 利用中 | 所得税計算 |
| 扶養者あり | `dependent_flag` | 旧互換 | V1 API・画面から削除。税計算は扶養人数を参照 |
| 被扶養者 | `dependent_of_other_flag` | 旧互換 | V1 API・画面から削除 |
| 有給残日数 | `paid_leave_remaining_days` | 利用中 | 日報・月間勤怠の有給情報 |
| 所得税計算 | `income_tax_calc_flag` | 利用中 | 月次給与Viewの所得税計算可否 |
| 住民税控除 | `resident_tax_calc_flag` | 利用中 | 住民税控除可否 |
| 住民税月額 | `resident_tax_monthly` | 利用中 | 年度別住民税明細がない場合のフォールバック |
| 雇用保険 | `employment_insurance_flag` | 利用中 | 雇用保険計算可否 |
| 社保対象 | `social_insurance_flag` | 利用中 | 社会保険全体の対象判定 |
| 健康保険 | `health_insurance_flag` | 利用中 | 健康保険計算可否 |
| 厚生年金 | `pension_insurance_flag` | 利用中 | 厚生年金計算可否 |
| 介護保険 | `care_insurance_flag` | 利用中 | 介護保険計算可否 |
| 通勤手当月額 | `commute_allowance_monthly` | 利用中 | 月次給与明細の通勤手当 |
| 日払い対象 | `daily_pay_flag` | 旧互換 | 正式判定は`employee_contract.payment_cycle`。保存時に同期するだけ |

## 4. 契約情報

保存先：`employee_contract`

| 画面項目 | DBカラム | 区分 | 主な利用先 |
|---|---|---|---|
| 契約開始日 | `contract_start_date` | 利用中 | 日報登録可否・候補・未入力者・月次集計の期間制御 |
| 契約終了日 | `contract_end_date` | 利用中 | 同上。開始日・終了日の当日は勤務可能 |
| 更新あり | `renewal_flag` | 旧互換 | V1 API・画面から削除 |
| 給与形態 | `salary_type` | 利用中 | 月給・週給・日給・時給の給与計算分岐 |
| 支払サイクル | `payment_cycle` | 利用中 | 日次給与対象、給与明細、旧`daily_pay_flag`同期 |
| 月給 | `monthly_salary` | 利用中 | 月給者の給与・時間単価換算 |
| 週給 | `weekly_wage` | 利用中 | 週給者の給与・日次概算 |
| 日給 | `daily_wage` | 利用中 | 日給者の給与・日次概算 |
| 時給 | `hourly_wage` | 利用中 | 時給者の給与・日次概算 |
| 標準労働時間 | `standard_working_hours` | 利用中 | 時間単価・所定時間関連の計算 |
| 契約メモ | `note` | 保存のみ | 従業員編集画面での管理のみ |

## 5. 手当・控除設定

保存先：`employee_payroll_item_enrollment.settings_json`

固定カラムではなく、次のマスター群から項目を生成する。

```text
allowance_master / deduction_master
  + payroll_item_balance_policy
  + payroll_item_parameter_definition
  -> 従業員画面の手当・控除設定
  -> employee_payroll_item_enrollment
```

| 設定 | 区分 | 主な利用先 |
|---|---|---|
| 有効/無効 | 利用中 | 対象従業員の日報入力候補、Rule、取引型項目の有効期間 |
| 適用開始・終了 | 利用中 | 対象日の日報・残高計算で設定が有効か判定 |
| 動的パラメーター | 利用中 | Rule引数、入力元上書き、寮タイプ等の業務値 |
| 入力元 | 利用中 | 日報入力、取引明細、固定・自動計算の経路判定 |
| 残高追跡 | 利用中 | 期首・発生・消化・残数/残高の表示と帳票 |

従業員別に設定できるかは手当・控除マスターだけでなく、対応Policyの`application_scope = EMPLOYEE_ENROLLMENT`で決まる。`ALL_EMPLOYEES`は全従業員共通であり、従業員画面へ個別設定として表示しない。

## 6. バッチ・帳票連携

従業員画面のToolbarから次を実行する。

| 操作 | jobCode | 利用する値 |
|---|---|---|
| 個別日別給与明細 | `PRINT_DAILY_PAY_SLIP` | 支払日・従業員ID |
| 従業員CSV出力 | `EXPORT_EMPLOYEE_CSV` | 従業員基本・給与・契約情報 |
| 従業員データ取込 | `IMPORT_EMPLOYEE` | 外部データ取込定義 |

## 7. 値の所有関係

- 在籍・退職：`employee`が正式な所有元。
- 給与計算条件：`employee_contract`が正式な所有元。
- 税・保険条件：`employee_payroll_profile`が正式な所有元。
- 従業員別手当・控除：`employee_payroll_item_enrollment`が正式な所有元。
- 寮費・携帯料金等の種類と振る舞い：手当・控除マスターとPolicyが正式な所有元。
- 日払い対象：`payment_cycle`が正式な所有元。`daily_pay_flag`は旧互換列。
