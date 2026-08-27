# 月次締め・帳票・台帳基盤 V1

## 1. 目的

月次締め時点の確定データと、日報修正などを反映する最新データを混同せず、
PDF・CSV・Spreadsheet台帳をVersion単位で再現できるようにする。

## 2. 基本原則

| 資産 | 役割 |
| --- | --- |
| View | 常に最新の業務データ。結合・集計・計算を完了した状態で公開する |
| inputTable | `execution_id`、対象月、Versionなどの実行条件 |
| historyTable | 締め時点の確定スナップショット。帳票ごとに物理テーブルを持つ |
| outputTable | ファイル生成用の作業データ。historyTableから作成する |
| S3ファイル | 利用者が確認・印刷・ダウンロードする完成物 |

正式帳票は次の順で生成する。

```text
View
  -> ストアド
  -> historyTable
  -> outputTable
  -> PDF / CSV
  -> S3
  -> report_history / monthly_closing_item
```

`View -> historyTable`と`View -> outputTable`を別々に実行しない。
完成ファイルと確定データの不一致を防ぐため、outputTableはhistoryTableから作成する。

現在のV1月次締めは、次の有効な帳票を自動実行する。

| 帳票 | jobCode | 完成形式 |
| --- | --- | --- |
| 月次給与明細 | `PRINT_MONTHLY_PAY_SLIP` | PDF |
| 月次労務費一覧 | `PRINT_MONTHLY_LABOR_COST_LIST` | Excel |
| 月次請求書 | `PRINT_MONTHLY_INVOICE` | 顧客別PDF |

月次請求書は顧客マスターの`invoice_type`から、3種類のJasperテンプレートを解決する。
対象期間の`vw_monthly_invoice_latest_detail`に明細が存在する顧客だけを出力対象とする。取引のない顧客へ空の請求書や顧客取引を作成しない。
PDF・CSV・Excelとして登録された帳票は、バッチ成功だけでなく、保存先・ファイル名・ファイルサイズが返ることを締め完了条件とする。有効な月次帳票が0件の場合も締めを失敗させる。

## 3. 日次と月次

### 3.1 日次帳票

- 確定処理を持たない
- 常に最新Viewを使用する
- 正式なファイル出力が必要な場合は`View -> outputTable -> 帳票`
- 日次給与明細などの正式出力は、出力時点のファイルを`report_history`へ保存する
- 正式出力をやり直した場合は上書きせず、同じ業務キーの別Versionとして残す
- 日次給与明細はバックアップ対象とし、原則7年間保持する
- 一時確認だけの場合は`View -> Thymeleaf -> iframe`
- ブラウザ印刷は`window.print()`を利用し、履歴を作らない

### 3.2 月次正式帳票

- 締め時にViewからhistoryTableへ確定データを保存する
- HTMLプレビューもhistoryTableから生成する
- PDF・CSVとHTMLプレビューは同じVersionを使用する
- 月次で履歴管理するPDF・CSVは原則7年間バックアップする

### 3.3 月次台帳

- 日々の確認時は最新ViewからSpreadsheet JSONを再生成できる
- 日報保存ごとの自動生成は行わない
- 台帳画面の表示・更新操作で再生成する
- 月次締め時に必ず最終生成してVersionを確定する
- 月間労務表、労務費一覧、入金確認表、月間集計表を台帳基盤で扱う

## 4. Versionと再処理

| 操作 | Version | 取得元 | 用途 |
| --- | ---: | --- | --- |
| 初回締め | v1 | 最新View | 最初の確定 |
| 失敗再実行 | 同じVersion | historyTable | ファイル生成などの復旧 |
| 再締め | 現在Version + 1 | 最新View | 修正後の再確定 |
| 過去表示 | 指定Version | S3 | 過去帳票・台帳の確認 |

失敗再実行でViewを読み直してはいけない。同じVersionの内容が変わるためである。
日報修正を取り込む場合は再締めを実行し、新しいVersionを作成する。

## 5. 状態

月次締め全体と各出力項目は、少なくとも次の状態を持つ。

