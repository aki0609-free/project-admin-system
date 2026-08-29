# 顧客情報・封筒宛名印刷 V1安定化

## 1. 対象

```text
顧客管理
  ├ 顧客情報
  ├ 現場一覧
  ├ 顧客社員（顧客担当者）
  ├ 請求単価
  └ 封筒宛名印刷
```

顧客マスタは日報、顧客請求締め、請求書、注文書、入金管理から参照される基幹マスタである。

## 2. V1仕様

### 2.1 顧客基本情報

| 区分 | 主な項目 |
|---|---|
| 基本 | 顧客名、ふりがな、短縮社名、郵便番号、住所、代表者、電話 |
| 契約・請求 | 職種、契約有無、請求書パターン、顧客締日、支払日 |

請求書パターンは次の3種類を維持する。

- `PATTERN_1`：職種のみ、現場名なし
- `PATTERN_2`：職種・役職別、現場名なし
- `PATTERN_3`：職種・役職・現場別

締日と支払日は共通`DayRule`で保持する。月末指定の場合は日付値を`NULL`とし、日付指定は1〜31日の範囲とする。

### 2.2 現場

顧客ごとに複数の現場を管理する。

- 現場名は必須
- 現場担当者の氏名、電話、メールを保持可能
- 会社からの距離は0km以上
- 別顧客に属する現場IDは更新・削除できない
- 日報等から参照中の現場は削除できない

### 2.3 顧客社員

顧客側の担当者を管理する。

- 氏名は必須
- ふりがな、役職、電話、メールを保持
- 請求書送信先の`To`または`CC`へ指定する場合はメールアドレス必須
- 別顧客に属する顧客社員IDは更新・削除できない

### 2.4 請求単価

請求単価は顧客の現場ごとに管理し、職種・現場役割・適用期間で解決する。

- 画面上の複数行保存は`bulk-save` APIを使用し、削除・追加・更新を1トランザクションで確定する
- いずれか1行の検証または保存に失敗した場合、同じ保存操作の変更はすべてロールバックする
- 同一の現場・職種・役職で適用期間が重複する単価は登録しない
- 単価は0以上、最大13桁＋小数2桁とする
- 職種コード・役職コードは100文字、名称は200文字、備考は1000文字以内とする

単価の詳細は顧客請求締めと月間集計のKBを参照する。

### 2.5 顧客・現場候補の同期

日報と翌日準備で使用する顧客・現場候補はPiniaストアでキャッシュする。
通常の画面遷移ではキャッシュを再利用するが、顧客の作成・更新・削除が成功した時点で強制再取得し、古い顧客名や削除済み現場が選択候補に残らないようにする。

## 3. 削除と参照整合性

顧客、現場、顧客社員は物理削除せず`deleted_at`による論理削除とする。

顧客削除時は次の順で検証・処理する。

```text
顧客の参照有無を検証
  ↓
現場の請求単価を論理削除
  ↓
現場を論理削除
  ↓
顧客社員を論理削除
  ↓
顧客を論理削除
```

## 4. 封筒宛名印刷

### 4.1 対応用紙

| 封筒 | 帳票コード | バッチコード | テンプレート |
|---|---|---|---|
| 長形3号 | `ENVELOPE_NAGA3` | `PRINT_ENVELOPE_NAGA3` | `envelope_naga3.jrxml` |
| 角形2号 | `ENVELOPE_KAKU2` | `PRINT_ENVELOPE_KAKU2` | `envelope_kaku2.jrxml` |

### 4.2 入力

- 印刷する顧客：1件以上、複数選択可
- 封筒タイプ：長3／角2
- スタンプ：請求書在中等
- 敬称：御中、様等

フォントはPDF出力での日本語表示を保証するため`IPAexGothic`に固定する。旧画面のフォント・文字サイズ選択は実PDFに反映されていなかったため、V1では表示しない。レイアウト変更は帳票ごとのJRXMLで行う。

### 4.3 処理フロー

```text
顧客画面で宛先を選択
  ↓
封筒種別に応じたREPORTバッチを即時実行
  ↓
customerIdsを1顧客=1 input行へ展開
  ↓
customersと結合してoutputテーブルへ宛名を作成
  ↓
JRXMLでPDFを生成
  ↓
ファイルを帳票ストレージへ保存
  ↓
画面のPDFプレビューを表示
  ↓
入力・中間テーブルをクリーンアップ
```

