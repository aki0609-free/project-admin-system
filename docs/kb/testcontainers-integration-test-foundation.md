# Testcontainers統合テスト基盤

## 1. 目的

ProjectAdminSystem V1のリファクタリングで、開発者のPCや共有AWS環境の状態に依存せず、次のミドルウェアを使う処理を自動確認できるようにする。

- MySQL
- MongoDB
- Redis

通常の単体テストと、Dockerを使用する統合テストを分離する。GitHub Actionsでは両方を必須チェックとして実行し、失敗したコードをパッケージ化しない。

## 2. 対象構成

| 種別 | Testcontainersイメージ | 主な確認対象 |
|---|---|---|
| MySQL | `mysql:8.0.46` | JPA、SQL、バッチメタデータ |
| MongoDB | `mongo:8.0` | 監査ログ等のDocument保存 |
| Redis | `redis:7.4-alpine` | キャッシュ、分散ロック |

コンテナの接続先はSpring Bootの`@ServiceConnection`で自動設定する。固定ポートは使用しないため、ローカルDocker Composeを起動したままでもポート競合しない。

## 3. テストの分類

### 3.1 通常テスト

JUnitタグ`integration`を持たないテストを実行する。

```bash
cd /Users/tatsukiakiyama/Documents/ProjectAdmin/public-repository/backend
./gradlew --no-daemon test
```

用途：

- Utility
- Mapper
- Validator
- ServiceのMockitoテスト
- 外部ミドルウェアを必要としないControllerテスト

通常テストではDockerを起動しない。

### 3.2 統合テスト

JUnitタグ`integration`を持つテストだけを実行する。

```bash
cd /Users/tatsukiakiyama/Documents/ProjectAdmin/public-repository/backend
./gradlew --no-daemon integrationTest
```

用途：

- Spring Boot ApplicationContext起動
- Repositoryと実DBのマッピング
- MySQL固有SQL
- MongoDB Document保存
- Redisへの読み書き
- 複数レイヤーをまたぐ処理

実行時に必要なコンテナは自動作成され、テスト終了後に自動破棄される。ローカルDocker ComposeのDBを使用したり、手動でテストデータを削除したりする必要はない。

## 4. 前提条件

ローカル実行時：

- Docker Desktopが起動済みであること
- Java 21を使用できること
- 初回のみDockerイメージのダウンロードが発生すること

AWS、RDS、MongoDB Atlas、ローカルの固定ポートへの接続情報は不要。

## 5. テスト用設定

テストプロファイル：

```text
test
```

主な設定：

- OpenAI機能：無効
- JPA DDL：`create-drop`
- Flyway：無効
- Spring Batchメタデータ：自動作成
- ファイルストレージ：一時ディレクトリ
- S3：無効
- Pythonコマンド：`python3`（Python前処理を含む取込テストで使用）

テスト用ファイルはOSの一時ディレクトリ配下へ保存し、開発用ストレージやS3を変更しない。

## 6. 共通基底クラス

ミドルウェアが必要な統合テストは、次の基底クラスを継承する。

```java
class SampleRepositoryIntegrationTest
        extends ContainerIntegrationTest {

    @Test
    void sample() {
        // RepositoryまたはServiceを実DBで確認する
    }
}
```

`ContainerIntegrationTest`が次を共通化する。

- `@SpringBootTest`
- `@ActiveProfiles("test")`
- Testcontainers設定の読み込み
- JUnitの`integration`タグ

個別テストでコンテナや接続URLを再定義しない。

## 7. 基盤疎通テスト

`InfrastructureContainersIntegrationTest`では、次を実データストアに対して確認する。

### MySQL

- `SELECT 1`が成功する
- 接続DB名が`ADMIN`である

### MongoDB

- 監査ログDocumentを保存できる
- IDで再取得できる
- テスト終了時に削除する

### Redis

- ランダムなテストキーを書き込める
- 同じ値を読み出せる
- テスト終了時に削除する

## 8. GitHub Actions

CIは次の順番で実行する。

```text
1. Frontend検証
2. Backend通常テスト
3. Backend Testcontainers統合テスト
4. Backendパッケージ作成
```

通常テストまたは統合テストが1件でも失敗した場合、CIを失敗させる。従来の`continue-on-error`による全体テストの参考実行は廃止する。

## 9. テスト追加ルール

- DBやRedisの挙動が不要なServiceはMockitoによる通常テストとする
- Repository、Entityマッピング、実SQLは統合テストとする
- テスト間で共有する固定IDを極力使わない
- Redisキーや業務キーにはUUID等を使用する
- 作成したデータは`finally`またはテストトランザクションで後片付けする
- AWS、Atlas、S3、OpenAIなど外部サービスへ接続しない
- 時刻依存ロジックは可能な限り`Clock`等で固定する

