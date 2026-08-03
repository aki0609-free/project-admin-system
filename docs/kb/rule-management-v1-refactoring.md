# Rule管理 V1リファクタリング

## 1. 目的

複雑な手当、控除、給与項目を持つ企業が、管理画面から計算Ruleを設定できる基盤を提供する。

柔軟性を維持しながら、次の事故を防止する。

- Ruleコード変更による参照切れ
- 参照中Ruleの削除
- 任意テーブルへのアクセス
- テナントをまたいだデータ取得
- SQLインジェクション
- DSLからの危険なJava API呼び出し
- パラメータの型不整合
- 意図しない大量データ取得

## 2. 対象機能

```text
システム運用
└── Rule管理
```

Rule管理と画面からのテスト実行は、`SYS_ADMIN`ロールだけが利用できる。

## 3. Rule識別子

`ruleName`は手当マスタ、控除マスタ、給与項目計算から参照される技術コードとして扱う。

- 新規作成時だけ入力可能
- 作成後は変更不可
- 表示名称は`ruleDisplayName`で変更可能
- 手当または控除から参照中の場合は削除不可
- 参照中の場合は無効化も不可
- 差し替える場合は新しいRuleを作成し、参照元を変更する

## 4. DSL運用方針

| DSL | 用途 | V1方針 |
|---|---|---|
| JEXL | 管理画面から作成する標準Rule | 推奨 |
| MVEL | 既存互換、高度な式 | 維持 |
| JAVA_BEAN | コード実装が必要な特殊Rule | 登録済みBeanだけ選択可能 |

### JEXL

- Strictモードを有効化
- Safeモードを有効化
- `JexlPermissions.RESTRICTED`を明示
- 未定義変数をエラーとする

### MVEL

- V1では既存互換のため維持
- Rule管理は`SYS_ADMIN`だけが操作可能
- 危険なクラス参照、生成、リフレクション関連構文を保存時と実行時に拒否
- 新しいRuleではJEXLを推奨

### JAVA_BEAN

- Springに登録されたEasy Rulesの`Rule` Beanだけを利用可能
- Bean名の自由実行は行わない
- 利用可能Beanは`GET /api/system/rule-beans`から取得
- 現在登録済みBeanがない場合、一覧は空になる

## 5. Ruleパラメータ

Ruleパラメータ定義の次の項目を実行時に適用する。

- 必須チェック
- デフォルト値
- データ型変換
- 表示順

対応型：

```text
STRING
INTEGER
LONG
DECIMAL
BOOLEAN
DATE
DATETIME
```

給与項目計算基盤が追加するシステムパラメータとの互換性を維持するため、パラメータマスタに未登録の値も保持する。

## 6. データソースカタログ

### 6.1 方針

Rule画面から物理テーブル名とWHERE句を直接入力しない。

画面では、あらかじめ登録されたRule用データソースカタログを選択する。

```text
Rule
└── DataSource
    ├── sourceName：DSL内で使用する名前
    ├── catalogCode：許可済みデータソース
    ├── singleRowFlag
    ├── activeFlag
    └── Column Mapping
        ├── 許可済みカラム
        ├── factKey
        ├── dataType
        └── requiredFlag
```

### 6.2 カタログ項目

| 項目 | 内容 |
|---|---|
| sourceCode | カタログコード |
| displayName | 画面表示名 |
| physicalName | Rule専用View名 |
| whereClauseTemplate | 名前付きパラメータを使った固定条件 |
| tenantScopedFlag | テナント条件を強制するか |
| maxRows | 最大取得件数（1～1000） |
| activeFlag | 利用可能か |
| columns | 公開可能なカラム |

`tenantScopedFlag=true`の場合、`whereClauseTemplate`へ必ず次を含める。

```sql
tenant_id = :tenantId
```

`tenantId`はログイン中のテナント情報からバックエンドが設定する。画面やRule実行リクエストから上書きできない。

### 6.3 互換性

既存Ruleの移行期間に限り、`catalogCode`がないデータソースは旧`tableName`／`whereClause`方式で実行できる。

新規設定はカタログ方式を使用する。既存Ruleは動作確認後に個別移行する。

## 7. データ取得制御

- Column Mappingがある場合は`SELECT *`を使用しない
- 許可カラムだけをSELECTする
- カタログの最大取得件数を強制する
- 最大値は1000件
- `singleRowFlag=true`で複数行取得した場合はエラー
- カタログの物理名は英数字とアンダースコアだけを許可
- 値はNamed Parameterでバインドする
- テナント条件はバックエンドが強制する
- 旧方式ではセミコロン、SQLコメント、更新・削除・DDL・UNIONなどを拒否する