封筒印刷は即時出力であり、7年バックアップ対象の月次帳票には含めない。

## 5. DB資産

正本SQL：

```text
backend/src/main/resources/sql/system/report/envelope/envelope_foundation_v1.sql
```

正本SQLは再実行可能で、次を作成・更新する。

- `envelope_print_input`
- `envelope_print_output`
- `report_master`の2帳票
- `report_param`の実行パラメータ
- `batch_job_definition`の2バッチ

Runtime Schema Manifestに含めるため、ローカルDockerのRuntime Schema実行時とAWS反映時に同じ資産が配置される。

`sql/initdata/system/report/default.sql`と`sql/system/batch/report/envelope.sql`は旧資産であり、現行スキーマと一致しない。V1のセットアップでは実行せず、上記の正本SQLを使用する。

## 6. テンプレート配置

ソース：

```text
backend/src/main/resources/reports/envelope_naga3.jrxml
backend/src/main/resources/reports/envelope_kaku2.jrxml
```

ローカル・AWSの保存先：

```text
documents/templates/reports/envelope_naga3.jrxml
documents/templates/reports/envelope_kaku2.jrxml
```

`BundledReportTemplateInitializer`が起動時に保存先を確認し、存在しない場合だけデプロイ資産から配置する。管理中のテンプレートを起動ごとに上書きしない。

移行元として確認した旧プロジェクト資産：

```text
/Users/tatsukiakiyama/Desktop/Workspace/SpringBoot/ProjectAdmin/backend/src/main/resources/reports/envelope_naga3.jrxml
/Users/tatsukiakiyama/Desktop/Workspace/SpringBoot/ProjectAdmin/backend/src/main/resources/reports/envelope_kaku2.jrxml
```

## 7. 主なコード

### フロントエンド

```text
frontend/src/features/customer/pages/CustomerMaster.vue
frontend/src/features/customer/components/CustomerFormDialog.vue
frontend/src/features/customer/components/EnvelopePrintDialog.vue
frontend/src/features/customer/composables/useCustomerEditDialog.ts
frontend/src/features/customer/composables/useEnvelopePrintDialog.ts
frontend/src/features/customer/mapper/customerMapper.ts
frontend/src/features/customer/validation/customerSchema.ts
```

### バックエンド

```text
backend/src/main/java/com/project/backend/features/customer/service/CustomerCommandService.java
backend/src/main/java/com/project/backend/features/customer/service/CustomerSiteBillingRateCommandService.java
backend/src/main/java/com/project/backend/features/customer/mapper/CustomerMapper.java
backend/src/main/java/com/project/backend/features/system/batch/service/BatchExecutionParameterService.java
backend/src/main/java/com/project/backend/features/system/report/service/initializer/BundledReportTemplateInitializer.java
```

## 8. 入力検証と正規化

- 顧客名は必須、文字項目はDBカム長以内
- 現場名と顧客社員名は必須
- 現場・顧客社員のメール形式を検証
- 会社からの距離は0以上
- 請求書To／CC担当者はメール必須
- 保存時に文字列の前後空白を除去
- 空文字の任意項目は`NULL`へ正規化
- フロントエンドとバックエンドの両方で検証
- 請求単価の複数行保存はAPI単位で原子性を保証

## 9. テスト

バックエンド：

- 他顧客の現場更新を拒否
- 請求書送信先のメール必須
- 不正なメール・距離・締日を拒否
- 文字列と月末DayRuleの正規化
- 長3／角2 JRXMLのコンパイルとPDF生成
- 複数顧客IDのバッチパラメータ保存・復元
- Map、入れ子リスト、1000件超の配列を拒否
- 請求単価の顧客所有権、期間重複、負数、小数桁、更新IDを検証

フロントエンド／E2E：

- 顧客詳細の開閉、現場タブ表示
- 新規顧客の必須項目表示
- 封筒印刷の入力ダイアログ
- 顧客選択から実際の長3／角2 PDF生成
- PDFプレビュー表示
- 顧客・現場候補の通常キャッシュと、顧客変更後の強制再取得

## 10. V1で追加しない範囲

- 封筒ごとの自由なフォント・文字サイズ編集
- 管理画面からのJRXMLレイアウト編集
- 封筒PDFの7年バックアップ
- 封筒テンプレートの顧客別切替
