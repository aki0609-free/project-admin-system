# 明細型・月次型 手当・控除取引基盤 V1

## 1. 目的

携帯料金のように明細到着時に金額が確定する控除、寮費のように従業員ごとに日次／月次の徴収方法が異なる控除、不定期に支給する臨時手当などを、既存の手当・控除マスター、月次締め、履歴基盤へ統合する。

項目ごとの専用テーブルは作らない。新しい同種の控除は、控除マスター、ポリシー、従業員別設定を追加して利用する。

## 2. 全体フロー

```text
手当・控除マスター
  ├─ DAILY_REPORT
  │    └─ Rule計算／手動調整 → 日報控除明細
  ├─ TRANSACTION または collectionMode=MONTHLY
  │    └─ 従業員画面で明細登録 → 従業員別手当・控除取引
  └─ DAILY_REPORT_AND_TRANSACTION
       ├─ 請求・発生額 → 従業員別手当・控除取引（残高加算）
       └─ 実徴収・実支給額 → 日報明細（残高消化）

確定済み日報手当・控除 + CONFIRMED手当・控除取引
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
| `TRANSACTION` | 明細到着・外部連携・月次入力による手当または控除 | 表示しない |
| `DAILY_REPORT_AND_TRANSACTION` | 請求・発生額を明細登録し、実際の徴収・支給を日報で入力 | 表示する |

`balance_tracking_flag=false` の項目は、残日数・繰越数量を管理しない。

残高管理を有効にした場合、`carry_forward_flag=true`で前月末残高を翌月へ繰り越す。`false`の場合は月初残高を0とし、当月の増加・消化だけで残高を計算する。

`advance_consumption_flag=false`では現在残高を超える日報消化・確定取引を拒否する。`true`では超過分を負の残高として保持し、後続の残高増加と相殺する。

## 4. 携帯料金

- 控除コード：`MOBILE_RENTAL`
- 入力方式：`DAILY_REPORT_AND_TRANSACTION`
- 計算区分：`MANUAL`（実際に徴収した額を日報で入力）
- 残高単位：`AMOUNT`
- 明細が届いたら、種類を「請求・残高を登録」として登録する。
- 請求明細の`CONFIRMED`は請求額の確定を意味し、徴収済みを意味しない。
- 確定した請求額は未徴収残高へ加算するが、月次給与へ直接控除しない。
- 日報には実際に徴収した金額を入力し、その金額を未徴収残高から減らす。
- 月末に残った金額は翌月の開始残高へ自動的に繰り越す。
- 同月に5件以上登録してもよい。
- `DRAFT`は残高・月次締めのどちらにも反映しない。
- 同じ明細の重複防止には任意の「明細番号」を使用する。

月次で残額を一括徴収する場合は、種類を「月次給与へ直接控除」として登録する。この明細は月次控除へ入り、残高を減らす。日報にも同額を登録してはならない。

## 5. 寮費

従業員別設定 `inputSource` で徴収方法を切り替える。

| 値 | 処理 |
| --- | --- |
| `DAILY_REPORT` | 日報へ実際に徴収した金額と未徴収残高を表示し、日報控除として保存する |
| `TRANSACTION` | 日報へ表示せず、月次一括徴収額を「明細・月次控除」へ登録する |

標準値は `DAILY_REPORT` とする。月次一括徴収は会社の実運用に合わせて確定額を登録する。

寮費の残高は「未徴収金額」で管理する。当月発生額は汎用ルール `CALENDAR_DAYS_TIMES_PARAMETER:dormitoryDailyAmount` により、当月の適用日数と従業員設定へ保存した日額から計算する。日報または明細取引で登録した実徴収額を差し引き、未徴収金額を翌月へ繰り越す。

このルールは寮費コードを判定しない。任意の手当・控除で「適用日数×従業員別単価」の金額残高を作るために利用できる。ルールとの不整合を防ぐため、このルールを選択中の残高単位は画面・サーバーとも `AMOUNT` に固定する。

## 5.1 明細型手当

- `target_type=ALLOWANCE`として控除と同じ`employee_payroll_item_transaction`へ保存する。
- 手当マスターを有効化し、Policyを`EMPLOYEE_ENROLLMENT`かつ入力元`TRANSACTION`にする。
- 従業員へそのPolicyを適用すると、従業員画面に「明細・月次手当」が表示される。
- `CONFIRMED`の金額だけを月次給与の支給項目として合算する。
- `DRAFT`は月次計算・締め履歴・給与明細のすべてから除外する。
- 臨時手当などの明細型手当で利用できる。日報で入力する手当は従来どおり`DAILY_REPORT`を選択する。

## 6. 月次締めと再締め

月次給与Viewは次を同じ控除コード単位で合算する。

1. 承認済み日報の手当・控除明細
2. 対象月が一致し、状態が`CONFIRMED`かつ種類が`PAYROLL_ITEM`の従業員別手当・控除取引

`BALANCE_ACCRUAL`は残高発生専用であり、月次給与額へ直接含めない。この分離により、請求額と日報徴収額の二重控除を防ぐ。

初回締めと再締めは常に最新Viewから取得する。締め時点の値は既存の履歴・出力テーブルへ版付きで保存する。再締めでは既存版を上書きせず、締めバージョンを増やす。帳票再出力は履歴データを使用する。

## 7. 取引データ

テーブル：`employee_payroll_item_transaction`

主要項目：

| カラム | 内容 |
| --- | --- |
| `employee_id` | 対象従業員 |
| `target_type` | `ALLOWANCE`（手当）または`DEDUCTION`（控除） |
| `target_code` | 手当・控除マスターコード |
| `target_month` | 集計対象月（月初日） |
| `transaction_date` | 明細日。同じ対象月内であること |
| `amount` | 控除金額 |
| `quantity` | 任意の対象数量・日数 |
| `transaction_purpose` | `BALANCE_ACCRUAL`（請求・残高発生）または`PAYROLL_ITEM`（月次給与へ直接反映） |
| `source_type` | `MANUAL` / `CSV` / `EXTERNAL` / `MONTHLY_OPERATION` |
| `source_reference` | 外部明細番号。指定時は重複を防止する |
| `status` | `DRAFT` / `CONFIRMED`。請求明細の確定は徴収完了を意味しない |

残高は、適用開始以降の`CREDIT - 日報消化 - DEBIT`で算出する。月初で残高をリセットしないため、未払い・未支給分は翌月以降もそのまま残る。

削除は論理削除とし、他テナント・他従業員のデータは取得・更新できない。

## 8. 画面操作

```text
従業員管理
→ 従業員を選択
→ 手当・控除設定タブ
→ 対象手当・控除を有効化
→ 入力元がTRANSACTIONまたはDAILY_REPORT_AND_TRANSACTIONの項目を選択
→ 明細・月次手当、または明細・月次控除
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

