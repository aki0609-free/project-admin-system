# 月次締め 項目用途・システム連携 V1

## 1. `monthly_closings`

| 項目 | 用途 |
|---|---|
| `target_month` | 自社締め対象月。月初日で保存 |
| `status` | `OPEN/PROCESSING/CLOSED/FAILED` |
| `closing_version` | 最後に正常完了したVersion |
| `closing_start/end_date` | その実行で使う会社給与締め期間Snapshot |
| `closing_rule_type/value` | 締日設定Snapshot |
| `closed_at/by` | 正常完了日時・実行ユーザー |
| `note` | 失敗時エラー等 |

## 2. `monthly_closing_execution`

締めの試行単位でVersion、状態、開始/完了時刻、実行者、エラーを保存する。失敗Versionも残すため、次Versionは「完了Version+1」ではなく既存試行を含む最大Version+1となる。

## 3. `monthly_closing_output_definition`

| 項目 | 用途 |
|---|---|
| `output_type` | `REPORT`または`LEDGER` |
| `output_code` | report codeまたはbook code |
| `execution_order` | 締め内の実行順 |
| `required_flag` | 完了必須か |
| `active_flag` | 実行対象 |
| `backup_retention_years` | 保持年数設定 |

請求書・注文書コードは`findActiveCompanyOutputs`で除外し、顧客請求締めからのみ生成する。

## 4. `monthly_closing_item`

出力定義ごとの状態・Target・history件数/表・ファイルmetadata・エラーを保持できる構造である。現行開始時の`target_key`は`ALL`である。

## 5. `monthly_closing_report_files`

REPORTで生成したファイルのVersion、report code、対象、Batch log、Storage key/name/type、content type、size、生成日時を保持する。自社月次はscope=`COMPANY`である。

## 6. 月次給与明細

月次Viewが日報・控除手当・税保険・残高取引を`SUM/LAST/BALANCE/TAX/FIXED`として集計し、締めJobがhistoryへVersion確定する。月次給与明細はhistoryを表示し、日報マスターの表示設定を直接使わない。

詳細は[月次給与明細の計算・表示・履歴確定フロー](monthly-pay-slip-item-flow-v1.md)を参照する。

## 7. 再締め

再締めは現在のViewとマスター、現在の締日設定から再計算し、新Versionのhistory・ファイル・台帳を生成する。旧Versionは上書きしない。画面の通常印刷は選択Versionの保存ファイルを取得する。

## 8. 関連機能

- 業務管理：会社給与締日、月次出力定義
- 日報：日次計算結果・根拠
- 控除手当/取引：期間合計・残高
- 帳票管理：View、Stored Procedure、history/output、Exporter
- 台帳管理：Spreadsheet確定版
- Backup：保持年数に応じた年度バックアップ
