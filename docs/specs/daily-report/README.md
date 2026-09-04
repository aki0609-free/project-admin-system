# 日報管理ドメイン仕様

## 対象画面

- 締め処理 → 日報入力（本資料では「日報管理」と記載）
- 画面URL：`/operation/daily-reports`
- API：`/api/daily-reports`

本資料は日報の登録・更新だけでなく、勤務時間、Rule給与計算、手当・控除、請求単価、貸付・貯蓄、有給、日次・月次帳票までを追跡対象とする。

## 資料

| 資料 | 内容 |
|---|---|
| [画面からDBまでの処理フロー](daily-report-screen-to-db-flow-v1.md) | 一覧・詳細・登録・更新・削除、未入力者、月次勤怠と関連クラス・テーブル |
| [入力項目の利用先・システム連携](daily-report-field-usage-and-integration-v1.md) | 全入力項目をRule、手当・控除、請求、給与明細、台帳・締めまで追跡 |
| [未使用・未連携機能の調査](daily-report-unused-and-unintegrated-v1.md) | 保存のみ、画面未公開、概算経路の不一致、V1安定化課題 |