- `DAILY_REPORT_AND_TRANSACTION`の携帯料金が日報と明細入力の両方へ表示されること
- 寮費 `MONTHLY` が日報へ表示されないこと
- 寮費 `DAILY` は従来どおり日報へ表示されること
- 同月の複数の `CONFIRMED` 明細が合算されること
- `DRAFT` が月次締めへ入らないこと
- `ALLOWANCE`取引が月次支給項目へ入り、締め履歴へ固定されること
- `BALANCE_ACCRUAL`が月次給与へ直接入らないこと
- 前月の未徴収金額が翌月開始残高へ繰り越されること
- 日報の徴収金額が同額の残高消化として保存されること
- 日報控除と取引控除が同じコードなら二重行ではなく1項目に集約されること
- 締め後の履歴項目へ確定額が保存されること
- 再締め時に最新値を新しいバージョンへ保存すること

## 11. AWS DEVへの適用順序

AWSプロファイルはHibernateの`ddl-auto=validate`を使用する。Entityが参照する新テーブル・新カラムを追加するリリースでは、必ずDB資産を先に適用する。

```text
1. RDSとEC2を起動
2. AWS CLIへログイン
3. apply_runtime_schema_upgrade.shを実行
4. RUNTIME_SCHEMA_UPGRADE_COMPLETEを確認
5. Deploy DEVを実行
6. backend / frontendのHealthyを確認
7. Cloudflare Access境界を確認
```

実行コマンド：

```bash
AWS_PROFILE=project-admin-terraform \
infrastructure/scripts/database/apply_runtime_schema_upgrade.sh
```

DB未適用のまま新Entityを含むイメージを起動すると、HibernateのSchema Validationが不足テーブルを検出し、バックエンドは起動しない。デプロイスクリプトは失敗時にバックエンドログ末尾を自動出力する。

切替に失敗した場合は、DBを破壊的に戻さず、まず直前に成功したDeploy DEVを再実行してアプリケーションを復旧する。今回の追加SQLは再適用可能な形にしている。
