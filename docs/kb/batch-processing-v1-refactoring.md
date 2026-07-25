# バッチ処理 V1 リファクタリング

## 1. 目的

ProjectAdminSystem V1の「システム運用 → バッチ処理」について、既存の帳票・メール・取込・バックアップ基盤を維持しながら、次を安定化する。

- バッチ定義の安全なCRUD
- 手動実行と定期実行の整合
- 同一バッチの二重起動防止
- 定期実行時のテナント情報引継ぎ
- 実行履歴の追跡性向上
- 失敗履歴からの再実行
- 管理機能の権限制御

新しい業務バッチは追加せず、既存基盤の安定化をV1対象とする。

## 2. V1対象範囲

### バッチ種別

| 種別 | 呼出先 |
|---|---|
| `REPORT` | 帳票生成 |
| `REPORT_MAIL` | 管理者用帳票生成、個人別帳票生成、メールキュー作成 |
| `MAIL` | 送信待ちメール処理 |
| `IMPORT` | 外部データ取込定義の実行 |
| `BACKUP` | バックアップ定義の実行 |

### 起動方法

| 値 | 内容 |
|---|---|
| `MANUAL` | 画面または業務機能からの即時実行 |
| `SCHEDULED` | CRONによる定期実行 |
| `RETRY` | 失敗履歴からの再実行 |

## 3. 変更前の主な問題

### 3.1 定期実行でテナント情報が消える

HTTPリクエストとは別のスケジューラスレッドで実行されるため、`TenantContext` が引き継がれていなかった。テナント条件が必要な定義・帳票・取込データを取得できない、または誤った範囲を参照する可能性があった。

### 3.2 二重起動防止が定期実行だけ

Redisロックは定期実行だけに存在し、手動実行と定期実行、または手動実行同士が重複する可能性があった。

### 3.3 履歴情報不足

次の情報が保存されていなかった。

- 起動方法
- 実行者
- 実行パラメータ
- 再実行元の履歴ID

失敗時に同じ条件で再実行する手段もなかった。

### 3.4 テナント境界がRepository条件にない

バッチ定義、実行履歴、出力ファイル取得のRepository検索条件に `tenant_id` が明示されていなかった。

### 3.5 入力と履歴取得が無制限

- 実行パラメータへ任意の入れ子JSONを渡せた
- 実行履歴を全件取得していた
- CRON式の形式検証が保存時に行われていなかった

## 4. V1確定仕様

## 4.1 権限

| 操作 | 権限 |
|---|---|
| バッチ管理画面の表示 | `SYS_ADMIN` |
| バッチ定義CRUD | `SYS_ADMIN` |
| スケジュール再読込・停止 | `SYS_ADMIN` |
| 実行履歴参照・再実行 | `SYS_ADMIN` |
| 業務画面からの即時実行 | ログインユーザー |
| 実行直後の帳票ファイル取得 | ログインユーザー |

即時実行とファイル取得をログインユーザーに残す理由は、従業員・顧客などの既存業務画面から帳票バッチを呼び出す導線を維持するためである。

バックエンドの認可を最終的なアクセス制御とする。

## 4.2 バッチ定義

`jobCode` の仕様：

- 英大文字で開始
- 英大文字、数字、アンダースコアだけを許可
- 最大100文字
- 作成後は変更不可
- テナント内で一意

例：

```text
MONTHLY_PAY_SLIP
IMPORT_RESIDENT_TAX
FISCAL_YEAR_REPORT_BACKUP
```

スケジュールを有効にする場合：

- `scheduleEnabled=true`
- `scheduleType=CRON`
- 有効なSpring形式の6フィールドCRON式を設定

例：

```text
0 0 2 * * *
```

## 4.3 二重起動防止

すべての起動方法で同じRedisロックを使用する。

```text
batch:execution:{tenantId}:{jobCode}
```

対象：

- 手動実行
- 定期実行
- 再実行
- 締め処理など内部サービスからの実行

同じテナント・同じ `jobCode` が実行中の場合、後続処理は開始しない。

