# 年度帳票バックアップ V1

## 1. 目的

会計年度内に生成した月次正式帳票を、通常の帳票履歴とは独立した
書類管理のバックアップ領域へコピーし、原則7年間保持する。

本機能はDBバックアップおよびRDSバックアップとは別機能である。

| 種類 | 対象 | 用途 |
| --- | --- | --- |
| 年度帳票バックアップ | PDF・CSV・Excel等の完成ファイル | 法定・業務上の長期保管とFileManagerからの閲覧 |
| 基盤のDBバックアップ | テーブルデータのCSV・ZIP | データ確認、移行、限定的な復元 |
| RDSバックアップ | MySQLインスタンス全体 | 障害・誤操作からのDB復旧 |

## 2. 保存先

```text
documents/backups/reports/
└── {tenantId}/
    └── {fiscalYear}/
        └── {reportCode}/
            └── {targetMonth}/
                └── v{closingVersion}/
                    └── {sourceFileId}-{fileName}
```

バックアップ領域はFileManagerから閲覧・ダウンロードできるが、
アップロード、名称変更、移動、削除はできない。

## 3. 対象帳票

`monthly_closing_output_definition`で次の条件を満たす帳票を対象とする。

- `output_type = REPORT`
- `active_flag = true`
- `backup_retention_years > 0`

帳票コードをソースコードへ固定しない。管理者メニューの締め帳票設定で
保持年数を設定した帳票が対象になる。

請求書は実ファイル側のコードが
`MONTHLY_INVOICE_PATTERN_1`～`3`であっても、
`MONTHLY_INVOICE`のバックアップ設定を適用する。

## 4. 実行タイミング

24時間稼働を前提にしないため、午前2時などの固定時刻では実行しない。

```text
会計年度終了
  -> 猶予日数経過
  -> 次回アプリケーション起動
  -> 未実行年度を検出
  -> 年度帳票バックアップを実行
```

既定値：

| 設定 | 既定値 |
| --- | ---: |
| 会計年度開始月 | 4月 |
| 年度終了後の猶予日数 | 14日 |
| 起動時キャッチアップ | 有効 |

設定API：

```text
GET  /api/admin/business-settings/annual-report-backup
PUT  /api/admin/business-settings/annual-report-backup
POST /api/admin/business-settings/annual-report-backup/{fiscalYear}/execute
```

すべて`SYS_ADMIN`限定である。

管理画面：

```text
管理者メニュー → 業務管理 → 帳票バックアップ
```

画面から設定保存、対象年度を指定した手動実行、直近の実行結果確認ができる。

## 5. 安全性と再実行

- `tenant_id + fiscal_year`で実行を一意にする
- `tenant_id + monthly_closing_report_file_id`でコピーを一意にする
- バックアップキーは元ファイルIDを含み、同名ファイルを上書きしない
- コピー後にバックアップ先の存在を確認する
- 全対象ファイルの記録件数が一致した場合だけ完了にする
- 途中失敗では元の帳票履歴を削除しない
- 失敗後の再実行ではコピー済みファイルを再利用し、未完了分だけを続行する
- 完了済み年度の再実行は同じ実行結果を返し、二重コピーしない

## 6. バックアップ成功後

全ファイル成功後に次を実行する。

1. `annual_report_backup_execution`を`COMPLETED`にする
2. `annual_report_backup_file`へコピー結果と保持期限を記録する
3. 対象の`report_history`を論理削除する
4. 対象の`monthly_closing_report_files`を論理削除する

元の生成ファイルは、この段階では物理削除しない。
S3バージョニングが有効なため、DB履歴削除とS3完全削除を同じ処理にすると
復旧性を失う可能性があるためである。

## 7. 保持期間

各バックアップファイルへ次を記録する。

```text
retention_until = 会計年度末 + backup_retention_years
```

V1の月次正式帳票は原則7年とする。S3ライフサイクルは
`documents/backups/reports/`だけを対象に、2557日経過後に期限切れとする。

S3バージョニングが有効なため、現行オブジェクトの期限切れ後、
非現行Versionをさらに30日保持してから完全削除する。DBには暦年ベースの
`retention_until`も記録し、業務上の保持期限とS3費用制御を分離する。

## 8. テスト

`AnnualReportBackupContainerIntegrationTest`で次を確認する。

- 猶予期間前の実行拒否
- 会計年度開始月から年度末までの対象選定
- バックアップ設定のある帳票だけをコピー
- 2つの対象月・異なる締めVersionの保持
- 7年後の保持期限記録
- 全件成功後の帳票履歴論理削除
- 元ファイルを物理削除しないこと
- 完了済み年度の再実行で二重コピーしないこと
- 起動時キャッチアップ対象から完了年度を除外すること

AWS、RDS、S3へは接続せず、MySQL Testcontainerとローカル一時ストレージで確認する。