```text
OPEN
PROCESSING
CLOSED
FAILED
```

- 必須項目が1件でも失敗した場合、月次締め全体を`CLOSED`にしない
- 帳票ファイル生成または顧客取引同期に失敗した場合も`CLOSED`にしない
- 成功済みファイルとhistoryTableは削除しない
- 同じVersionの再実行では失敗項目だけを再処理する
- 任意項目の失敗を締め失敗にするかは締め対象マスタの`required_flag`で決める

## 6. 月次締め対象マスタ

```text
monthly_closing_output_definition
```

| カラム | 内容 |
| --- | --- |
| `output_type` | `REPORT` / `LEDGER` |
| `output_code` | `report_code`または`book_code` |
| `execution_order` | 実行順 |
| `required_flag` | 締め完了に必須か |
| `active_flag` | 有効フラグ |
| `backup_retention_years` | バックアップ保持年数。月次正式帳票は原則7 |

日次の一時プレビュー印刷は月次締め対象へ登録しない。

## 7. 帳票マスタ

月次正式帳票は帳票ごとに次を明示する。

| 設定 | 内容 |
| --- | --- |
| `source_view_name` | 最新データを返す許可済みView |
| `input_table` | 実行条件テーブル |
| `output_table` | ファイル生成作業テーブル |
| `history_table` | Version付き確定テーブル |
| `html_template_key` | S3上のThymeleafテンプレート |
| `html_template_version` | テンプレートVersion |

View名・テーブル名を画面リクエストから自由入力させない。
帳票マスタおよびデータソースカタログで許可した名前だけを利用する。

## 8. S3 HTMLテンプレート

```text
documents/templates/reports/html/{reportCode}/v{version}/template.html
```

- 管理画面から編集しない
- 配布スクリプトまたは限定IAM権限で登録する
- 帳票履歴へテンプレートVersion、S3キー、ハッシュ値を保存する
- Thymeleafへ渡す変数は`definition`、`rows`、`summary`、`parameters`、`metadata`

## 9. HTMLプレビュー

| 種類 | データ取得元 | 履歴 |
| --- | --- | --- |
| 月次正式帳票 | 指定VersionのhistoryTable | 既存履歴を参照 |
| 日次一時プレビュー | 最新View | 作成しない |

ブラウザ印刷は出力形式ではなく、HTMLプレビューに付与する画面機能とする。
日別HTMLプレビューの認証、S3テンプレート、View、マスターの詳細は、
`docs/kb/daily-html-preview-report-foundation.md`を参照する。

## 9.1 保存済み月次帳票の確認・印刷

月次PDFは締め時に生成した完成ファイルを正本として、次の順で確認・印刷する。

```text
印刷を選択
  -> 対象月・closingVersion・reportCode・targetをバックエンドへ送信
  -> monthly_closing_itemの保存済みファイル情報を検索
  -> バックエンドが権限とtenantを検証
  -> S3からファイルを取得
  -> PDFプレビュー
  -> ブラウザ印刷
```

- クライアントから任意のS3キーを指定させない
- 通常画面は最新の成功Versionを選択する
- 履歴画面では過去Versionを選択できる
- プレビュー・ブラウザ印刷では新しい帳票履歴を作らない
- 再生成が必要な場合は「再印刷」ではなく「再締め」または「失敗再実行」を使う
- CSVはブラウザ印刷ではなく、保存済みファイルのダウンロードを基本とする

## 9.2 日次給与明細の履歴・バックアップ

日次給与明細は月次締めVersionを使用せず、次の業務キーで出力履歴を管理する。

```text
DAILY_PAY_SLIP:{paymentDate}:{employeeId}
```

- 出力ごとにVersionを採番する
- S3ファイル、`report_history`、`report_output_file`を関連付ける
- 同じ支払日・従業員で再出力しても、過去Versionを上書きしない
- 年度バックアップでは成功した全Versionを対象にする
- 一時プレビュー印刷は日次給与明細の正式出力履歴とは別扱いにする

## 9.3 将来のメール送信

