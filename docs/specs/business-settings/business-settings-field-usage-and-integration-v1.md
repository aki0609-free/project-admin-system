# 業務管理 入力項目の利用先・システム連携 V1

ドメイン：業務管理

## 1. 退職時文言

| 画面項目 | DB列 | 制約 | 利用先 |
|---|---|---|---|
| ダイアログタイトル | `dialog_title` | 必須、200文字 | `EmployeeResignDialog`のタイトル |
| 案内文 | `guidance_message` | 必須、2000文字 | 退職確認Dialogの説明 |
| 警告見出し | `confirmation_message` | 必須、500文字 | 退職確認Dialogの警告欄 |

正本は`setting_code=DEFAULT`の1設定である。保存前後に空白を除去する。

## 2. 退職時TODO

| 項目 | DB列 | 制約・変換 | 利用先 |
|---|---|---|---|
| TODOコード | `code` | 必須、100文字、大文字化、作成後変更不可、テナント内一意 | マスター識別 |
| 項目名 | `name` | 必須、255文字 | 退職Dialog、未完了エラー |
| 説明 | `description` | 任意、1000文字、空白はNULL | 退職Dialog補足 |
| 必須項目 | `required_flag` | 必須Boolean | Frontendの保存ボタン制御＋Backend退職API検証 |
| 表示順 | `display_order` | 0以上 | 退職Dialog・管理一覧の順序 |
| 有効 | `active_flag` | 必須Boolean | 退職Dialogと必須判定の対象 |

無効TODOと論理削除TODOは新しい退職処理に表示されない。退職実行時は「有効かつ必須」の全IDがrequestに含まれることをサーバーでも確認する。

## 3. 給与締日・支払日

各設定は`DayRule`で表現される。

| 子項目 | 意味 | 制約 |
|---|---|---|
| `type` | 月末または日指定 | 必須 |
| `value` | 日指定時の日 | 1～31。月末時はNULL保存 |
| `monthOffset` | 対象月からの前後月 | -12～12、未指定は0 |

| 画面項目 | DB列 | 利用先 |
|---|---|---|
| 給与締日 | `closing_day_type/value`, `closing_month_offset` | 月次対象期間、月次勤怠、給与締め |
| 給与支払日 | `payment_day_type/value`, `payment_month_offset` | 給与支払予定日の計算 |

未登録時は画面・業務処理ともに月末締め・翌月25日払いを補完する。管理画面で保存すると、それ以降はDB設定が優先される。

## 4. 月次締め帳票

| 項目 | 保存先 | 利用先・意味 |
|---|---|---|
| 生成 | `active_flag` | 月次締めジョブの実行対象 |
| 順序 | `execution_order` | 帳票・台帳を処理する順番、1以上 |
| 帳票コード | `output_code` | `operation_report_preview.report_code`との結合キー |
| 帳票名 | 保存しない | 帳票管理側の表示名を参照 |
| jobCode | 保存しない | 帳票管理側のジョブを参照 |
| 形式 | `output_type=REPORT` | 現画面はREPORTのみ管理 |
| 保存年数 | `backup_retention_years` | 年度帳票バックアップの対象判定・保持期限、V1は1～7またはNULL |
| 必須 | `required_flag` | 現Backendは保存時に常にtrue |

請求書パターン帳票`MONTHLY_INVOICE_PATTERN_*`の保持年数は、直接設定がない場合`MONTHLY_INVOICE`の設定を継承する。

## 5. 年度帳票バックアップ

| 項目 | DB列 | 制約 | 利用先 |
|---|---|---|---|
| 会計年度の開始月 | `fiscal_year_start_month` | 1～12 | 帳票の対象年度・年度末算出 |
| 年度終了後の猶予日数 | `grace_days` | 0～90 | 実行可能日＝年度末＋猶予日数 |
| 起動時に未処理年度を自動確認 | `startup_enabled` | Boolean | `ApplicationReadyEvent`の対象設定選別 |
| 設定を有効にする | `active_flag` | Boolean | 自動・手動実行可否 |
| 手動実行年度 | DB保存なし | 画面2000～2200、Backend2000～9999 | その場の実行requestのみ |

保存年数はこのタブではなく「締め帳票」タブの各帳票に設定する。年度バックアップはRDSスナップショットやDB全体バックアップではなく、確定済み月次帳票ファイルの保存機能である。

## 6. 外部サポートリンク

| 項目 | DB列 | 制約 | 利用先 |
|---|---|---|---|
| インシデント報告URL | `incident_report_url` | 必須、HTTPS、host必須、2048文字 | `AppHeader`のインシデント報告 |
| マニュアルURL | `manual_url` | 同上 | `AppHeader`のマニュアル |

URLは`URI.toASCIIString()`で保存され、ヘッダーでは安全なHTTPS URLへ解決して別タブで開く。

## 7. テナントと監査列

各設定Entityは`BaseEntity`を継承し、`tenant_id`、作成・更新日時、論理削除日時等を持つ。主要な一意制約は`tenant_id + setting_code`または`tenant_id + code`である。
