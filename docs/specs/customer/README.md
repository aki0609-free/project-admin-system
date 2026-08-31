# 顧客管理ドメイン仕様

## 対象画面

- 顧客情報：`/customer/information`
- 取引管理：`/customer/transaction`

顧客締め・月次締めの処理仕様は、このフォルダの対象外とする。

## V1確定事項（2026-08-31）

- 現場・顧客担当者のIDは内部自動採番とし、画面には表示しない。
- 請求単価は`DAILY` / `HOURLY` / `MONTHLY`の3種類とし、`FIXED`は`MONTHLY`へ統合する。
- 顧客担当者は顧客別の請求書メールグループへ自動同期する。
- 契約状態は`ACTIVE`（契約中）/ `INACTIVE`（未契約）/ `ENDED`（契約終了）の選択式とする。
- 契約状態はV1時点では管理・表示用途とし、締め処理や請求の実行可否には使用しない。
- `CANCELED` / `WRITE_OFF`は互換性のため内部予約値として残すが、V1の操作対象外とする。

## 資料

| 資料 | 内容 |
|---|---|
| [画面からDBまでの処理フロー](customer-management-screen-to-db-flow-v1.md) | 画面、API、Service、Repository、DBの流れと関連クラス |
| [入力項目の利用先・システム連携](customer-field-usage-and-integration-v1.md) | 画面項目、API項目、DBカラム、他機能での利用状況 |
| [未使用・未連携機能の調査](customer-unused-and-unintegrated-v1.md) | 未参照コード、画面から到達できないAPI、保存のみ・未完成の業務連携 |