帳票生成とメール送信は分離する。メール送信のためにViewやストアドを再実行せず、
利用者が指定した保存済み帳票Versionを添付する。

```text
View / historyTable
  -> PDF生成
  -> S3保存
  -> report_history / report_output_file
  -> mail_queue
  -> メール送信
```

メール対応の有無は出力形式へ含めず、別の配信定義で管理する。

```text
report_delivery_definition
```

| 設定 | 内容 |
| --- | --- |
| `report_code` | 対象帳票 |
| `delivery_type` | `NONE` / `EMAIL` |
| `trigger_type` | `MANUAL` / `AFTER_OUTPUT` / `AFTER_MONTHLY_CLOSE` |
| `recipient_resolver_code` | 従業員・顧客・請求先担当者などの宛先解決方式 |
| `mail_type` | メール業務種別 |
| `mail_template_key` | メッセージテンプレート |
| `attachment_version_policy` | 指定Version / 最新成功Version |
| `active_flag` | 有効フラグ |

- 従業員のメールアドレスを帳票サービスへ直書きしない
- 宛先は`recipient_resolver_code`に対応するマスター駆動のResolverで取得する
- 送信キュー作成時に宛先アドレスと添付Versionをスナップショット保存する
- `mail_queue`はS3キーではなく、可能な限り`report_output_file_id`を参照する
- 添付ファイルは保存時のハッシュ値を検証してから送信する
- 重複送信は`business_key + output_version + mail_type + recipient_key`で防止する
- 再送は同じ帳票Versionを使用し、新しいPDFを生成しない

対象例：

| 帳票 | ファイル単位 | 宛先Resolver |
| --- | --- | --- |
| 日次給与明細 | 支払日・従業員・Version | 従業員 |
| 月次給与明細 | 対象月・従業員・締めVersion | 従業員 |
| 請求書 | 対象月・顧客・締めVersion | 顧客請求先 |

## 10. 段階移行

1. 月次締め実行・項目・対象マスタを追加（実装済み）
2. 帳票マスタへView、historyTable、HTMLテンプレート情報を追加（実装済み）
3. ストアドを`View -> historyTable -> outputTable`へ統一
4. 台帳を締めVersion付きS3キーへ対応（実装済み）
5. `operation/reportpreview`の共通描画処理を`system/report`へ移動
6. 旧定義を新しい締め対象マスタへ移行
7. 互換APIを確認後に旧処理を削除

初期DDL：

```text
backend/src/main/resources/sql/operation/monthly/closing_output_foundation_v1.sql
backend/src/main/resources/sql/system/report/monthly_snapshot_foundation_v1.sql
```

月次請求書3パターンのDB・ストアド・Jasper資産と適用手順は、
`docs/kb/monthly-invoice-jasper-foundation.md`を参照する。

追加済みの管理テーブル：

```text
monthly_closing_execution
monthly_closing_output_definition
monthly_closing_item
```

`MonthlyClosingJobService`は`monthly_closing_output_definition`に登録された有効な月次帳票と台帳を実行する。
`REPORT`は帳票基盤、`LEDGER`はSpreadsheet台帳基盤へ委譲する。台帳は初回締めを`v1`、再締めを`v2`以降として、次のVersion付きキーへ確定保存する。

```text
documents/generated-reports/ledgers/{tenantId}/{bookCode}/{yyyy-MM}/closing/v{closingVersion}/...
```

対象別台帳は、締め時に選択候補を全件解決し、従業員・顧客などの対象ごとに1ファイルを生成する。月間集計表や入金確認表の締め前手入力値は、通常の月次作業ファイルから確定Versionへ引き継ぐ。

`monthly_closing_output_definition`／`monthly_closing_item`による項目単位の失敗再実行は次工程で接続する。V1の初回締め・再締めでは、すべての有効帳票と顧客取引同期が成功した後だけ`monthly_closings.status`を`CLOSED`へ更新する。

ストアドは`execution_id`だけを引数に受け取り、inputTableから
`tenant_id`、`target_month`、`closing_version`、`execution_mode`を取得する。
`INITIAL`と`RECLOSE`は最新ViewからhistoryTableを作成し、
`RETRY`はhistoryTableを変更せずoutputTableだけを再構築する。