## 10. 今回検出・修正した不具合

監査ログのMongoDB定義が次のようになっていた。

```java
@Document(collation = "audit_logs")
```

`collation`はコレクション名ではなく照合順序の指定であるため、MongoDB 8.0では監査ログ保存時に不正な`locale`として失敗した。

修正後：

```java
@Document(collection = "audit_logs")
```

影響範囲：

- MongoDBの監査ログ保存先
- 監査ログの保存・参照処理

既存環境に既定名のコレクション（例：`auditLog`）が存在し、移行対象データがある場合は、`audit_logs`へ移行してから旧コレクションを整理する。新規・空環境では移行不要。

## 11. トラブルシューティング

### Dockerへ接続できない

Docker Desktopを起動してから再実行する。

```bash
docker info
```

### 初回実行が遅い

MySQL、MongoDB、Redisイメージを初回だけダウンロードするため。2回目以降はローカルキャッシュを利用する。

### ローカルの3306等と競合しないか

Testcontainersはランダムなホスト側ポートへ割り当てるため、原則として競合しない。

### テストレポート

```text
backend/build/reports/tests/test/index.html
backend/build/reports/tests/integrationTest/index.html
```

## 12. 完了条件

- 通常テストが成功する
- Spring Bootのテスト用ApplicationContextが起動する
- MySQL疎通テストが成功する
- MongoDB保存・取得テストが成功する
- Redis読み書きテストが成功する
- GitHub Actionsで通常テストと統合テストが必須化されている

## 13. 税金データ取込テスト

税金系は次の2経路を確認する。

| テスト | 確認内容 |
|---|---|
| `ResidentTaxImportContainerIntegrationTest` | 自治体通知CSVをPythonで12か月へ正規化し、MySQLへUPSERTして住民税控除詳細へ反映 |
| `IncomeTaxImportContainerIntegrationTest` | 国税庁ExcelのPython変換後CSVをMySQLへUPSERTし、所得税控除詳細へ反映 |

住民税テストはJavaから実際に`python3`を起動する。国税庁Excelについては
公式2026年版の実ファイルをDockerのPython環境で変換し、1928行、キー重複なし、
先頭0～104,999円、最終3,500,000円以上の税額帯まで生成できることを確認した。

この検証で次の基盤不具合を修正した。

- Spring BatchのExecutionContextへ保存する取込結果がSerializableでなく、ジョブが`UNKNOWN`になっていた
- Python出力のUTF-8 BOMが先頭ヘッダーへ残り、1列目を認識できなかった
- アップロードファイルへPython前処理を適用できなかった
- 国税庁2026年版Excelの列位置と複数行ヘッダーに未対応だった
- `.xls`読込に必要な`xlrd`がコンテナへ入っていなかった
- 所得税表の再取込がINSERT_ONLYで重複する設計だった

## 14. 時間境界テストの方針

ProjectAdminSystemでは、締日、月次処理、給与計算、通知、帳票、年度バックアップなどで日時の境界が業務結果へ影響する。

本番コードから`LocalDate.now()`、`LocalDateTime.now()`、`Instant.now()`を直接呼び出す実装は、対象ドメインのリファクタリング時に段階的に`Clock`注入へ変更する。

一括置換は行わない。次の順番で、対象機能の動作確認と同時に導入する。

```text
1. DayRuleUtils・締日計算
2. 日報・給与対象日
3. お知らせ生成
4. バッチスケジュール
5. 月次帳票・年度帳票バックアップ
6. 監査ログ等の記録日時
```

### 14.1 テスト対象

- 月末と月初
- 年末と年始
- 会計年度末と新年度
- 2月28日と閏年の2月29日
- 30日までの月における31日締め
- 当月末、翌月末、翌々月末
- 日付変更直前・直後
- 日本時間とUTCの変換
- 有効期間の開始日・終了日
- バッチの予定日時
- 月次処理の重複防止
- 年度帳票バックアップの対象年度

### 14.2 タイムゾーン

業務日付の基準は原則として次を使用する。

```text
Asia/Tokyo
```

DBや外部サービスとの時刻連携では`Instant`を使用し、画面表示や業務日付への変換時に`Asia/Tokyo`を適用する。

### 14.3 単体テスト

日時計算だけを確認する場合は、固定した`Clock`を使用する。

```java
Clock fixedClock = Clock.fixed(
        Instant.parse("2026-01-30T15:00:00Z"),
        ZoneId.of("Asia/Tokyo")
);
```

この例の日本時間は、2026年1月31日0時である。

### 14.4 Testcontainers統合テスト

