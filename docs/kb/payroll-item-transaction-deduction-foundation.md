# 明細型・月次型控除取引基盤 V1

## 1. 目的

携帯料金のように明細到着時に金額が確定する控除と、寮費のように従業員ごとに日次／月次の徴収方法が異なる控除を、既存の手当・控除マスター、月次締め、履歴基盤へ統合する。

項目ごとの専用テーブルは作らない。新しい同種の控除は、控除マスター、ポリシー、従業員別設定を追加して利用する。

## 2. 全体フロー

```text
控除マスター
  ├─ DAILY_REPORT
  │    └─ Rule計算／手動調整 → 日報控除明細
  └─ TRANSACTION または collectionMode=MONTHLY
       └─ 従業員画面で明細登録 → 従業員別控除取引

確定済み日報控除 + CONFIRMED控除取引
  → 月次給与View
  → 締めストアド
  → historyTable / outputTable
  → 帳票生成
```

## 3. 入力方式

`payroll_item_balance_policy.input_source` で標準入力元を管理する。

| 値 | 用途 | 日報への表示 |
| --- | --- | --- |
| `DAILY_REPORT` | Rule計算または日報上の手動調整 | 表示する |
| `TRANSACTION` | 明細到着・外部連携・月次入力 | 表示しない |

`balance_tracking_flag=false` の項目は、残日数・繰越数量を管理しない。

## 4. 携帯料金

- 控除コード：`MOBILE_RENTAL`
- 入力方式：`TRANSACTION`
- 残数量管理：なし
- 明細が届くたびに、従業員画面の「明細・月次控除」から1件登録する。
- 同月に5件以上登録してもよい。
- `CONFIRMED` の明細だけを月次締めへ含める。
- `DRAFT` は編集中として月次締めから除外する。
- 同じ明細の重複防止には任意の「明細番号」を使用する。

## 5. 寮費

従業員別設定 `collectionMode` で徴収方法を切り替える。

| 値 | 処理 |
| --- | --- |
| `DAILY` | 日報へ金額・徴収日数・残日数を表示し、日報控除として保存する |
| `MONTHLY` | 日報へ表示せず、月次一括金額を「明細・月次控除」へ登録する |

標準値は `DAILY` とする。月次運用は自動で金額を作らず、V1では会社の実運用に合わせて確定額を登録する。

## 6. 月次締めと再締め

月次給与Viewは次を同じ控除コード単位で合算する。

1. 承認済み日報の控除明細
2. 対象月が一致し、状態が `CONFIRMED` の従業員別控除取引

初回締めと再締めは常に最新Viewから取得する。締め時点の値は既存の履歴・出力テーブルへ版付きで保存する。再締めでは既存版を上書きせず、締めバージョンを増やす。帳票再出力は履歴データを使用する。

## 7. 取引データ

テーブル：`employee_payroll_item_transaction`

主要項目：

| カラム | 内容 |
| --- | --- |
| `employee_id` | 対象従業員 |
| `target_code` | 控除マスターコード |
| `target_month` | 集計対象月（月初日） |
| `transaction_date` | 明細日。同じ対象月内であること |
| `amount` | 控除金額 |
| `quantity` | 任意の対象数量・日数 |
| `source_type` | `MANUAL` / `CSV` / `EXTERNAL` / `MONTHLY_OPERATION` |
| `source_reference` | 外部明細番号。指定時は重複を防止する |
| `status` | `DRAFT` / `CONFIRMED` |

削除は論理削除とし、他テナント・他従業員のデータは取得・更新できない。

## 8. 画面操作

```text
従業員管理
→ 従業員を選択
→ 手当・控除設定タブ
→ 対象控除を有効化
→ 携帯料金、または寮費 collectionMode=MONTHLY を選択
→ 明細・月次控除
→ 対象月、明細日、金額、状態を入力
```

対象月を過去月へ切り替えた場合、明細日の初期値はその月の1日になる。対象月外の明細日はAPIでも拒否する。

## 9. DB適用資産

```text
backend/src/main/resources/sql/daily_report/payroll_item_balance_foundation_v1.sql
backend/src/main/resources/sql/daily_report/payroll_item_transaction_foundation_v1.sql
backend/src/main/resources/sql/system/report/pay_slip/monthly_pay_slip_view_foundation_v1.sql
```

AWSとTestcontainersは `runtime-schema-manifest.txt` の依存順で同じSQLを適用する。

## 10. 検証観点

- `TRANSACTION` の携帯料金が日報へ表示されないこと
- 寮費 `MONTHLY` が日報へ表示されないこと
- 寮費 `DAILY` は従来どおり日報へ表示されること
- 同月の複数の `CONFIRMED` 明細が合算されること
- `DRAFT` が月次締めへ入らないこと
- 日報控除と取引控除が同じコードなら二重行ではなく1項目に集約されること
- 締め後の履歴項目へ確定額が保存されること
- 再締め時に最新値を新しいバージョンへ保存すること