## 10.1 顧客取引への請求額同期

月次請求書を全顧客分生成した後、同じ締めVersionの確定請求履歴から顧客取引を作成・更新する。

```text
monthly_invoice_history.total_amount
  -> CustomerTransactionCommandService.upsertFromMonthlyClosing
  -> customer_transactions.billing_amount
```

Viewの最新値やPDFの解析結果から請求額を作らない。請求書と顧客取引は、必ず同じ`monthly_invoice_history`を正本とする。

| 条件 | 再締め時の処理 |
| --- | --- |
| 未入金 | 最新Versionの請求額・入金予定日へ更新 |
| 一部入金 | 請求額を更新し、入金額・手数料・相殺から状態を再計算 |
| 入金済み／過入金 | 金額不整合を防ぐため再締めを失敗させる |

重複防止単位：

```text
tenant_id + customer_id + target_month
```

取引には次の追跡情報を保存する。

```text
source_type = MONTHLY_CLOSING
source_invoice_history_id
source_closing_version
```

顧客マスターの締日・支払日ルールも取引へスナップショットし、`expected_payment_date`は対象月と支払日ルールから計算する。

追加DDL：

```text
backend/src/main/resources/sql/operation/monthly/customer_transaction_sync_v1.sql
```

## 11. 月次給与明細

### 11.1 業務前提

- 全従業員を月給者として月次給与明細を生成する
- 日次の支払いは日給ではなく、月給の前払いとして扱う
- 月次給与明細の`前払い`は、対象期間内の`daily_payments`のうち
  `status = PAID`となった`actual_amount`の合計
- `planned_amount`や`PENDING`は月次給与明細の控除へ含めない
- 日次前払いの予定額は月給から自動算出しない。会社の前払い計算ルールまたは
  管理者入力で決め、支払確定後の`actual_amount`だけを月次精算へ使う
- 月次給与明細は対象月・従業員・締めVersionごとに履歴を保持する
- 完成PDFは7年バックアップ対象とする

### 11.2 原本項目

原本には次の項目がある。

| 区分 | 主な項目 |
| --- | --- |
| 勤怠 | 勤務日数、早出残業時間、深夜時間 |
| 支給 | 基本給、早出残業手当、深夜手当、勤務態度手当、運転手当、管理手当、可変手当 |
| 控除1 | 健康保険、子ども・子育て支援金、厚生年金、雇用保険、所得税、住民税、可変法定控除 |
| 控除2 | 前払い、寮費、携帯電話貸出料、Wi-Fi使用料、貯金額、借入金返済額、可変控除 |
| 合計 | 総支給額、総控除1額、総控除2額、法定預り返金額、差引支給額 |

空欄になっている項目名セルは、企業ごとの可変手当・控除を出すための領域として扱う。

### 11.3 View構成

月次給与明細の最新値は次のViewで構成する。

```text
vw_monthly_pay_slip_employee_month
  + vw_monthly_pay_slip_attendance
  + vw_monthly_pay_slip_advance
  + vw_monthly_pay_slip_variable_item_source
  + vw_monthly_pay_slip_variable_item
  + vw_monthly_pay_slip_company
  -> vw_monthly_pay_slip_latest
```

`vw_monthly_pay_slip_latest`は`tenant_id + target_month + employee_id`で1行を返す。
対象月は`monthly_closings`を基準にするため、日報が0件の月給者も対象にできる。
会社名は`company_profile`の有効なマスターから取得し、締め時点の名称を
`monthly_pay_slip_history.company_name`へ保存する。JRXMLへ会社名を直書きしない。
会社プロフィールが未登録の場合は、締めストアドを失敗させる。

初期DDL：

```text
backend/src/main/resources/sql/system/report/pay_slip/monthly_pay_slip_view_foundation_v1.sql
```

### 11.4 可変項目

可変項目は次の3区分に分け、マスターの`display_order`とコードで順序を固定する。