## 8. API

すべて`SYS_ADMIN`限定。

| Method | Path | 用途 |
|---|---|---|
| GET | `/api/system/rules` | Rule一覧 |
| GET | `/api/system/rules/{id}` | Rule詳細 |
| POST | `/api/system/rules` | Rule作成 |
| PUT | `/api/system/rules/{id}` | Rule更新 |
| DELETE | `/api/system/rules/{id}` | Rule論理削除 |
| POST | `/api/system/rules/execution/fire` | 保存済みRuleのテスト実行 |
| GET | `/api/system/rule-data-source-catalogs/active` | 利用可能データソース |
| GET | `/api/system/rule-beans` | 登録済みJAVA_BEAN Rule |

テスト実行リクエスト：

```json
{
  "ruleName": "OVERTIME_ALLOWANCE",
  "context": {
    "parameters": {
      "employeeId": 10,
      "targetMonth": "2026-07",
      "hours": 8
    }
  }
}
```

旧フロントエンドは`facts`を直接送信していたが、バックエンド仕様と一致していなかったため、`context.parameters`へ統一した。

## 9. 画面変更

- 既存Ruleの`ruleName`を編集不可に変更
- 表示されていなかったTestタブを追加
- Test入力の名称を`Input Parameters`へ変更
- 物理`tableName`入力をカタログ選択へ変更
- `whereClause`入力を廃止
- カラム名をカタログの許可カラムから選択
- カラムのデータ型はカタログから自動設定
- JAVA_BEANは登録済みBeanから選択

## 10. DDL

参照ファイル：

```text
backend/src/main/resources/sql/system/rule/catalog_v1.sql
```

ローカル初期構築では`Hibernate ddl-auto=update`を利用できるが、AWS DEVの通常起動は`ddl-auto=validate`である。したがって、AWS DEVではEntity追加だけでテーブルやカラムは自動作成されない。

本DDLは次の場合に使用する。

- AWS DEVなど`ddl-auto=validate`の環境
- 本番移行前のDB資産整理
- Confluence上でのDB定義レビュー

既存DBへ手動適用する場合は、Hibernateがすでに同じテーブル／カラムを作成していないことを確認する。

### 10.1 AWS DEV適用記録

2026-07-27にAWS DEVのRDSへ`catalog_v1.sql`を適用した。

- 対象DB：`project-admin-dev-mysql` / `ADMIN`
- 適用前スナップショット：`project-admin-dev-before-rule-catalog-v1-20260727-143816z`
- 適用前の`rule_data_source`：0件
- 作成したテーブル：
  - `rule_data_source_catalog`
  - `rule_data_source_catalog_column`
- `rule_data_source.catalog_code`：NULL許可の`VARCHAR(100)`として追加
- `tenant_id, catalog_code`の検索インデックス：追加済み
- 一意制約、CHECK制約、外部キー：確認済み
- 既存Ruleデータの更新：なし
- DDL適用に使用した一時IAM権限、S3一時ファイル、EC2一時ファイル：削除済み

適用後、最新バックエンド・フロントエンドをAWS DEVへデプロイし、backend、frontend、Redis、Cloudflare Tunnelの正常起動とActuatorの`UP`を確認した。

## 11. テスト

追加した主なテスト：

```text
RuleMasterCommandServiceTest
RuleMasterValidatorTest
RuleParameterResolverTest
RuleValueConverterTest
JexlDslExecutorTest
GeneralDataFetcherTest
RuleDataSourceCatalogServiceTest
```

確認項目：

- 参照中Ruleの削除拒否
- `ruleName`変更拒否
- 危険なSQL／DSL拒否
- 必須パラメータ
- デフォルト値
- 型変換
- JEXL未定義変数拒否
- カタログ物理Viewの利用
- リクエストのtenantIdをログインテナントで上書き
- 公開カラムだけをSELECT
- カタログの無効カラムを画面へ返さない

## 12. 未実施・次工程

次の項目は、対象業務データの仕様を確認してから追加する。

- `vw_rule_employee_basic`などのRule専用View
- データソースカタログの初期マスターデータ
- 既存Ruleのカタログ方式への移行
- Rule変更履歴と適用開始日のバージョン管理
- MVELからJEXLへの段階的移行

V1ではRule実行結果を給与・締め処理側で保存する。Ruleの適用開始日を使った履歴再計算はV2候補とする。