日時によってDB更新結果が変わる場合は、固定した`Clock`とTestcontainersを組み合わせる。

確認例：

- 締め処理の対象レコード
- 対象月と従業員による重複防止
- バッチ実行履歴の対象日
- 通知の公開開始日と終了日
- 月次帳票の対象年月
- 会計年度終了後のバックアップ対象

テストでOSやコンテナ自体のシステム時刻を変更しない。アプリケーションへ注入した`Clock`だけを切り替える。

### 14.5 導入時の確認事項

各ドメインで`Clock`を導入する前に、次を整理する。

- 業務日付のタイムゾーン
- 日付だけで判定するか、時刻まで判定するか
- 締日が存在しない月の扱い
- 休日の場合の前営業日・翌営業日規則
- 再実行時の重複防止単位
- 手動実行時に基準日を指定できるか

これらが未確定の場合は、推測して実装せず、対象ドメインの仕様確認を行う。

### 13.6 第1段階：締日計算

`DayRuleUtils`は基準年月を引数で受け取る純粋な日付計算であり、現在日時を参照していない。そのため`Clock`は追加せず、月次締め期間の重複していた日付丸め処理を`DayRuleUtils`へ統一した。

追加した確認ケース：

- 末日締め
- 20日締め
- 31日締めの平年2月
- 31日締めの閏年2月
- 12月から翌年1月への年跨ぎ
- 締日値が未設定の場合の既存仕様（31日扱い）

`Clock`は、`LocalDate.now()`等を使用する通知生成、バッチ、記録日時へ適用する段階で導入する。

### 13.7 第2段階：お知らせ機能

アプリケーション共通の`Clock`を追加し、業務タイムゾーンを次へ固定した。

```text
Asia/Tokyo
```

お知らせ領域では、当日検索、自動生成基準日、DayRule対象年月、お知らせ削除日時、NoticeRule削除日時へClockを適用した。

時間境界テストでは、UTCの月末と日本時間の月初が異なる時刻を固定し、DayRule通知が日本時間の対象月で解決されることを確認する。

テスト用のClockだけを固定し、OS、JVM、Dockerコンテナ、DBのシステム時刻は変更しない。

### 13.8 第3段階：バッチ機能

バッチ実行の開始、終了、失敗、最終実行、定義削除日時へ共通Clockを適用した。

バッチおよびお知らせの`ThreadPoolTaskScheduler`にも共通Clockを設定し、Cron実行の基準タイムゾーンを`Asia/Tokyo`へ統一した。

固定Clockを使用する単体テストで時刻値を確認し、Testcontainers統合テストではClock Beanを含むApplicationContextが正常に起動することを確認する。

### 13.9 第4段階：帳票・バックアップ

帳票ファイル名、帳票マスター削除日時、バックアップファイル名、バックアップ履歴、バックアップ定義削除日時、月次締め帳票の生成日時へ共通Clockを適用した。

月次正式帳票について、MySQL・Redis・ローカルストレージを組み合わせた
`ReportFileLifecycleContainerIntegrationTest`を追加した。

確認内容：

- 帳票入力テーブルへの実行条件登録
- 前処理SQLによる出力テーブル生成
- CSV完成ファイルの生成と保存
- `report_history`と保存ファイルの関連付け
- `monthly_closing_report_files`と保存ファイルの関連付け
- 初回締めVersion 1と再締めVersion 2を別ファイルとして保持
- Version 2作成後もVersion 1のファイルと履歴を参照可能
- 新規生成帳票を`documents/generated-reports/reports`へ保存

このテストで、Hibernateの新規DB生成時に
`report_history.request_params_json`が本番DDLより小さい型になる不整合を検出した。
Entityの定義を`LONGTEXT`へ統一し、月次締めパラメータを欠落なく保存できるようにした。

年度帳票バックアップは24時間稼働を前提にしない方針とし、
`AnnualReportBackupContainerIntegrationTest`を追加した。
年度終了から設定された猶予日数が経過した後、最初のアプリケーション起動時に
未実行年度を検出する。管理者による手動実行にも同じサービスを使用する。

詳細は`docs/kb/annual-report-backup-v1.md`を参照する。

固定日時：

```text
2026-12-31T15:00:01Z
```

この時刻は日本時間では2027年1月1日0時0分1秒となる。ファイル名の日付部分が`20270101`となり、年境界でUTC日付を誤って使用しないことを確認する。

ファイル名のランダム部分は固定せず、日時部分と既存フォーマットを正規表現で検証する。

## 14. Spreadsheet台帳生成の統合テスト

2026-07-29に、締め処理のSpreadsheet台帳生成をTestcontainersの実MySQLとLOCALストレージで統合確認した。

