# 汎用マスター基盤 V1

## 1. 目的

システムメニューとユーザー管理を再利用可能な基盤とし、案件固有の業務マスターをコード追加なしで業務管理へ公開する。

- システム運用 → 共通設定 → 汎用マスター定義：入力構造を定義する。
- 管理者メニュー → 業務管理：公開済み定義を動的タブとして表示し、実データを登録する。
- 業務処理：マスターコードと基準条件を契約としてデータを参照する。

## 2. ライフサイクル

1. 汎用マスター定義を下書きで作成する。
2. 項目キー、型、必須、一覧表示、検索対象、単位等を設定する。
3. 定義を公開する。
4. 業務管理にタブが表示される。
5. 共通一覧・編集ダイアログからデータを登録する。

登録済みデータがある公開マスターは、項目キーの追加・削除および型変更を禁止する。表示名、単位、表示順等の互換性を壊さない変更だけを許可する。

## 3. テーブル

| テーブル | 用途 |
|---|---|
| `generic_master_definition` | マスターコード、名称、表示先、公開状態 |
| `generic_master_field_definition` | 項目キー、型、入力・一覧・検索メタデータ |
| `generic_master_record` | テナント別の実データ。値は定義済みキーだけをJSONで保持 |

定義とデータはすべてテナント分離し、データは楽観ロックと論理削除に対応する。

## 4. 対応する項目型

- `STRING`
- `TEXT`
- `NUMBER`
- `DATE`
- `BOOLEAN`
- `SELECT`

`SELECT`の選択肢はJSON配列、または`title`と`value`を持つJSONオブジェクト配列で定義する。

## 5. API

### 定義管理

- `GET /api/system/generic-masters/definitions`
- `POST /api/system/generic-masters/definitions`
- `PUT /api/system/generic-masters/definitions/{id}`
- `POST /api/system/generic-masters/definitions/{id}/publish`
- `DELETE /api/system/generic-masters/definitions/{id}`

### 業務管理

- `GET /api/admin/business-settings/generic-masters`
- `GET /api/admin/business-settings/generic-masters/{definitionId}/records`
- `POST /api/admin/business-settings/generic-masters/{definitionId}/records`
- `PUT /api/admin/business-settings/generic-masters/{definitionId}/records/{recordId}`
- `DELETE /api/admin/business-settings/generic-masters/{definitionId}/records/{recordId}`

すべて`SYS_ADMIN`権限を必要とする。

## 6. 会社・期間別給与制度の移行

`PAYROLL_POLICY`を予約マスターコードとして公開し、次の構成で移行した。

1. 既存`payroll_policy_setting`を初回だけ汎用レコードへ移行する。
2. 期間重複・日付順序・正数条件は`GenericMasterBusinessContract`で検証する。
3. 業務管理では専用フォームを廃止し、公開定義から動的タブを生成する。
4. 日報・給与Ruleは汎用Resolverを優先して参照する。
5. 汎用定義が未導入の環境だけ旧テーブルへフォールバックする。

旧テーブルとAPIはロールバック互換のため当面残すが、公開済み汎用定義が存在する環境では計算の正本として使用しない。

給与制度のように業務固有の整合性を必要とするマスターは、汎用テーブルや画面を分岐させず、マスターコード単位のContractとResolverを追加する。この方法を後続の業務マスター移行でも標準とする。

## 7. 退職時設定の移行

退職時設定は、用途に応じて共通設定と汎用マスターへ分離した。

### 退職ダイアログ文言

次の3項目をシステム共通設定で管理する。

- `BUSINESS / RESIGNATION_DIALOG / DIALOG_TITLE`
- `BUSINESS / RESIGNATION_DIALOG / GUIDANCE_MESSAGE`
- `BUSINESS / RESIGNATION_DIALOG / CONFIRMATION_MESSAGE`

従業員の退職ダイアログはこの設定値を参照する。共通設定が未導入の環境では、旧`employee_resignation_setting`へフォールバックする。

### 退職時TODO

`RESIGNATION_CHECKLIST`を予約マスターコードとして公開し、次の項目を持つ。

- `CODE`: TODOコード
- `NAME`: 項目名
- `DESCRIPTION`: 説明
- `REQUIRED_FLAG`: 必須判定
- `DISPLAY_ORDER`: 表示順

既存`employee_resignation_checklist_master`は初回だけ汎用レコードへ移行する。退職ダイアログの表示と退職実行時の必須確認は同じResolverを使用し、画面とサーバー検証の参照元を一致させる。

同一の有効TODOコードおよび不正な表示順は`ResignationChecklistGenericMasterContract`で拒否する。旧テーブルとAPIはロールバック互換のため残すが、公開済み定義が存在する環境では使用しない。

## 8. 給与締日・支払日設定の移行

`PAYROLL_CLOSING_SETTING`を予約マスターコードとして公開し、給与の締日と支払日を1件の有効レコードで管理する。

- 設定コードは`PAYROLL`固定
- 締日・支払日はそれぞれ対象月、日付種別、日を保持
- 日付種別は`DAY_OF_MONTH`または`END_OF_MONTH`
- 有効レコードは常に1件だけとし、削除・無効化による設定欠落を拒否

既存の締め処理・SQL帳票は`closing_setting`を広く参照しているため、汎用レコード保存時に`GenericMasterRecordSynchronizer`を通じて同テーブルへ互換投影する。設定の編集元は汎用マスター、`closing_setting`は既存処理を壊さないための読取投影という位置付けである。

この同期は汎用マスターと同一トランザクションで行われるため、帳票だけ旧値を参照する状態を作らない。旧専用APIはロールバック互換のため残すが、業務管理画面からは使用しない。
