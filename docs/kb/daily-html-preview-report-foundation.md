# 日別HTMLプレビュー帳票基盤 V1

## 1. 目的

履歴・バックアップを必要としない日別帳票を、最新の業務Viewと
S3上のThymeleafテンプレートから生成し、画面内のiframeで確認する。

印刷可能な帳票は、PDFを新規生成せずブラウザ標準印刷を利用する。

## 2. 対象帳票

| 帳票コード | 帳票名 | 出力区分 | 履歴 | 印刷ボタン |
| --- | --- | --- | --- | --- |
| `DAILY_LABOR_COST_PREVIEW` | 日別労務費一覧 | `HTML_PREVIEW` | なし | なし |
| `DAILY_PAYMENT_PREPARATION` | 給与支払表 | `HTML_PRINT` | なし | あり |

`HTML_PREVIEW`でも利用者自身がブラウザメニューを操作することは制限しない。
システム画面として印刷ボタンを提供するかどうかを出力区分で制御する。

## 3. 処理フロー

```text
最新の承認済み日報・日次支払
  -> 日別帳票専用View
  -> tenant_id + target_dateで取得
  -> S3からHTMLテンプレート取得
  -> Thymeleaf Template Engine
  -> 認証済みAPIレスポンス
  -> VueがHTML文字列を取得
  -> iframe srcdocへ表示
  -> 必要な帳票だけwindow.print()
```

HTML帳票は次の資産を作成しない。

- `report_history`
- `report_output_file`
- S3完成PDF
- 年度バックアップ

正式な履歴が必要になった場合は、HTML印刷を流用せず、JasperReportsなどの
正式帳票基盤へ別帳票コードとして追加する。

## 4. セキュリティ

以前はiframeの`src`から直接HTML APIを開いていたため、JWTを送信できず、
HTMLエンドポイントだけが`permitAll`になっていた。

V1では次の方式へ変更した。

```text
Vue
  -> Authorization: Bearer ...付きでHTMLを取得
  -> 取得済みHTMLをiframeのsrcdocへ設定
```

- `/api/operation/report-previews/html`は認証必須
- APIには`X-Tenant-ID`を付与
- SQLは必ず`tenant_id`で絞り込む
- iframeには`allow-same-origin allow-modals`だけを許可
- S3 HTMLは管理されたテンプレート領域以外から読み込まない
- View名と絞込列名はDBマスターから取得し、安全な識別子形式を検証する
- 画面から任意のView名・列名・S3キーを送信させない

## 5. S3 HTMLテンプレート

### 5.1 保存規則

```text
documents/templates/reports/html/{reportCode}/v{version}/template.html
```

現在のキー：

```text
documents/templates/reports/html/DAILY_LABOR_COST_PREVIEW/v1/template.html
documents/templates/reports/html/DAILY_PAYMENT_PREPARATION/v1/template.html
```

### 5.2 初期配布

アプリ起動時、S3またはローカルストレージにテンプレートが存在しない場合だけ、
デプロイ資産から帳票定義のVersionを登録する。

既に存在するテンプレートは上書きしない。このため、Jasperテンプレートと同様に
S3上で調整したレイアウトを保持できる。

初期配布元：

```text
backend/src/main/resources/templates/operation/reportpreview/daily_labor_cost.html
backend/src/main/resources/templates/operation/reportpreview/daily_payment_preparation.html
```

### 5.3 ハッシュ検証

`operation_report_preview.html_template_hash`にSHA-256を設定した場合、
取得したテンプレートのハッシュが一致しなければ描画を停止する。

初期版のマスタSQLではハッシュを`NULL`としている。レイアウト確定後にS3資産の
SHA-256を採取し、マスターへ設定する。

### 5.4 業務管理からの登録・差し替え

`管理者メニュー → 業務管理 → プレビュー帳票`で、次を登録・更新できる。

- 表示する処理（日次、月次、翌日準備、台帳）
- 帳票コード・帳票名
- データ元View/Table
- 対象日・月の絞込列
- 並び順・画面表示順・有効状態
- `HTML_PREVIEW`／`HTML_PRINT`
- Thymeleaf HTMLテンプレート

通常の帳票管理とは分離し、表示カラム定義を追加しない。専用HTMLはViewの行一覧を
`rows`として直接参照するため、レイアウト調整のたびにJava Rendererを追加する必要はない。

V1調整中はVersionを常に`1`とする。既存定義でHTMLを再選択すると、同じ
`v1/template.html`を上書きするため、アプリの再ビルドなしでレイアウトを更新できる。
新規登録時はHTML必須、既存定義のメタデータだけを変える場合はHTML省略可とする。

