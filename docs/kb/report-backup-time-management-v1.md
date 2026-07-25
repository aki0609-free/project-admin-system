# 帳票・バックアップ日時管理 V1

## 1. 目的

帳票とバックアップで個別に取得していた現在日時を、アプリケーション共通の`Clock`へ統一する。

次を実現する。

- 業務日付を日本時間へ統一
- 日跨ぎ・年跨ぎを自動テスト可能にする
- OS、JVM、Docker、AWSのタイムゾーン差異を業務結果へ持ち込まない
- JasperReports作成時にファイル名や履歴日時の共通基盤を変更しなくてよい状態にする

## 2. タイムゾーン

業務日付：

```text
Asia/Tokyo
```

DBへ保存する時刻：

```text
Instant
```

ファイル名へ埋め込む年月日時分秒は、共通Clockを日本時間として`LocalDateTime`へ変換して使用する。

## 3. 変更対象

### 帳票

- PDF・CSV・XLSXのファイル名
- 帳票マスターの論理削除日時
- 月次締めで生成した帳票ファイルの生成日時

### DBバックアップ

- CSVファイル名
- ZIPファイル名
- 成功履歴の実行日時
- 失敗履歴の実行日時
- バックアップ定義の論理削除日時
- バックアップ定義配下カラムの論理削除日時

## 4. 変更しない仕様

- 帳票ファイル名の基本形式
- バックアップファイル名の基本形式
- S3保存キー
- LOCAL保存キー
- StorageType
- 帳票履歴
- バックアップ履歴のDB構造
- REPORT_MAILの個人別生成
- メール送信処理
- JRXMLの管理方式
- 管理画面のテスト印刷

DDL変更はない。

## 5. ファイル名

### 帳票

```text
{fileName}_{yyyyMMddHHmmss}.{extension}
```

`fileName`未設定時：

```text
{reportCode}_{yyyyMMddHHmmss}.{extension}
```

### バックアップCSV

```text
{targetCode}_{yyyyMMdd_HHmmss_SSS}_{random8}.csv
```

### バックアップZIP

```text
backup_{yyyyMMdd_HHmmss_SSS}_{random8}.zip
```

ランダム8文字は同一時刻の重複生成対策として維持する。

## 6. 年跨ぎテスト

固定するUTC時刻：

```text
2026-12-31T15:00:01Z
```

日本時間：

```text
2027-01-01T00:00:01+09:00
```

期待する帳票ファイル名：

```text
salary_slip_20270101000001.pdf
```

期待するバックアップファイル名：

```text
EMPLOYEE_20270101_000001_000_{random8}.csv
```

UTCの2026年12月31日ではなく、日本時間の2027年1月1日が使用されることを確認する。

## 7. 追加テスト

```text
ReportFileNameBuilderTest
ReportMasterAdminServiceTest
BackupFileNameBuilderTest
BackupHistoryBuilderTest
BackupTargetCommandServiceTest
MonthlyClosingJobExecutorTest
```

確認項目：

- 帳票ファイル名の日本時間・年境界
- fileName未設定時のreportCode利用
- バックアップCSV・ZIPの日本時間・年境界
- 既存ファイル名形式の維持
- 成功・失敗履歴の固定実行日時
- 定義と配下カラムへ同じ削除日時を設定
- 月次締め帳票の生成日時

## 8. JasperReports作業との境界

JasperReports作成側は、今回整備したファイル名・履歴・S3保存処理を変更しない。

JasperReports作成側の担当：

- JRXML
- 帳票固有レイアウト
- 帳票固有SQL・ストアド
- サンプルパラメータ
- プレビュー確認

共通基盤の変更が必要な場合は、対象帳票コードと影響範囲を整理して、本チャットへ戻して確認する。

## 9. 確認結果

- 帳票・バックアップ関連テスト：成功
- 全バックエンド通常テスト：成功
- MySQL・MongoDB・Redis Testcontainers統合テスト：成功
- Spring Boot ApplicationContext起動：成功
