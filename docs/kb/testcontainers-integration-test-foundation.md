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

## 13. 時間境界テストの方針

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

### 13.1 テスト対象

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

### 13.2 タイムゾーン

業務日付の基準は原則として次を使用する。

```text
Asia/Tokyo
```

DBや外部サービスとの時刻連携では`Instant`を使用し、画面表示や業務日付への変換時に`Asia/Tokyo`を適用する。

### 13.3 単体テスト

日時計算だけを確認する場合は、固定した`Clock`を使用する。

```java
Clock fixedClock = Clock.fixed(
        Instant.parse("2026-01-30T15:00:00Z"),
        ZoneId.of("Asia/Tokyo")
);
```

この例の日本時間は、2026年1月31日0時である。

### 13.4 Testcontainers統合テスト

日時によってDB更新結果が変わる場合は、固定した`Clock`とTestcontainersを組み合わせる。

確認例：

- 締め処理の対象レコード
- 対象月と従業員による重複防止
- バッチ実行履歴の対象日
- 通知の公開開始日と終了日
- 月次帳票の対象年月
- 会計年度終了後のバックアップ対象

テストでOSやコンテナ自体のシステム時刻を変更しない。アプリケーションへ注入した`Clock`だけを切り替える。

### 13.5 導入時の確認事項

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

固定日時：

```text
2026-12-31T15:00:01Z
```

この時刻は日本時間では2027年1月1日0時0分1秒となる。ファイル名の日付部分が`20270101`となり、年境界でUTC日付を誤って使用しないことを確認する。

ファイル名のランダム部分は固定せず、日時部分と既存フォーマットを正規表現で検証する。