保存時には、View/Table、絞込列、HTML拡張子、ファイルサイズ、tenantを検証する。
帳票コードと処理区分は保存先や画面連携の識別子になるため、登録後は変更不可とする。

## 6. データView

### 6.1 日別労務費一覧

```text
vw_daily_labor_cost_preview
```

- `work_date`を`target_date`として公開
- 承認済み日報だけを対象
- `salary_type`（給与計算基準）と`payment_cycle`（支払サイクル）を別の軸として判定
- 日給・時給ベースの発生労務費は、その勤務日に日報へ保存した総支給額
- 月給ベースは月額÷20、週給ベースは週額÷5を当日の発生労務費とする（V1固定算定）
- 日払いは、勤務日の発生労務費と当日支払額を同額とする
- 週払い・月払いの当日支払額は、日報の`payment_date`が帳票対象日と一致する分の発生労務費を合算する
- 週払い・月払いは、支払日以外の当日支払額を0円とする
- 支払額はこの帳票の目的に合わせて控除後手取りではなく、発生労務費を用いる
- 日全体の発生労務費・支払額をWindow関数で算出

V1の月額÷20・週額÷5は日別労務費の概算用であり、給与締めの確定計算式ではない。

### 6.2 給与支払表

```text
vw_daily_payment_preparation_preview
```

- `payment_date`を`target_date`として公開
- 帳票発行日と`payment_date`が一致する承認済み日報だけを対象
- 日払い・週払い・月払いの全支払区分を対象
- 同一の`payment_date`を持つ日報を従業員単位で合算
- 手当、控除、貯蓄、貸付返済を支払準備用に集計
- 支払区分は`employee_contract.payment_cycle`から取得し、日・週・月の順で表示
- 金種は従業員ごとの支払額を分解してから全員分を合算
- 1円単位で過不足なく配れるよう、1万円から1円までの金種を表示

### 6.3 日次給与明細の手当・控除

- 日次表示が有効で単位が`DAILY`または`BOTH`の手当・控除マスターを起点に、マスター表示順で最大10件展開する
- 適用範囲が`ALL_EMPLOYEES`なら全従業員、`EMPLOYEE_ENROLLMENT`なら勤務日時点で有効な従業員別紐付けがある項目だけを表示する
- 入力元が`DAILY_REPORT`または`DAILY_REPORT_AND_TRANSACTION`の項目を対象とし、日報に実績明細がなければ0円で表示する
- 日報に保存された`daily_report_allowances`と`daily_report_deductions`があれば、その実績金額を表示する
- 旧データ等で合計値だけが残り明細行がない場合は、差額を「その他手当」または「その他控除」として表示する
- 「その他控除」はマスター項目ではなく、`daily_report.deduction_amount`から同日報の`daily_report_deductions.amount`合計を引いた正の差額である。控除実績がすべて明細保存されていれば表示されない
- 備考は日報の「備考」(`daily_report.work_description`)を使用する。同一支払日の承認済み日報が複数ある場合は勤務日順に` / `で連結し、未入力は除外する
- 貯金と借入金返済額は控除明細へ表示し、控除合計に含める
- 携帯電話貸出料とWi-Fi使用料は、確定請求明細で未徴収残高を作り、日報で実際に徴収した額だけを日次給与明細と支払額へ反映する
- ローカル確認用の`E2E-DAILY-001`（2026年9月5日）には、寮費、携帯電話貸出料、Wi-Fi使用料、法定準備金、貯金、借入金返済額の実額を登録する

全員の支払総額を一度だけ金種分解すると、総額は一致しても従業員ごとの封筒へ
配れない組み合わせが生じる。例えば6,000円を2名へ渡す場合、総額12,000円を
分解した1万円札1枚・千円札2枚では配れないため、5千円札2枚・千円札2枚とする。

両Viewとも画面側は`target_date`だけを意識する。
日別労務費一覧Viewは`work_date`と`payment_date`を同じ対象日軸に合流させ、
発生と支払を別々に集計する。

## 7. DB適用

適用SQL：

```text
backend/src/main/resources/sql/system/report/preview/daily_preview_foundation_v1.sql
```

このSQLは次を行う。

1. `operation_report_preview`へHTMLテンプレート管理列を追加
2. 日別View 2件を作成
3. 日別プレビュー帳票マスター2件を登録

不足列だけを追加するため、Hibernateの`ddl-auto:update`実行前後のどちらでも適用できる。

