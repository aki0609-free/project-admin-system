# 従業員管理 V1 現行仕様・リファクタリング記録

## 1. 目的

従業員管理のV1仕様、主要なコード配置、保存境界、検証方法をまとめる。

対象画面：

- 従業員情報
- 従業員別の手当・控除設定
- 給与・税金設定
- 契約情報
- 退職処理
- 貸付・積立

従業員勤務表はV1対象外のため、本書の完成対象には含めない。

## 2. 画面とAPI

### 2.1 従業員情報

- 画面URL：`/employee/information`
- 一覧API：`GET /api/employees`
- 詳細API：`GET /api/employees/{id}`
- 登録API：`POST /api/employees`
- 更新API：`PUT /api/employees/{id}`
- 削除API：`DELETE /api/employees/{id}`

画面は「基本情報」「手当・控除設定」「給与・税金」「契約情報」の4タブで構成する。

社員コードはV1では登録時の必須入力とし、登録後は変更できない。DBの内部IDは自動採番だが、社員コード自体の自動連番は行わない。

### 2.2 退職処理

- 退職API：`POST /api/employees/{id}/resign`
- 退職取消API：`POST /api/employees/{id}/cancel-resignation`
- 設定取得API：`GET /api/employees/resignation-configuration`

退職は通常編集から在籍状態を書き換えず、専用処理を使用する。管理画面で設定した必須TODOが未完了の場合は退職できない。退職取消後は在籍状態へ戻り、通常編集を再開できる。

### 2.3 手当・控除設定

従業員画面へ表示する項目をコードへ固定しない。

以下の条件を満たす給与項目ポリシーが、自動的に従業員の「手当・控除設定」タブへ表示される。

- ポリシーが有効
- 対応する手当／控除マスターが有効
- 適用対象が `EMPLOYEE_ENROLLMENT`

`ALL_EMPLOYEES` の項目は全従業員へ適用されるため、従業員別タブでは変更させない。

パラメーターの入力欄はパラメーター定義から生成する。必須値、数値、日付、真偽値、選択肢はフロントとバックエンドの両方で検証する。Rule Resolverが値を解決するパラメーターは従業員画面へ表示しない。

動的入力欄は `EmployeePayrollItemParameterField.vue` へ統一する。定義の表示順を維持したまま、`TEXT`、`NUMBER`、`SELECT`、`BOOLEAN`、`DATE`を同じレンダラーで描画する。

Fuyo初期設定では次の3項目を従業員別設定として扱う。

| 項目 | 標準入力元 | 残高管理 | 従業員画面 |
|---|---|---:|---|
| 寮費 | 日報／従業員設定による明細切替 | 日数 | 利用有無・寮タイプ・徴収方式 |
| 携帯電話貸出料 | 明細取引 | なし | 利用有無・共通控除明細 |
| Wi-Fi使用料 | 明細取引 | なし | 利用有無・共通控除明細 |

新しい手当・控除は、管理画面の「適用・連携設定」で適用対象を`従業員ごとに設定`にすると従業員タブへ自動連携される。項目コードごとのフロント実装は追加しない。

### 2.4 貸付・積立

- 画面URL：`/employee/loan-savings`
- 貸付API：`/api/employee-loans`
- 積立API：`/api/employee-savings`

同一従業員へ複数の有効な貸付または積立設定を登録できない。返済・積立の増減は日報側の確定処理と連携し、残高を即時反映する。

## 3. 保存時の整合性

保存ボタンは表示中のタブだけでなく、4タブすべてを検証する。不正な値がある場合は保存APIを呼ばず、該当タブへ移動してエラーを表示する。

主な検証：

- 社員コード・氏名の必須および文字数
- メールアドレス形式
- 税・有給・給与・労働時間などの非負数
- 契約終了日が契約開始日以降であること
- 有効化した手当・控除の必須パラメーターと型

バックエンドではBean Validationに加え、社員コード変更、退職状態への直接変更、重複コード、契約期間を検証する。

## 4. 旧データ互換

従業員本体だけが存在し、給与プロファイルまたは契約レコードが存在しない旧データでも詳細APIは空の既定値を返す。画面で保存すると不足していた子レコードを作成する。

`dormitory_flag` と `dormitory_type` は移行互換列として残っている。V1の画面と業務判定は、固定列ではなくマスター駆動の手当・控除設定を使用する。

## 5. 削除と日時

従業員削除は物理削除ではなく論理削除である。日報などの業務参照が存在する場合は削除ポリシーが拒否する。

削除日時はアプリ共通の `Clock` から取得する。固定Clockを使用することで、時系列テストでも削除時点を再現できる。

## 6. 主なコード配置

### フロントエンド

- 画面：`frontend/src/features/employees/pages/EmployeePage.vue`
- 編集Dialog：`frontend/src/features/employees/components/EmployeeEditDialog.vue`
- 手当・控除設定：`frontend/src/features/employees/components/EmployeePayrollItemSettingsPanel.vue`
- 画面ロジック：`frontend/src/features/employees/composables/useEmployeeEditDialog.ts`
- 入力検証：`frontend/src/features/employees/validation/employeeSchemas.ts`
- API型：`frontend/src/features/employees/types/employeeApiTypes.ts`
- API変換：`frontend/src/features/employees/utils/employeeConverters.ts`

### バックエンド

- Controller：`backend/src/main/java/com/project/backend/features/employee/controller/EmployeeAdminController.java`
- Service：`backend/src/main/java/com/project/backend/features/employee/service/EmployeeAdminService.java`
- Entity：`backend/src/main/java/com/project/backend/features/employee/entity/Employee.java`
- Mapper：`backend/src/main/java/com/project/backend/features/employee/mapper/EmployeeMapper.java`
- マスター駆動設定：`backend/src/main/java/com/project/backend/features/master/payrollitem/balance/EmployeePayrollItemSettingService.java`

## 7. テスト

- バックエンド従業員ドメインテスト
- 従業員登録と手当・控除設定のE2E
- 基本／給与税金／契約タブの共通フォームE2E
- 全タブ横断保存バリデーションE2E
- 退職TODOのE2E
- 貸付・積立のE2E
- CSV出力・個別日別給与明細のE2E
- 権限別アクセスE2E

## 8. V1で維持する境界

- 手当・控除マスター／Ruleの業務仕様変更は、System／Rule解析後に別途実施する。
- 従業員勤務表はV1では実装しない。
- 既存API互換列のDB削除は、全参照箇所と移行資産を確認してから別作業で行う。