```text
ALLOWANCE
LEGAL_DEDUCTION
OTHER_DEDUCTION
```

Window関数の`ROW_NUMBER()`で区分ごとの`item_no`を採番し、
最終Viewでは次の連番カラムへ展開する。

```text
allowance_item_name_01 / allowance_item_value_01 ... 12
legal_item_name_01     / legal_item_value_01     ... 12
other_item_name_01     / other_item_value_01     ... 12
```

汎用の`item_name1`だけにせず区分をカラム名へ含める。
同じ番号の手当と控除を誤って混在させないためである。

各区分の件数は次のカラムで返す。

```text
allowance_item_count
legal_deduction_item_count
other_deduction_item_count
```

いずれかが12を超えた場合、締め処理は失敗させる。
13件目以降を帳票から黙って欠落させてはいけない。
枠数を増やす場合はView、historyTable、outputTable、帳票テンプレートを同時に変更する。

### 11.5 計算できる範囲

現行データからViewだけで安全に計算できる項目：

- 月給契約の基本給
- 承認済み日報の勤務日数、勤務・残業・深夜・休日時間
- 日報へ確定保存された手当明細
- 日報へ確定保存された控除明細
- 前払い実績
- 貯金額、借入金返済額
- 従業員給与プロファイルの月額住民税

Viewへ接続済みの項目：

- 標準報酬月額
- 健康保険
- 子ども・子育て支援金
- 厚生年金
- 雇用保険
- 社会保険料等控除後の給与額
- 甲欄の所得税

現段階で確定計算として扱わない項目：

- JEXL、MVEL、Java Beanで月次実行する`AUTO`手当・控除
- 乙欄・丙欄の所得税
- 介護保険
- 法定預り返金額

MySQL ViewからJavaのRuleエンジンは実行できない。
中間結果テーブルを作らずView中心にする場合、上記は次のどちらかでSQL化する。

1. 税・保険マスターを参照する専用Viewまたは決定的なSQL関数
2. Ruleに`SQL_VIEW`方式を追加し、許可済みViewだけを参照する

V1では1を採用した。税・保険計算を汎用JEXLへ寄せるより、
適用年、等級、料率、端数処理をSQL資産として明示した方が検証しやすい。
JEXL、MVEL、Java Beanは日報入力時などJava側で実行し、
確定額を日報明細へ保存した項目だけを月次Viewで集計する。

`vw_monthly_pay_slip_tax_calculation`は、不足マスターを0円として確定しない。
次のいずれかに該当すると`calculation_ready = false`と
`calculation_error_code`を返し、締めストアドが処理を中止する。

| エラーコード | 内容 |
| --- | --- |
| `PAYROLL_PROFILE_MISSING` | 従業員給与プロファイルがない |
| `CALCULATION_PERIOD_MISSING` | 対象月の計算期間マスターがない |
| `CALCULATION_PERIOD_NOT_VERIFIED` | 対象月の税・保険マスターが確認済みではない |
| `ROUNDING_MODE_UNSUPPORTED` | 未対応の端数処理方式 |
| `CARE_INSURANCE_RATE_MISSING` | 介護保険対象だが対象版の料率がない |
| `STANDARD_REMUNERATION_MISSING` | 対象月に有効な従業員別標準報酬月額がない |
| `STANDARD_REMUNERATION_OVERLAPPED` | 標準報酬月額の適用期間が重複している |
| `HEALTH_INSURANCE_RATE_MISSING` | 対象年の健康保険料率がない |
| `PENSION_INSURANCE_RATE_MISSING` | 対象年の厚生年金料率がない |
| `EMPLOYMENT_INSURANCE_RATE_MISSING` | 対象年の雇用保険料率がない |
| `CHILD_CARE_SUPPORT_RATE_MISSING` | 適用対象だが支援金率がない |
| `INCOME_TAX_CATEGORY_UNSUPPORTED` | 甲欄以外の所得税計算 |
| `INCOME_TAX_BRACKET_MISSING` | 対象年・扶養人数・課税額に一致する税額表がない |