ロックにはTTLを設定し、解除時はLuaスクリプトでロック所有者を確認してから削除する。

## 4.4 定期実行とテナント

スケジュール登録時にバッチ定義の `tenant_id` を保持する。

実行時の流れ：

```text
CRON発火
  → TenantContextへtenant_idを設定
  → バッチ定義をtenant_id + jobCodeで再取得
  → Redisロック取得
  → バッチ実行
  → ロック解除
  → TenantContextを必ずクリア
```

これにより、スケジューラスレッドでも対象テナントのデータだけを処理する。

## 4.5 実行パラメータ

再実行のため、実行時パラメータをJSONで履歴へ保存する。

許可範囲：

- 最大50項目
- キーは英字で開始
- キーは英数字、アンダースコア、ピリオド、ハイフン
- 値は文字列、数値、真偽値、`null`
- 文字列は1項目1,000文字以内
- 配列、オブジェクト、入れ子構造は不可

業務パラメータの例：

```json
{
  "targetMonth": "2026-07",
  "employeeId": 1001
}
```

パスワード、APIキー、アクセストークンなどの秘密情報は渡さない。秘密情報はAWS Secrets Managerまたはアプリケーション設定から取得する。

## 4.6 実行履歴

履歴へ次を保存する。

- バッチコード、名称、種別、対象コード
- 実行状態
- 起動方法
- 実行者
- 開始・終了日時
- 実行パラメータJSON
- 再実行元履歴ID
- 完了メッセージまたはエラー
- 出力ファイル情報

画面の一覧取得は最新200件とする。

## 4.7 再実行

- 再実行できるのは `FAILED` の履歴だけ
- 元履歴のパラメータを復元する
- 現在有効なバッチ定義を利用する
- 新しい実行履歴を作成する
- `trigger_type=RETRY`
- `retry_source_log_id` に元履歴IDを保存する
- 二重起動防止の対象とする

定義が無効または削除済みの場合は再実行しない。

## 4.8 テナント分離

次の検索はRepositoryの条件へ `tenant_id` を明示する。

- バッチ定義一覧・詳細・コード検索
- バッチコード重複確認
- 実行履歴一覧・ジョブ別一覧
- 再実行元履歴
- 出力ファイル取得
- スケジュール個別再読込・停止対象

フロントエンドから別テナントのIDを指定されても取得・操作できない。

## 5. API

| Method | Path | 内容 | 権限 |
|---|---|---|---|
| GET | `/api/system/batch-jobs` | 定義一覧 | SYS_ADMIN |
| GET | `/api/system/batch-jobs/{id}` | 定義詳細 | SYS_ADMIN |
| POST | `/api/system/batch-jobs` | 定義作成 | SYS_ADMIN |
| PUT | `/api/system/batch-jobs/{id}` | 定義更新 | SYS_ADMIN |
| DELETE | `/api/system/batch-jobs/{id}` | 定義削除 | SYS_ADMIN |
| POST | `/api/system/batch/execute/{jobCode}` | 即時実行 | ログインユーザー |
| POST | `/api/system/batch/scheduled-execute/{jobCode}` | スケジュール設定を使った手動実行 | SYS_ADMIN |
| POST | `/api/system/batch/retry/{logId}` | 失敗履歴の再実行 | SYS_ADMIN |
| GET | `/api/system/batch/logs` | 最新履歴 | SYS_ADMIN |
| GET | `/api/system/batch/logs/job/{jobCode}` | ジョブ別履歴 | SYS_ADMIN |
| GET | `/api/system/batch/logs/{logId}/file` | 出力ファイル取得 | ログインユーザー |
| POST | `/api/system/batch/schedules/reload` | 全スケジュール再読込 | SYS_ADMIN |
| POST | `/api/system/batch/schedules/{id}/reload` | 個別再読込 | SYS_ADMIN |
| POST | `/api/system/batch/schedules/{id}/cancel` | 個別停止 | SYS_ADMIN |

## 6. DB変更

