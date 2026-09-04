# 書類管理 未使用・未連携機能の調査 V1

ドメイン：書類管理

## 1. V1で注意すべき実装

| 優先度 | 対象 | 現状 | 推奨 |
|---|---|---|---|
| 高 | テナント分離 | 保存キーに`tenant_id`を自動付与しない。画面も`X-Tenant-ID: default`固定 | V1単一テナント制約を明記。汎用化前に`documents/{tenantId}/...`等へ分離 |
| 高 | 取込スクリプト | SYS_ADMINが登録した`.py/.sh`をサーバーOS Processとして実行できる | 権限・監査を強化し、署名/ハッシュ・許可済みスクリプト管理を検討 |
| 対応済 | ファイル監査 | DBメタデータは持たない | V1では変更操作・downloadの利用者、Tenant、area、path、action、結果をアプリケーションログへ記録 |
| 対応済 | upload上書き | 同名をBackendで拒否し、画面に差替手順を表示 | 無確認の上書きを行わない |
| 中 | S3 Versioning費用 | 年度帳票以外の非現行Versionに削除Lifecycleがない | 保存要件を決め、会社書類・スクリプト等の非現行Version保持期間を設定 |
| 中 | 検索性能 | 領域全Objectを再帰取得してアプリで部分一致 | V1規模を制限。将来はDB索引やS3 Inventory等を検討 |
| 中 | テンプレート更新 | 書類管理では参照専用 | 「書類管理から更新できる」と誤解させず、帳票・台帳管理を正式経路にする |
| V1対象外 | upload内容検査 | 拡張子と容量中心で、マルウェア・実MIME・内容検査なし | 現運用ではウイルススキャンを導入しない。許可利用者とファイル種別の制限は維持する |
| 低 | 詳細の作成日時 | 更新日時を作成日時として返す | UIラベルまたはStorage metadataを改善 |

## 2. FileManager固有の境界ケース

### ワイルドカードだけの検索（対応済）

Syncfusionの`searchString`から`*`を除去した結果が空の場合は、現在フォルダー配下を再帰一覧する。自動テストで固定済み。

### フォルダDownload判定（対応済）

ZIPにするかはサーバー側の保存状態から判定し、requestの`data[].isFile`欠落時もフォルダーをZIP化する。Backendテストで固定済み。

### move/renameの原子性

moveとrenameは「コピー→削除」で、ストレージを跨ぐDBトランザクションではない。コピー後の削除失敗時は両方残る可能性がある。S3/LOCALそれぞれで障害時復旧手順が必要である。

## 3. 業務連携されていないもの

- 会社書類は顧客ID、従業員ID、契約種別、保存期限等の業務メタデータを持たない。
- 書類管理の削除は帳票履歴・バックアップ実行テーブル等を更新しない。ただし生成帳票・バックアップ領域は参照専用なので通常画面から不整合を起こせない。
- S3 Version履歴はFileManagerから確認・復元できない。
- eTagはレスポンスに含むが、更新競合制御や条件付き更新には利用しない。

## 4. テスト状況

確認できた主なBackendテスト：

- `DocumentAreaPolicyTest`
- `DocumentStorageKeyResolverTest`
- `DocumentManagementServiceTest`
- `SyncfusionFileManagerServiceTest`
- `LocalStorageServiceTest`
- `S3StorageServiceTest`
- `StorageServiceTest`
- `ImportScriptPathResolverTest`
- `BundledImportScriptInitializerTest`

V1で追加を推奨するテスト：

1. Syncfusion FileManagerのread/upload/rename/move/delete/download E2E
2. 読取専用領域への直接API更新が403になること
3. S3でフォルダZIPダウンロードと日本語ファイル名
4. テナントを切り替えた場合に同一キーが衝突する現制約の検知
5. uploadしたスクリプトのハッシュ、実行者、実行対象を追跡できること
6. 大量Object時の一覧・検索時間とメモリ使用量

## 5. V1判断

単一テナント・最大4名の現運用であれば、LOCAL/S3共通基盤と領域別権限は実用可能な構成である。ただしリリース条件として次を推奨する。

1. V1は単一テナントであることを運用・設計書へ明記する。
2. uploadは同名を拒否する現仕様を維持する。
3. 書類操作ログと取込スクリプト実行ログをCloudWatch Logsで確認する。
4. 取込スクリプト編集者をSYS_ADMINの中でも運用上限定する。
5. 生成帳票・バックアップ・テンプレートは参照専用を維持する。
6. S3 Version復元と誤削除時の手順をKB化する。
