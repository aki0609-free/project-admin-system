# お知らせRule V1リファクタリング

## 1. 目的

システム運用のお知らせRuleから、期限日・締日などに応じた通知を安全かつ重複なく自動生成する。

本KBは次を対象とする。

- お知らせRule CRUD
- 手動生成
- Cronスケジュール
- 通知対象データ取得
- 通知本文生成
- 重複生成防止
- テナント分離

## 2. 利用権限

次のAPIはすべて`SYS_ADMIN`限定。

```text
/api/system/notice-rules/**
```

対象操作：

- Rule一覧・詳細
- Rule作成・更新・削除
- 手動生成
- スケジュール再読込・停止

生成されたお知らせの閲覧権限は、ダッシュボードのお知らせ機能側で管理する。

## 3. Ruleコード

`ruleCode`は自動生成履歴とスケジュールを識別する技術コードとして扱う。

- 新規作成時だけ入力可能
- 作成後は変更不可
- 表示名称は`ruleName`で変更可能
- 画面では既存Ruleの`ruleCode`を編集不可にする
- 削除は論理削除

## 4. 日付取得方式

### DATE_COLUMN

対象テーブルの日付カラムを通知対象日として利用する。

必須項目：

```text
targetTableName
targetKeyColumnName
targetDateColumnName
```

### DAY_RULE

締日などのDayRule種別・値から対象年月の日付を解決する。

必須項目：

```text
targetTableName
targetKeyColumnName
targetDayTypeColumnName
targetDayValueColumnName
```

従来はDAY_RULEでも`targetDateColumnName`が必須になっていたため、方式別バリデーションへ修正した。

## 5. 通知日条件

| 条件 | 内容 |
|---|---|
| BEFORE_DAYS | 対象日の指定日前 |
| EXACT_DAY | 対象日当日 |
| AFTER_DAYS | 対象日の指定日後 |
| DAY_OF_MONTH | 毎月の指定日 |
| MONTH_END | 月末 |

`daysBefore`は0～3650、`dayOfMonth`は1～31の範囲で検証する。

## 6. テンプレート

使用可能な変数：

```text
{label}
{date}
{key}
```

本文形式：

```text
PLAIN_TEXT
HTML
MARKDOWN
```

HTMLは既存の`NoticeContentRenderer`でサニタイズしてから保存する。

## 7. テナント分離

通知対象SQLには、バックエンドが必ず次の条件を追加する。

```sql
tenant_id = :tenantId
```

`tenantId`はログイン情報またはスケジュール登録時のRuleテナントから取得する。

画面の`whereClause`から上書きできない。

スケジューラースレッドでは、登録時にRuleの`tenantId`を保持し、実行前に`TenantContext`へ設定し、完了時に必ず削除する。

## 8. SQL安全対策

- テーブル名・カラム名は英数字とアンダースコアだけを許可
- `whereClause`内のセミコロンとSQLコメントを拒否
- INSERT、UPDATE、DELETE、DDL、UNION、CALLなどを拒否
- Named Parameterで`tenantId`をバインド
- 1Ruleあたりの対象取得上限を1000件に設定
- SQL実行時にも安全検証を再実行

現在は既存互換のため対象テーブル・カラム設定を維持している。

Rule管理と同様の「お知らせ対象データソースカタログ」へ移行する場合は、画面・DB・既存Ruleデータへ影響するため別工程とする。

## 9. 重複生成防止

次の単位で生成済み判定を行う。

```text
tenantId
ruleCode
targetTableName
targetKey
targetDate
```

テナントIDを明示的に検索条件へ追加し、別テナントの生成履歴を重複と誤判定しないよう修正した。

DBの一意制約も同じ単位で設定されている。

## 10. 処理分割

### NoticeAutoGenerateService

- 対象Rule取得
- 対象行取得
- Rule単位・対象件数の集計
- `NoticeGenerator`への委譲

### NoticeGenerator

- 対象キー・日付検証
- 通知日の一致判定
- 重複判定
- お知らせ作成
- 生成履歴作成

旧テストは分割前の依存関係を`NoticeAutoGenerateService`へ注入していたため、`NoticeGenerator`がnullとなり6件すべて失敗していた。

サービス責務ごとにテストを分割して修正した。

## 11. API

| Method | Path | 用途 |
|---|---|---|
| GET | `/api/system/notice-rules` | Rule一覧 |
| GET | `/api/system/notice-rules/{id}` | Rule詳細 |
| POST | `/api/system/notice-rules` | Rule作成 |
| PUT | `/api/system/notice-rules/{id}` | Rule更新 |
| DELETE | `/api/system/notice-rules/{id}` | Rule論理削除 |
| POST | `/api/system/notice-rules/generate` | 全Rule手動生成 |
| POST | `/api/system/notice-rules/generate/{id}` | 指定Rule手動生成 |
| POST | `/api/system/notice-rules/schedules/reload` | 全スケジュール再読込 |
| POST | `/api/system/notice-rules/schedules/{id}/reload` | 指定Rule再読込 |
| POST | `/api/system/notice-rules/schedules/{id}/cancel` | 指定Rule停止 |

削除成功時はHTTP 204を返す。

## 12. エラー

| 状況 | HTTP Status |
|---|---:|
| 入力値・SQL・Cron形式不正 | 400 |
| `ruleCode`変更 | 409 |
| Rule未存在 | 404 |
| 権限不足 | 403 |
| 内部エラー | 500 |

## 13. テスト

```text
NoticeAutoGenerateServiceTest
NoticeGeneratorTest
NoticeRuleValidatorTest
DateColumnNoticeTargetResolverTest
NoticeContentRendererTest
```

主な確認項目：

- 複数Rule・複数対象の集計
- 生成／スキップ判定
- 対象キーなし
- 通知日不一致
- テナント単位の重複
- お知らせ・履歴登録
- DAY_RULE方式の必須項目
- `ruleCode`変更拒否
- 危険なWHERE句拒否
- Cron形式
- テナント条件強制
- 1000件上限

## 14. 次工程候補

既存Ruleの移行方針を確認後、次を検討する。

1. お知らせ対象データソースカタログ
2. 対象テーブル・カラムの選択式UI
3. Rule変更履歴
4. スケジュール実行履歴
5. 失敗時の管理者通知
6. 複数EC2／コンテナ構成時のScheduler Lock

現在のAWS構成はEC2 1台のため、V1では単一Schedulerを前提とする。
