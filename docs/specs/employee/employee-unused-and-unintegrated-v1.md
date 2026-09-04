# 従業員管理 未使用・未連携機能の調査 V1

ドメイン：従業員管理

## 1. 重要な不整合

### 1.1 旧寮カラムと動的控除基盤が併存

`employee`には次の固定カラムが残っている。

- `dormitory_flag`
- `dormitory_type`

V1では動的給与項目基盤を正式値とし、旧列をAPI・画面から切り離した。DB列と旧データ補正用メソッドは移行互換として残すが、新しい業務処理では参照・更新しない。

一方、日報の給与項目基盤は寮費を動的控除として扱う方針である。ただし既存SQLの一部は現在も`employee.dormitory_type`を初期設定へコピーしている。

```text
画面の正式経路
  控除マスター + Policy + 従業員別Enrollment

残存する旧経路
  employee.dormitory_flag / dormitory_type
  -> 一部初期化SQL・CSV
```

正式値は動的給与項目基盤である。DB列のDROPは、既存データ移行を確認した後の別マイグレーションで行う。

### 1.2 寮費残高サマリーが常に未追跡値

旧寮サマリーは実残高ではなかったため、V1 API・画面モデルから削除した。

- `dormitoryOpeningDays`
- `dormitoryCurrentMonthDays`
- `dormitoryConsumedDays`
- `dormitoryRemainingDays`

残高表示は`payrollItemSettings`内の動的な各給与項目設定を正式値として使用する。

## 2. 旧互換・非表示Request項目

| 項目 | 状態 | 現行動作 |
|---|---|---|
| `dailyPayFlag` | 旧互換 | 画面入力なし。`paymentCycle == DAILY`から保存時に同期 |
| `dormitoryFlag` | DB移行互換のみ | API・画面から削除。動的控除設定が正式値 |
| `dormitoryType` | DB移行互換のみ | 同上 |
| `resignDate` | 専用処理 | 通常更新Requestには含まれるが、退職API以外では拒否・無視 |
| `activeFlag` | 専用処理 | 通常フォームから変更不可。退職・退職取消が変更 |

これらはAPI DTOと画面modelへ残っているため、見かけ上は編集可能項目に見える。正式な更新経路を資料とOpenAPIで明確にする必要がある。

## 3. 保存されるが現行業務へ未連携の項目

| 項目 | 状態 | 未連携内容 |
|---|---|---|
| `dependentFlag` | DB移行互換のみ | API・画面から削除。税計算は税区分・扶養人数を参照 |
| `dependentOfOtherFlag` | DB移行互換のみ | 同上 |
| `renewalFlag` | DB移行互換のみ | API・画面から削除 |
| 契約開始・終了日 | 連携済み | 日報保存・未入力者・月次勤怠・画面候補を期間で制御 |
| 契約メモ | 保存のみ | 他機能・帳票へ連携しない |
| 従業員メール | 未連携 | メール宛先グループへ自動登録しない |

## 4. V1メニューへ未接続の旧勤務表機能

次の旧勤務表一式は日報と重複していたため、V1アプリケーションコードから削除した。

```text
EmployeeTimesheetPage.vue
EmployeeTimesheetEditDialog.vue
EmployeeTimesheetTable.vue
useEmployeeTimesheet*.ts
EmployeeTimesheetController / Service / Repository / Entity
```

`employee_timesheet`テーブルと旧SQL資産は既存DB互換のため残している。DROPはデータ有無を確認して別マイグレーションで行う。

## 5. 適用期間編集の制約

従業員別手当・控除設定は適用開始・終了を保持するが、従業員画面の保存Requestは主に次を送る。

- 対象種別・コード
- 有効/無効
- 動的パラメーター

適用開始日は新規従業員では入社日、更新では保存日を基準にServiceが決める。画面から任意の過去日・未来日を直接指定する機能ではない。

これは履歴整合性を守る一方、将来予約や訂正開始日を画面指定したい場合には不足する。V1では仕様として明記し、直接DB更新で回避しないこと。

## 6. 削除対象ではないもの

- 社員コード変更不可は仕様である。
- 退職を通常更新から行えないのは仕様である。
- `ALL_EMPLOYEES`の給与項目が従業員別設定に出ないのは仕様である。
- 日報で使う給与項目は、マスターだけでなくPolicy・適用範囲・入力元が揃って初めて表示される。
- メールアドレスのメール基盤への自動登録はV1で見送った運用である。

## 7. V1判断一覧

| 優先度 | 課題 | 推奨 |
|---|---|---|
| 完了 | 旧寮列と動的控除基盤の二重管理 | API・画面は動的基盤へ統一。DB列DROPは移行後 |
| 完了 | 旧寮残高DTOが実値でない | DTO・画面モデルから削除 |
| 完了 | 契約期間が日報可否へ未連携 | 共通Policyで保存・候補・集計を制御 |
| 完了 | 旧勤務表コードが残存 | アプリケーションコードを削除。DB表は移行互換 |
| 完了 | 扶養boolean・更新あり | API・画面から削除。DB列は移行互換 |
| 維持 | 契約メモ | 契約管理上の自由記録として維持 |

## 8. 調査した主な実装

```text
frontend/src/features/employees/
frontend/src/app/menu/employeeMenu.ts
backend/src/main/java/com/project/backend/features/employee/
backend/src/main/java/com/project/backend/features/master/payrollitem/
backend/src/main/resources/sql/daily_report/payroll_item_balance_foundation_v1.sql
backend/src/main/resources/sql/employee/dormitory_foundation_v1.sql
```