税・保険マスターは対象年ごとに公式資料から取り込み、締め前に
全従業員の`calculation_ready = true`を確認する。
既存の`sql/tax/income_tax_table_init.sql`と
`sql/tax/insurance_rate_master_init.sql`は旧簡易データであり、
本番給与計算へ使用しない。

対象月ごとに次のマスターを登録する。

```text
payroll_calculation_period
```

| カラム | 内容 |
| --- | --- |
| `target_month` | 給与対象月の月初日 |
| `income_tax_year` | 使用する源泉徴収税額表の年 |
| `insurance_rate_year` | 使用する保険料率マスターの年／版 |
| `child_care_support_required` | 支援金計算の適用有無 |
| `rounding_mode` | V1は`HALF_UP`のみ |
| `verified_flag` | 公式資料との確認完了 |
| `source_note` | 資料名、版、確認メモ |

保険料率の年度切替日をViewへハードコードしない。
対象月がどのマスター版を使うかは`payroll_calculation_period`で明示する。

従業員別の標準報酬月額は次の履歴マスターに保持する。

```text
employee_standard_remuneration
```

| カラム | 内容 |
| --- | --- |
| `effective_from` / `effective_to` | 適用期間 |
| `health_standard_remuneration` | 健康保険の標準報酬月額 |
| `pension_standard_remuneration` | 厚生年金の標準報酬月額 |
| `source_type` | 資格取得時決定、定時決定、随時改定など |

その月の総支給額から標準報酬月額を毎月引き直してはいけない。
決定・改定済みの適用期間付きマスターを参照する。

### 11.6 スナップショット

月次給与明細の締め処理は次の順で行う。

```text
INITIAL / RECLOSE
  vw_monthly_pay_slip_latest
    -> monthly_pay_slip_history
    -> monthly_pay_slip_history_item
    -> monthly_pay_slip_output
    -> PDF
    -> S3

RETRY
  monthly_pay_slip_history
    + monthly_pay_slip_history_item
    -> monthly_pay_slip_output
    -> PDF
    -> S3
```

`monthly_pay_slip_history_item`は計算用の中間テーブルではない。
可変項目名・金額・表示順を締めVersionごとに再現するための正式な履歴明細である。
再締め時だけ最新Viewを読み、失敗再実行では同じ履歴を使う。

実装DDL：

```text
backend/src/main/resources/sql/system/report/pay_slip/monthly_pay_slip_snapshot_foundation_v1.sql
```

ストアド：

```text
sp_monthly_pay_slip_snapshot(execution_id)
sp_monthly_pay_slip_cleanup(execution_id)
```

正式履歴：

```text
monthly_pay_slip_history
monthly_pay_slip_history_item
```

ファイル生成用：

```text
monthly_pay_slip_render_output
monthly_pay_slip_render_output_item
```

履歴と出力作業テーブルでは可変項目を正規化して保持する。
既存の帳票レンダラーは`JRMapCollectionDataSource`を使用し、
JasperReportsへDB接続を渡さないため、描画時だけ次のViewで最大12枠へ展開する。

```text
vw_monthly_pay_slip_render_flat
```

帳票マスタの`query_sql`はこのViewを`execution_id`で絞り込む。
後から手当・控除を追加しても履歴テーブルのDDL変更は不要であり、
13項目以上へ拡張するときだけViewとJRXMLを同時に変更する。

### 11.7 DDL適用順

```text
1. system/report/monthly_snapshot_foundation_v1.sql
2. system/report/pay_slip/setup.sql
3. system/report/pay_slip/monthly_pay_slip_view_foundation_v1.sql
4. system/report/pay_slip/monthly_pay_slip_snapshot_foundation_v1.sql
```

新規環境では、テーブル作成とマスター投入を分離した統合DDLへ整理してから適用する。
現状の`setup.sql`は既存環境との互換性のため、旧テーブル作成も残している。

### 11.8 検証結果

MySQL 8.4の使い捨てDBで次を確認した。

