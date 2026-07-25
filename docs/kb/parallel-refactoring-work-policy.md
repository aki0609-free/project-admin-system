# 並行リファクタリング作業ルール

## 1. 目的

インフラ・システム運用・S3・Testcontainersを整備する作業と、HR分析およびJasperReportsのリファクタリングを並行しても、変更の衝突や仕様の不整合を発生させないための作業境界を定義する。

## 2. 作業開始条件

並行作業を開始する前に、現在の変更をテストし、チェックポイントとしてコミットする。

確認項目：

```bash
cd /Users/tatsukiakiyama/Documents/ProjectAdmin/public-repository/backend
./gradlew --no-daemon test
./gradlew --no-daemon integrationTest

cd /Users/tatsukiakiyama/Documents/ProjectAdmin/public-repository/frontend
npm run type-check
npm run lint
```

既知のフロントエンドエラーが残る場合は、今回変更によるエラーと既存エラーを区別してコミットメッセージまたはKBへ記録する。

未コミットの大規模変更を同じ作業フォルダで複数チャットから編集しない。

## 3. 推奨ブランチ

```text
refactor/system-infrastructure-v1
refactor/hr-analytics-v1
refactor/jasper-reports-v1
```

HR分析とJasperReportsは同時に変更せず、HR分析を完了・確認してからJasperReportsへ進む。

Git worktreeを使用する場合も、チェックポイントコミットを作成した後に分離する。

## 4. このチャットの担当領域

この「インフラ構築・書類管理（S3）」チャットは次を担当する。

```text
infrastructure/**
.github/workflows/**
backend/src/main/java/com/project/backend/app/config/**
backend/src/main/java/com/project/backend/app/storage/**
backend/src/main/java/com/project/backend/features/system/**
backend/src/test/java/com/project/backend/testsupport/**
frontend/src/features/system/**
docs/kb/**
```

特に次は普通のチャットから変更しない。

- S3キー設計
- StorageServiceとStorageBackend
- 帳票の共通生成・履歴・保存処理
- メールキューと添付処理
- バッチ基盤
- Testcontainers共通設定
- GitHub Actions
- Terraform
- Syncfusion導入予定の書類管理・台帳管理

## 5. HR分析チャットの担当領域

HR分析は次へ限定する。

```text
backend/src/main/java/com/project/backend/features/application/**
backend/src/test/java/com/project/backend/features/application/**
frontend/src/features/application/**
frontend/src/app/menu/analysisMenu.ts
```

実施してよい内容：

- 応募者データ分析
- 応募媒体データ分析
- Controller、Service、Mapper、DTOの整理
- Vue画面、Composable、API、型定義の整理
- 集計ロジックの単体テスト
- 既存権限`application:view`の使用
- AIを使用しない状態での画面・API確認

実施しない内容：

- OpenAI共通基盤の変更
- 権限コードの新設
- 共通HTTPクライアントの変更
- 共通フォーム部品の大規模変更
- S3、帳票、メール、バッチへの依存追加
- 他ドメインのEntityやRepository変更

共有部品の変更が必要になった場合は、その場で変更せず、必要理由と影響するファイルをこのチャットへ戻して確認する。

## 6. JasperReportsチャットの担当領域

JasperReports作業は、原則として帳票資材と帳票固有データだけへ限定する。

```text
backend/src/main/resources/reports/**
backend/src/main/resources/sql/system/report/{reportCode}/**
backend/src/main/resources/templates/operation/reportpreview/**
```

実施してよい内容：

- JRXML作成
- 帳票ごとのレイアウト調整
- フォント、改ページ、表示形式の確認
- 帳票固有の前処理SQL・ストアド・出力項目整理
- サンプルパラメータとプレビュー確認
- 帳票単位のテスト

変更しない領域：

```text
backend/src/main/java/com/project/backend/features/system/report/**
backend/src/main/java/com/project/backend/app/storage/**
backend/src/main/java/com/project/backend/features/system/mail/**
backend/src/main/java/com/project/backend/features/system/batch/**
```

帳票基盤側の変更が必要になった場合は、次を整理してこのチャットへ戻す。

- 対象帳票コード
- 現在の動作
- 必要な動作
- 共通基盤を変更する理由
- 影響する保存・履歴・メール処理

## 7. JasperReportsの既存契約

次の合意済み仕様を維持する。

- JRXMLはV1ではLOCAL／デプロイ資材
- 管理画面のテスト印刷は履歴・S3へ保存しない
- REPORT_MAILは管理者用全員分PDFと個人別PDFを生成
- 個人別PDFは`business_key`単位
- メール未登録者も個人PDFを保存しFAILEDを記録
- 一部失敗でも他の従業員は継続
- 送信先は従業員マスター駆動
- 保存失敗または履歴登録失敗時は不要ファイルを削除

## 8. 1機能ごとの進め方

複数ドメインをまとめて変更しない。

```text
1. 対象機能の現状確認
2. 仕様・影響範囲の整理
3. 既存動作を固定するテスト
4. リファクタリング
5. 対象テスト
6. 全通常テスト
7. 必要な場合だけTestcontainers統合テスト
8. 変更ファイル一覧と残課題を記録
9. 1機能単位でコミット
```

## 9. 合流時の確認

各ブランチを合流する前に、次を確認する。

- 共通基盤ファイルを意図せず変更していない
- APIパスとDTOの変更がフロントエンドと一致している
- DDL変更の有無を明記している
- AI無効状態でSpring Bootが起動する
- 通常テストが成功する
- Testcontainers統合テストが成功する
- フロントエンド型検査結果を記録している
- 帳票はプレビューとPDF表示を確認している

競合が発生した場合は、両方の変更を機械的に残さず、ファイルの担当チャット側で解決する。

## 10. 普通のチャットへ渡す開始指示

```text
ProjectAdminSystem V1のHR分析機能だけをリファクタリングします。

対象は次に限定してください。
- backend/src/main/java/com/project/backend/features/application/**
- backend/src/test/java/com/project/backend/features/application/**
- frontend/src/features/application/**
- frontend/src/app/menu/analysisMenu.ts

AI機能はV1では使用せずOFFのままにします。
既存仕様を変更する前に影響範囲を整理してください。
共通基盤、S3、帳票、メール、バッチ、権限コード、共通HTTPクライアントは変更しないでください。
共有ファイルの変更が必要な場合は実装せず、理由と対象ファイルを先に提示してください。
一度に複数ドメインを変更せず、応募者分析、応募媒体分析の順に1機能ずつ進めてください。
各機能で既存動作を固定するテストを先に追加し、変更後に対象テストと全通常テストを実行してください。
```
