# 手当・控除マスター／Rule連携 V1リファクタリング

## 1. 目的

手当・控除マスターを、複雑な給与項目を管理画面から設定できる基盤として安定化する。

本対応では、マスター定義、Rule選択、計算区分の整合性、権限、法定控除の根拠データ参照を対象とする。

## 2. V1で確定した仕様

### 2.1 権限

| 権限 | 操作 |
|---|---|
| `master:view` | 一覧、詳細、法定控除の根拠テーブル参照 |
| `master:manage` | 新規登録、更新、削除、Rule候補取得 |

- 閲覧対象者は固定Role名で判定せず、Role管理から `master:view` を付与する。
- 更新対象者はRole管理から `master:manage` を付与する。
- 初期状態では `SYS_ADMIN` が全権限を持つ。
- Role管理で一般利用者へ `master:view` を割り当てることで、全利用者が閲覧できる。

## 2.2 マスターコード

- 英大文字で開始する。
- 使用可能文字は英大文字、数字、アンダースコアとする。
- 最大50文字とする。
- 入力値は登録時に大文字へ正規化する。
- 作成後は変更不可とする。
- 削除は物理削除せず、`enabled=false` と `deleted_at` を設定する論理削除とする。
- 過去の日報・給与明細に保存済みのコードおよび名称スナップショットは変更しない。
- 論理削除済みコードは履歴識別子として予約し、再利用しない。

## 2.3 計算区分

| 計算区分 | 必須条件 | Rule |
|---|---|---|
| `MANUAL` | 手入力許可が有効 | 保存しない |
| `FIXED` | 固定金額が必須 | 保存しない |
| `AUTO` | 有効なRuleの選択が必須 | 選択したRule名を保存 |

共通制約：

- 初期金額、下限金額、上限金額、表示順は0以上。
- 下限金額は上限金額以下。
- `AUTO` 以外へ変更した場合、既存の `rule_name` は自動的に消去する。
- `AUTO` 保存時には、同一テナント内に有効かつ未削除のRuleが存在することをサーバー側で再検証する。
- Rule実行結果が負数、非数、無限値の場合は計算エラーとする。
- Rule実行失敗時は、給与項目コードとRule名を含む業務エラーへ変換する。

## 2.4 Rule選択

- Rule名の自由入力を廃止する。
- マスター編集画面では有効なRuleだけをプルダウン表示する。
- プルダウン表示は「表示名（Rule名）」とする。
- RuleのDSL本文、データソース、パラメーターはマスター画面へ公開しない。
- Rule本体の作成・更新・テスト実行は引き続き `SYS_ADMIN` 限定とする。

## 2.5 法定控除の詳細参照

法定控除の詳細タブは、計算根拠となるDBデータを確認する読み取り専用画面とする。

| 詳細種別 | 参照内容 |
|---|---|
| `INCOME_TAX` | 現在年の所得税額表 |
| `RESIDENT_TAX` | 現在年度の従業員別・月別住民税 |
| `HEALTH_INSURANCE` | 現在年の健康保険料率 |
| `PENSION` | 標準報酬月額テーブル |
| `EMPLOYMENT_INSURANCE` | 現在年の雇用保険料率 |

- 詳細タブからの追加、更新、削除は行わない。
- 根拠マスターの更新は、それぞれの正式な取込・マスター管理機能に閉じる。
- 対象データがない場合は空表ではなく警告を表示する。
- 旧実装に存在した、バックエンドAPIを持たない汎用詳細CRUDは削除した。

## 3. API

### 手当マスター

| Method | Path | 権限 |
|---|---|---|
| GET | `/api/master/allowances` | `master:view` |
| GET | `/api/master/allowances/{id}` | `master:view` |
| POST | `/api/master/allowances` | `master:manage` |
| PUT | `/api/master/allowances/{id}` | `master:manage` |
| DELETE | `/api/master/allowances/{id}` | `master:manage` |

### 控除マスター

| Method | Path | 権限 |
|---|---|---|
| GET | `/api/master/deductions` | `master:view` |
| GET | `/api/master/deductions/{id}` | `master:view` |
| POST | `/api/master/deductions` | `master:manage` |
| PUT | `/api/master/deductions/{id}` | `master:manage` |
| DELETE | `/api/master/deductions/{id}` | `master:manage` |

### 給与項目用Rule候補

| Method | Path | 権限 |
|---|---|---|
| GET | `/api/master/payroll-rule-options` | `master:manage` |

`targetType=ALLOWANCE` または `targetType=DEDUCTION` を必須とする。

レスポンスには、対象種別または `GENERAL` に一致する有効Ruleの
`id`、`ruleName`、`ruleDisplayName` だけを含める。

## 4. テナント分離

- 一覧、詳細、コード検索、日次・月次の給与項目取得は `tenant_id` と `deleted_at is null` を明示条件にする。
- Ruleの存在確認と候補一覧も現在の `tenant_id` を明示条件にする。
- 別テナントのIDまたはコードを指定しても取得・更新できない。

## 5. 今回変更しない範囲

- `PAYROLL_RULE` の日次・月次・給与計算への組込み。
- `PayrollItemQueryType.PAYROLL` と `BONUS` の項目抽出。
- 手当・控除マスターの初期データ投入。
- 所得税、住民税、保険料、標準報酬月額のデータ投入。
- DDLおよび本番用マスターデータの確定。

`PAYROLL_RULE` はV1対象だが、日報リファクタリング時に日次入力、月次集計、給与計算の境界を確認して実装する。

## 6. DB資産へ戻るときの確認事項

- `allowance_masters` と `deduction_masters` のコード一意制約を、確定したテナント設計に合わせる。
- 既存行の `tenant_id`、`deleted_at`、`enabled` を点検する。
- `AUTO` 行の `rule_name` が、有効な `rule_master.rule_name` を参照しているか点検する。
- `MANUAL` 行の `allow_manual_input` を点検する。
- `FIXED` 行の `default_amount` を点検する。
- 金額の負数および `min_amount > max_amount` を補正する。
- Roleごとの `master:view` / `master:manage` 割当を確定する。

## 7. 確認結果

- バックエンド全テスト：成功。
- 給与項目マスター整合性テスト5件：成功。
- フロントエンド対象範囲のESLint：成功。
- フロントエンド本番ビルド：成功。
- フロントエンド全体型チェック：既存の別機能の型エラーが残っているため失敗。ただし今回変更した手当・控除・給与項目範囲に新規型エラーはない。

## 8. 手動確認シナリオ

1. `master:view` のみを持つ利用者で手当・控除一覧と詳細を閲覧できる。
2. 同利用者には新規登録、保存、削除ボタンが表示されない。
3. `master:manage` を持つ利用者で新規登録できる。
4. 既存コードが編集不可である。
5. `AUTO` 選択時だけRuleプルダウンが表示される。
6. 無効なRuleをAPIへ直接送信すると400になる。
7. `FIXED` で固定金額未入力の場合は保存できない。
8. `MANUAL` で手入力許可を外すと保存できない。
9. 削除後に一覧から消えるが、DB行は論理削除で残る。
10. 所得税、住民税、健康保険、厚生年金、雇用保険の詳細タブで根拠データを参照できる。
