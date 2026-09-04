# 手当マスター 未使用・未連携機能の調査 V1

## 1. 重要な不整合

### 1.1 課税対象設定が税計算に未反映

`taxable`は登録・更新・一覧表示されるが、月次給与Viewはこの列を参照しない。非課税手当を登録しても、現状は総支給に含まれたまま課税対象額へ入る可能性がある。

V1で通勤手当等の非課税手当を扱うなら高優先度で修正が必要である。

### 1.2 旧月次表示設定（管理画面から除外済み）

月次表示は手当マスターではなく`vw_monthly_pay_slip_statement_item_source`を正とする。旧`showOnMonthlyStatement`はDB互換項目として残すが、管理画面と汎用MONTHLY providerから除外した。

## 2. 汎用月次・給与単位の未接続

`allowanceUnit=MONTHLY/PAYROLL`を選択できる一方、`AllowancePayrollItemValueProvider`はPAYROLLに空リストを返す。現行月次給与は日報実績とView主体であり、月次Ruleとしてマスターを一括実行する機能ではない。

## 3. 詳細参照タイプは拡張口のみ

`AllowanceDetailResolver`、`AllowanceDetailProvider`、詳細responseは存在するが、詳細種別はNONEだけである。控除の税・保険詳細に相当する画面はない。

関連する次の型・mapperは現状の画面で実質未使用である。

- `AllowanceDetailTableRow`
- `toAllowanceDetailRows()`
- 旧`AllowanceSavePayload` alias（2026-09-02に削除済み）
- 旧`AllowanceValueRequest`（2026-09-02に削除済み。共通PayrollItem request経路へ統一）

## 4. 共通Policyの未連携

| 項目 | 状態 |
|---|---|
| `carryForwardFlag` | 残高計算で未参照 |
| `advanceConsumptionFlag` | 超過許可判定で未参照 |
| `accrualFrequency` | 保存・返却のみ |
| `accrualRuleName` | 計算では参照するが画面編集不可 |
| parameterの`dailyDisplayFlag` | 保存・返却されるが日報UIの表示制御で未参照 |

## 5. 保存・分類中心

- `allowanceType`は計算分岐で未参照。
- `note`は管理メモのみ。
- `detailViewType`はNONE固定。
- `taxable`は現時点では保存・表示のみ。

## 6. V1対応判断

| 優先度 | 課題 | 推奨 |
|---|---|---|
| 高 | 非課税手当が課税額へ反映されない | 月次Viewで課税支給と総支給を分離 |
| 対応済み | 旧月次表示設定 | 管理画面から除外し、表示用Viewへ責務を統一 |
| 中 | MONTHLY/PAYROLL単位のRule未実行 | V1のView計算方針を明記し、誤設定を防ぐ |
| 中 | 残高Policyの一部未実装 | 対象手当の要件に応じて実装またはUI説明 |
| 低 | 詳細拡張用の未使用型 | V1後に削除または将来拡張として隔離 |

2026-09-05確認時点で、日報候補、従業員別適用、Rule計算、明細取引、残高、月次View集計は控除と同じ共通給与項目基盤へ接続済みである。`taxable`の月次税計算への接続だけは未完了であり、反映済みとは扱わない。

## 7. 必須テスト観点

- MANUAL/FIXED/AUTOと手動変更理由
- 全従業員手当と従業員別手当
- Rule parameterの型・既定値・resolver
- 日報手当から月次総支給への集計
- 取引型手当から月次への集計
- 課税/非課税手当を含む課税対象額
- 月次表示用Viewと給与明細の項目
- 無効化・論理削除後の候補除外

## 8. 調査した主な実装

```text
frontend/src/features/master/allowance/
frontend/src/features/master/payrollitem/
backend/src/main/java/com/project/backend/features/master/allowance/
backend/src/main/java/com/project/backend/features/master/payrollitem/
backend/src/main/resources/sql/system/report/pay_slip/
```