DDL：

```text
backend/src/main/resources/sql/system/batch/batch_v1.sql
```

追加カラム：

| カラム | 内容 |
|---|---|
| `trigger_type` | MANUAL / SCHEDULED / RETRY |
| `executed_by` | 実行ユーザー。定期実行はsystem |
| `parameters_json` | 再実行用パラメータ |
| `retry_source_log_id` | 再実行元履歴ID |

既存履歴の補完値：

- `trigger_type=MANUAL`
- `executed_by=legacy`

既存DBへ適用する場合は、DDLを一度だけ実行する。新規DBではHibernate `ddl-auto=update` による生成対象となる。

旧DBに `job_code` 単独のUNIQUEインデックスが存在する場合は、インデックス名を確認して削除した後、次へ変更する。

```text
UNIQUE (tenant_id, job_code)
```

## 7. 画面変更

実行履歴へ次を追加する。

- 起動方法
- 実行者
- 再実行元ID
- 失敗時の「再実行」
- 出力ファイルがある場合の「ダウンロード」

「バッチ処理」メニューは `SYS_ADMIN` のみ表示する。

## 8. テスト

追加テスト：

- 正常なスカラーパラメータの保存・復元
- 入れ子パラメータの拒否
- 不正なパラメータ名の拒否
- 不正CRON式の拒否
- `jobCode` 変更の拒否
- 小文字を含む `jobCode` の拒否

確認結果：

- バッチ対象テスト：成功
- バックエンド全テスト：成功
- バッチ配下のESLint：成功
- バッチ配下のTypeScriptエラー：0件

フロントエンド全体の型検査には、応募者・顧客・日報・メール・Storybookなど既存の別機能エラーが残っている。バッチ処理由来のエラーは解消済み。

## 9. 運用確認

デプロイ後は次を確認する。

1. SYS_ADMINでバッチ管理画面を開ける
2. 一般ユーザーにはバッチ管理メニューが表示されない
3. 定義の新規作成・更新・論理削除ができる
4. 不正なCRON式を保存できない
5. 手動実行の履歴が `MANUAL` と実行ユーザーで記録される
6. 定期実行の履歴が `SCHEDULED`、`system` で記録される
7. 同一ジョブの実行中に二重実行が拒否される
8. 失敗履歴を再実行し、元履歴IDが記録される
9. 帳票出力がある場合にファイルを取得できる
10. 定期実行が対象テナントのデータだけを処理する

## 10. V2候補

- バッチ定義単位の実行許可ロール
- 実行時間上限の定義化
- Redisロックの延長（heartbeat）
- 実行履歴の保存期間・自動アーカイブ
- 大量履歴のサーバーサイドページング
- スケジュール実行監視と失敗通知
- 実行パラメータスキーマのバッチ定義別管理
- 承認フローと連動した実行制御

## 11. 業務日時とスケジューラー

バッチ機能の業務タイムゾーンは`Asia/Tokyo`とする。

アプリケーション共通の`Clock`を次へ適用した。

- 実行開始日時
- 正常終了日時
- 失敗終了日時
- バッチ定義の最終実行日時
- バッチ定義の論理削除日時
- バッチ用`ThreadPoolTaskScheduler`

Cron式自体や実行頻度は変更していない。JVMやOSの既定タイムゾーンにかかわらず、日本時間を基準に次回実行時刻を計算する。

お知らせ用`ThreadPoolTaskScheduler`にも同じ共通Clockを設定し、通知CronとバッチCronのタイムゾーンを統一した。

追加テスト：

```text
BatchSchedulerConfigTest
BatchExecutionLogServiceTest
BatchExecutionServiceTest
BatchJobDefinitionCommandServiceTest
```

主な確認項目：

- スケジューラーが共通Clockを使用する
- 開始・失敗日時が固定Clockと一致する
- 正常終了日時と最終実行日時が一致する
- 定義削除日時が固定Clockと一致する
- 実行終了後もRedisロックを解放する

APIおよびDBカラムの変更はない。