対象テスト：

```text
backend/src/test/java/com/project/backend/features/operation/book/service/SpreadsheetLedgerGenerationIntegrationTest.java
```

実行方法：

```bash
cd /Users/tatsukiakiyama/Documents/ProjectAdmin/public-repository/backend

./gradlew --no-daemon integrationTest \
  --tests 'com.project.backend.features.operation.book.service.SpreadsheetLedgerGenerationIntegrationTest'
```

### 14.1 テストデータ

テスト専用テーブルとViewをMySQLコンテナ内に作成し、次の4件を登録する。

| データ | 件数 | 期待 |
| --- | ---: | --- |
| 対象テナント・対象月 | 2 | 生成対象 |
| 別テナント・対象月 | 1 | 除外 |
| 対象テナント・別月 | 1 | 除外 |

テスト終了時に、テスト用View、テーブル、台帳マスタ、カタログ、テンプレートJSON、生成JSONを削除する。ローカルDocker Compose、AWS、RDS、S3のデータは使用しない。

### 14.2 確認項目

- MySQLの台帳専用Viewからデータを取得できる
- `:tenantId`で別テナントを除外する
- `:targetMonth`で別月を除外する
- 対象2件だけを`ROW`変数へ展開する
- 従業員コード・従業員名を文字列として維持する
- 金額をJSON数値として維持する
- 2件目の相対参照数式を`=C2*2`から`=C3*2`へシフトする
- 生成JSONを`GENERATED_REPORTS`領域へ保存する
- 保存JSONを再読込できる
- 保存JSONに未展開の`${...}`が残っていない
- Workbookのlocaleが`ja`になる

### 14.3 実行結果

```text
SpreadsheetLedgerGenerationIntegrationTest
  generate_shouldQueryTenantRowsExpandWorkbookAndSaveJson PASSED

BUILD SUCCESSFUL
```

初回テストでは、JSON内の同じ数値が`9.8E+2`と`980.0`で表現される差をオブジェクト全体の一致判定が検出した。業務値としては同じため、文字列表現ではなく`BigDecimal`の数値比較へ変更した。生成処理の不具合ではない。

## 15. 税・社会保険データの統合テスト

2026-08-09に次のテスト範囲を追加した。

| テスト | 確認範囲 |
|---|---|
| `IncomeTaxImportContainerIntegrationTest` | 所得税正規化CSV、UPSERT、控除詳細 |
| `ResidentTaxImportContainerIntegrationTest` | 住民税通知CSV、Python月別化、UPSERT、控除詳細 |
| `InsuranceRateImportContainerIntegrationTest` | 健康・介護・厚生年金・雇用・支援金の取込と訂正更新 |
| `RuntimeSchemaAssetsIntegrationTest` | 本番SQL資産、公式2026料率による給与View計算 |

公式PDFの抽出値確認と、DB取込・給与計算のTestcontainersテストを分ける。
外部サイトへ接続するテストを通常CIへ含めると不安定になるため、公式PDFの
取得は年度更新作業として実行し、抽出結果を公式値と人手照合する。

### 15.1 公式PDF変換結果

実際の2026年度公式PDFを使用し、次の本人負担率を抽出した。

```text
HEALTH_INSURANCE       0.04805
CARE_INSURANCE         0.00810
PENSION                0.09150
EMPLOYMENT_GENERAL     0.00500
EMPLOYMENT_AGRICULTURE 0.00600
EMPLOYMENT_CONSTRUCTION 0.00600
CHILD_CARE_SUPPORT     0.00115
```

厚生年金は抽出失敗時の固定値フォールバックを廃止した。
雇用保険PDFでは農林水産等の見出しと料率が別行になるため、見出し直後の
料率行を安全に探索する処理へ修正し、3事業区分すべてを確認した。

### 15.2 給与View計算結果

標準報酬月額300,000円、総支給300,000円、介護保険対象、支援金対象の
テスト従業員を登録し、次を確認した。

| 項目 | 期待値 | 結果 |
|---|---:|---|
| 健康・介護保険 | 16,845円 | PASSED |
| 子ども・子育て支援金 | 345円 | PASSED |
| 厚生年金 | 27,450円 | PASSED |
| 雇用保険 | 1,800円 | PASSED |
| 社会保険合計 | 46,440円 | PASSED |
| 課税対象額 | 253,560円 | PASSED |
| 計算可能判定 | `true` | PASSED |

雇用保険は計算経路確認のため建設事業の本人率を使用した。本番の事業区分は
労働保険上の正式区分を確認してから確定する。

`tax_import_foundation_v1.sql`を同じMySQLへ2回適用し、対象7定義と
介護保険4カラムが重複しないことも確認した。