1. DDLを記載順に新規適用できる
2. 架空の月給者について最新Viewを生成できる
3. `PAID.actual_amount`が月次の前払いへ集計される
4. `INITIAL`でv1履歴と出力データを作成できる
5. v1確定後に前払い実績を変更しても、`RETRY`はv1の金額を維持する
6. `RECLOSE`は変更後の最新Viewをv2へ保存する

確認例：

| 操作 | 最新の前払い | 出力Version | 出力された前払い |
| --- | ---: | ---: | ---: |
| INITIAL | 20,000 | v1 | 20,000 |
| 日次支払を25,000へ変更後にRETRY | 25,000 | v1 | 20,000 |
| RECLOSE | 25,000 | v2 | 25,000 |

これにより、失敗再実行による確定履歴の変質を防ぎつつ、
日報・支払修正は再締めVersionへ取り込めることを確認した。

### 11.9 JasperReportsテンプレート

月次給与明細の配布元テンプレート：

```text
backend/src/main/resources/reports/monthly_pay_slip.jrxml
```

日本語フォント：

```text
backend/src/main/resources/fonts/ipaexg.ttf
backend/src/main/resources/fonts/ipaex.xml
backend/src/main/resources/jasperreports_extension.properties
```

原本の印刷設定に合わせ、帳票はB5横、従業員1名につき1ページで構成する。

| 領域 | 内容 |
| --- | --- |
| ヘッダー | 対象月、氏名、会社名、従業員コード、締め期間、締めVersion |
| 勤怠 | 勤務日数、実働、残業、深夜、休日、有休 |
| 支給 | 基本給と可変手当12枠 |
| 控除1 | 社会保険、税、可変法定控除12枠 |
| 控除2 | 前払いを含む可変控除12枠 |
| 合計 | 総支給、控除1、控除2、控除合計、差引支給 |

起動時の`BundledReportTemplateInitializer`は、
利用中のストレージにテンプレートが存在しない場合だけ配布元を登録する。

```text
LOCAL: {local-base-path}/reports/monthly_pay_slip.jrxml
S3:    s3://{document-bucket}/reports/monthly_pay_slip.jrxml
```

既存のS3テンプレートは自動上書きしない。
テンプレート改訂時はS3キーまたはVersion管理方式を決め、明示的に配布する。

自動テスト：

```text
MonthlyPaySlipJasperTemplateTest
BundledReportTemplateInitializerTest
```

確認結果：

1. JRXMLをJasperReports 6.21.3でコンパイルできる
2. IPAexGothicをPDFへ埋め込み、日本語が文字化けしない
3. 従業員2名の入力から2ページのPDFを生成できる
4. 支給・控除・合計がB5横1ページへ収まる

サンプルPDF：

```text
output/pdf/monthly_pay_slip-sample.pdf
```

## 12. 日次支払明細

### 12.1 業務単位

- 日次支払は月給の前払いとして扱う
- `payment_date + employee_id`を1明細とする
- 同じ支払日に複数の日報が含まれる場合は、View内で集計する
- `daily_payments.actual_amount`を当日の確定支払額とする
- 正式PDFは`report_history`とS3へ保存し、年度バックアップで7年保持する
- 一時プレビューと正式出力を混同しない

### 12.2 データ生成

```text
daily_report / daily_report_allowances / daily_report_deductions
daily_payments / employee
  -> vw_daily_pay_slip_item_source
  -> vw_daily_pay_slip_item_ranked
  -> vw_daily_pay_slip_work_summary
  -> vw_daily_pay_slip_latest
  -> sp_daily_pay_slip_prepare(execution_id)
  -> daily_pay_slip_output
  -> JasperReports
  -> S3 / report_history
```

手当・控除はコード順に並べ、各10項目まで帳票へ展開する。
11項目以上を許可する場合は、View、outputTable、JRXMLを同時に拡張する。

使用資産：

```text
backend/src/main/resources/sql/system/report/pay_slip/daily_pay_slip_view_foundation_v1.sql
backend/src/main/resources/sql/system/report/pay_slip/daily_pay_slip_stored.sql
backend/src/main/resources/sql/system/report/pay_slip/daily_pay_slip_table.sql
backend/src/main/resources/reports/daily_pay_slip.jrxml
```

