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
デプロイ資産からVersion 1を登録する。

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

## 6. データView

### 6.1 日別労務費一覧

```text
vw_daily_labor_cost_preview
```

- `work_date`を`target_date`として公開
- 承認済み日報だけを対象
- 従業員・支払区分単位に発生労務費を集計
- 同日に登録された日次支払額を表示
- 日全体の発生労務費・支払額をWindow関数で算出

### 6.2 給与支払表

```text
vw_daily_payment_preparation_preview
```

- `payment_date`を`target_date`として公開
- 承認済み日報の給与計算結果と`daily_payments`を統合
- 日次支払が保存済みなら`actual_amount`を優先
- 未保存なら日報の`estimated_net_pay_amount`を使用
- 手当、控除、貯蓄、貸付返済を支払準備用に集計
- 当日支払総額から金種枚数を算出

両Viewとも画面側は`target_date`だけを意識する。
元テーブルの`work_date`／`payment_date`の違いを画面へ漏らさない。

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
- 金種：1万円札2枚・千円札4枚
- 今回変更したフロントファイルのESLint成功

フロント全体の型検査には、応募者・顧客・Storybookなど並行変更箇所の既存エラーが残っている。
今回の日別プレビュー変更から新たな型エラーは検出されていない。

## 10. DEV適用後の確認

1. SQLをDEV DBへ適用
2. バックエンドをデプロイしてHTMLテンプレートをS3へ初期登録
3. 日次管理で対象日を選択
4. 日別労務費一覧を開き、最新日報との金額を照合
5. 給与支払表を開き、日次支払画面との金額を照合
6. 給与支払表の「ブラウザ印刷」を確認
7. プレビュー操作で`report_history`が増えないことを確認
8. 別tenantのデータが表示されないことを確認

プレビューAPIが失敗した場合、画面には共通エラーメッセージとTrace IDを表示する。
Trace IDをバックエンドログで検索し、DB View、マスター、テンプレートのどこで
失敗したかを確認する。

## 11. 今後変更しやすい箇所

- Viewの計算式：業務計算変更時
- Thymeleafテンプレート：表示・罫線・改ページ変更時
- `operation_report_preview`：帳票追加、Version更新、印刷可否変更時
- 金種列：1円・5円・10円などの表示追加時

View、テンプレート、画面操作を分離しているため、帳票レイアウト変更だけで
日報・給与計算ロジックを変更する必要はない。