### 7.1 ローカルDocker

`local` profileでは、HibernateによるEntityテーブル更新後に上記SQLを自動適用する。

```text
OperationReportPreviewSchemaInitializer
```

起動のたびにViewとマスターを冪等更新するため、DB Volumeを作り直しても
日次画面だけが存在してView／帳票定義が欠落する状態にはならない。

適用後は`OperationReportPreviewReadinessValidator`が次を検証する。

- `vw_daily_labor_cost_preview`
- `vw_daily_payment_preparation_preview`
- `DAILY_LABOR_COST_PREVIEW`
- `DAILY_PAYMENT_PREPARATION`

いずれかが不足する場合、日次帳票が利用不能なまま起動成功として扱わない。

### 7.2 AWS DEV

AWS DEVはアプリケーションユーザーへDDL権限を付与しないため、従来どおり
`apply_runtime_schema_upgrade.sh`からSQLを適用する。

通常の`aws` profile起動ではReadiness Checkだけを行う。
DB更新スクリプトも、View 2件とマスター2件が揃わなければ失敗する。

## 8. 主なコード資産

### バックエンド

```text
features/system/report/service/builder/ReportHtmlTemplateKeyBuilder.java
features/system/report/service/loader/ReportHtmlTemplateLoader.java
features/system/report/service/core/ReportHtmlTemplateRenderer.java
features/system/report/service/initializer/BundledReportHtmlTemplateInitializer.java
```

既存の`operation/reportpreview`は日次・月次画面との互換入口として残す。
テンプレートの取得・検証・描画という共通責務は`system/report`側へ移した。

### フロントエンド

```text
features/operation/reportpreview/api/useOperationReportPreviewHtml.ts
features/operation/reportpreview/components/OperationReportTab.vue
```

## 9. 検証結果

- Spring Bootバックエンドのコンパイル成功
- HTMLテンプレートキー・Version・パストラバーサル検証成功
- S3テンプレート読込・SHA-256不一致検出テスト成功
- 実テンプレート2件のThymeleaf描画成功
- マスター管理された`target_date`列でのSQL生成テスト成功
- MySQL 8.4でDDL・View・マスターSQLの適用成功
- サンプル日別労務費：12,000円 + 15,000円 = 27,000円
- サンプル支払額：10,000円 + 14,000円 = 24,000円
- 日払い・週払い・月払いの同一支払日集計をMySQLで検証
- 金種を従業員別に分解後、全員分へ合算することを検証
- 10円・5円・1円を含む1円単位の過不足なしを検証
- 今回変更したフロントファイルのESLint成功

フロント全体の型検査には、応募者・顧客・Storybookなど並行変更箇所の既存エラーが残っている。
今回の日別プレビュー変更から新たな型エラーは検出されていない。

## 10. DEV適用後の確認

1. SQLをDEV DBへ適用
2. バックエンドをデプロイしてHTMLテンプレートをS3へ初期登録
3. 日次管理で対象日を選択
4. 日別労務費一覧を開き、最新日報との金額を照合
5. 給与支払表を開き、同一支払日の各日報との金額を照合
6. 日払い・週払い・月払いが表示され、週・月払いは複数日分が合算されることを確認
7. 各従業員へ支払額どおりに配布できる金種枚数になっていることを確認
8. 給与支払表の「ブラウザ印刷」を確認
   - URL、印刷日時、ブラウザのページタイトルが用紙へ出ないこと
9. プレビュー操作で`report_history`が増えないことを確認
10. 別tenantのデータが表示されないことを確認

プレビューAPIが失敗した場合、画面には共通エラーメッセージとTrace IDを表示する。
Trace IDをバックエンドログで検索し、DB View、マスター、テンプレートのどこで
失敗したかを確認する。

給与支払表の印刷CSSでは`@page { margin: 0 }`を指定し、本文側へ印刷余白を持たせる。
Chromium系ブラウザでは、これにより標準のURL・印刷日時・ページタイトルを印字領域から
除外する。端末の印刷設定で「ヘッダーとフッター」が強制される場合は、印刷ダイアログでも
同項目をオフにする。

## 11. 今後変更しやすい箇所

- Viewの計算式：業務計算変更時
- Thymeleafテンプレート：表示・罫線・改ページ変更時
- `operation_report_preview`：帳票追加、Version更新、印刷可否変更時
- 金種列：1円・5円・10円などの表示追加時

View、テンプレート、画面操作を分離しているため、帳票レイアウト変更だけで
日報・給与計算ロジックを変更する必要はない。