### 12.3 用紙・ページ構成

原本の印刷設定に合わせ、B5縦1ページへ2名分を上下に配置する。

| 入力人数 | PDFページ数 | 最終ページ |
| ---: | ---: | --- |
| 1名 | 1 | 上段のみ |
| 2名 | 1 | 上段・下段 |
| 3名 | 2 | 2ページ目は上段のみ |
| n名 | `ceil(n / 2)` | 奇数時は下段が空欄 |

月次給与明細とは色・罫線・合計欄のデザインを共通化するが、
ページ制御は共有しない。

### 12.3.1 画面プレビューと本印刷

日次管理の帳票一覧で「日払い明細」を選択すると、
`vw_daily_pay_slip_latest`の最新データを使ったHTMLプレビューを表示する。
このプレビューは金額・手当・控除を印刷前に確認するためのもので、
履歴保存およびブラウザ印刷の対象にはしない。

「印刷」を押したときだけ帳票基盤を実行し、JasperReportsのB5 PDFを生成する。
生成したPDFはPDFプレビューダイアログで確認してから本印刷し、
ファイルと`report_history`を保存する。

```text
帳票行クリック
  -> ViewからHTMLプレビュー（保存なし）
  -> 印刷
  -> View -> inputTable -> ストアド -> outputTable
  -> JasperReports PDF
  -> PDFプレビュー
  -> 帳票履歴・ストレージ保存
```

HTMLテンプレートは次を使用する。

```text
documents/templates/reports/html/DAILY_PAY_SLIP/v2/template.html
```

### 12.4 DDL適用順

新規環境または日次支払明細を作り直せる開発環境では、次の順で適用する。

```text
1. system/report/pay_slip/daily_pay_slip_table.sql
2. system/report/pay_slip/daily_pay_slip_view_foundation_v1.sql
3. system/report/pay_slip/daily_pay_slip_stored.sql
4. system/report/pay_slip/setup.sql
```

`daily_pay_slip_table.sql`は作業テーブルを再作成するため、
既存環境へ適用する前に対象テーブルが一時データだけであることを確認する。

全員分を一括生成する場合は`employeeId`を指定しない。このため、
`daily_pay_slip_input.employee_id`はNULL許可が必須である。
`setup.sql`には既存環境を修復する非破壊の`ALTER TABLE`を含める。
AWS反映後はランタイムスキーマ更新スクリプトが、NULL許可、View、ストアド、
帳票マスター、バッチ定義をまとめて検証する。

### 12.5 自動検証

```text
DailyPaySlipJasperTemplateTest
MonthlyPaySlipJasperTemplateTest
BundledReportTemplateInitializerTest
```

確認結果：

1. 月次はB5横・1名／ページ
2. 日次はB5縦・2名／ページ
3. 日次3名の入力から2ページを生成
4. 日本語フォントを埋め込み
5. 配布元の月次・日次テンプレートをストレージへ初期登録可能
6. MySQL 8.4で日次Viewとストアドを新規適用可能
7. 支払日が同じ3名について、手当・控除を含む3件のoutputを生成可能

サンプルPDF：

```text
output/pdf/monthly_pay_slip-sample.pdf
output/pdf/daily_pay_slip-sample.pdf
```

## 13. 帳票・台帳連携の最終確認（2026-08-27）

- 月次締めは帳票定義だけでなく、有効な台帳定義も取得して生成する。
- 台帳の確定ファイルは締めVersionを含む別キーへ保存し、再締めで過去Versionを上書きしない。
- 帳票ファイルも初回締めと再締めの履歴・実ファイルを両方保持する。
- 管理画面用テンプレートと締め処理の生成元は同じマスターを参照する。
- コード生成台帳とテンプレート台帳を`rendererKey`で切り替え、画面側の個別ハードコードを不要にする。

Testcontainersで、帳票の初回締め・再締め・履歴Version・ファイル保持と、台帳のView取得・テンプレート展開・保存を確認済みである。
