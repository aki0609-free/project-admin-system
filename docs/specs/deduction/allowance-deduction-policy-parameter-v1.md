# 手当・控除Policy／parameter整理 V1

## 1. 月次処理の確定方針

V1の月次給与は、日報明細・明細取引・税保険マスターを月次Viewで集計し、締め処理で履歴へ確定する。JEXL・MVEL・Java Beanの汎用Ruleを月次Viewから呼び出さない。

したがって、次の設定を月次計算条件として新たに接続しない。

| 現行項目 | 現状 | V1方針 |
|---|---|---|
| `showOnMonthlyStatement` | 保存されるが月次View・帳票で未参照 | 月次帳票の表示項目は表示用Viewを正とする。2026-09-02に管理画面から除外した |
| `carryToMonthlySettlement` | 保存されるが月次Viewで未参照 | 月次の計算対象は計算用Viewを正とする。2026-09-02に管理画面から除外した |
| `MONTHLY` / `PAYROLL`汎用Rule | providerの業務呼出元がない | V1では実行基盤を追加しない。法定控除等は月次Viewで計算する |

DB列と既存API項目は、既存データおよびSQL資産との互換性確認が終わるまで直ちに削除しない。先に画面から誤設定を防ぎ、最終資産整理時にDDLを確定する。

なお、手当の`taxable`は表示制御ではなく課税対象額の計算条件である。これは月次Viewへ反映すべき別課題として残す。

## 2. Policy項目の意味と現状

| 画面項目 | API／DB | 想定する意味 | 現行実装 | V1推奨 |
|---|---|---|---|---|
| 適用対象 | `applicationScope` | 全従業員へ自動適用するか、従業員ごとの利用登録を必要とするか | 従業員画面・日報候補で利用中 | 維持 |
| 標準入力元 | `inputSource` | 日報明細または任意タイミングの明細取引のどちらで実績を作るか | 利用中 | 維持 |
| 残高を管理する | `balanceTracking` | 数量・金額の発生と消化を累積管理するか | 利用中 | 維持 |
| 残高単位 | `balanceUnit` | 金額、日数、時間、回数 | 利用中 | 維持 |
| 加算頻度 | `accrualFrequency` | DAILY／MONTHLY／MANUALという加算契機 | 保存されるが残高計算で未参照 | 画面へ出さず、V1では廃止候補 |
| 加算Rule | `accrualRuleName` | 残高の発生数量を求める方法 | `CALENDAR_DAYS_IN_ENROLLMENT`と`MANUAL_TRANSACTION`のみ実利用 | 2方式を明示的な「残高発生方式」として再設計候補 |
| 残高を繰り越す | `carryForward` | 月末残高を翌月へ持ち越すか | 判定では未参照。現行計算は登録開始日から通算するため常に繰越相当 | V1は常時繰越として項目を画面から外す候補 |
| 残高超過を許可 | `advanceConsumption` | 残高を超える消化を許すか | 判定では未参照。表示時には負数を0へ丸める | V1は超過禁止へ統一し、項目を画面から外す候補 |

### 残高計算の現行式

```text
前月以前残高 = 前月末までの発生 + CREDIT取引 - 日報消化 - DEBIT取引
当月発生     = 当月の自動発生 + 当月CREDIT取引
当月消化     = 当月の日報消化 + 当月DEBIT取引
残高         = 前月以前残高 + 当月発生 - 当月消化
```

現行は負残高を`0`として返すため、超過量を後から確認できない。`advanceConsumption`を正式採用する場合は、保存前検証、負残高保持、画面表示、月跨ぎテストまで一体で実装する必要がある。

## 3. parameter定義の意味

parameterは「給与項目そのもの」ではなく、従業員ごとの適用設定とRule計算に必要な補助値を定義する。

| 画面項目 | API／DB | 用途 | 現行接続 |
|---|---|---|---|
| パラメーターキー | `key` / `parameter_key` | 設定JSON・Rule・Resolverを結ぶ不変識別子 | 接続済み。作成後は変更不可 |
| 表示名 | `displayName` | 従業員設定画面の項目名 | 接続済み |
| 入力型 | `inputType` | TEXT／NUMBER／SELECT／BOOLEAN／DATE | 保存時検証・従業員画面で利用中 |
| 初期値 | `defaultValue` | 従業員別設定がない場合の既定値 | 接続済み |
| 必須 | `required` | 従業員別設定保存時に入力を要求する | 接続済み |
| Ruleへ渡す | `ruleParameter` | 対象項目のRuleパラメーターへ値を追加する | 接続済み |
| 日報に表示 | `dailyDisplay` | parameter自体を日報へ表示する意図 | 保存・返却のみで日報UI未接続 |
| 入力元切替 | `inputSourceOverride` | 従業員ごとにDAILY_REPORT／TRANSACTIONを切り替える | 接続済み。SELECT型1項目だけ許可 |
| Rule値Resolver | `ruleValueResolverKey` | 別parameterの選択肢からRule用数値を解決する | `SELECT_OPTION_CALCULATION_VALUE:<key>`を実装済み |
| 表示順 | `displayOrder` | 従業員設定画面での順序 | 接続済み |

## 4. `dailyDisplay`を接続する前の境界

従業員設定値と、その日の日報で入力する実績値は区別する。

- 従業員設定値：寮タイプ、徴収方式、基準単価など。有効期間付きで従業員設定へ保存する。
- 日報実績値：当日の金額、支払日数、手動変更理由など。日報明細へ保存する。

`dailyDisplay=true`のparameterは、まず日報に読み取り専用の「計算条件」として表示する。日報から直接変更すると従業員設定の有効期間を破壊するため、V1では編集させない。

日報固有の入力値が必要な場合はparameter値の上書きではなく、日報明細側の入力定義として別に扱う。接続時には次をテストする。

1. 対象日に有効な従業員設定だけが表示される。
2. `dailyDisplay=false`は日報へ表示されない。
3. 日報の表示値とRuleへ渡した値が一致する。
4. 過去日報を開いても現在の従業員設定で表示が変わらないよう、計算根拠のスナップショットを保存する。

## 5. V1実装順序

1. 月次の未接続設定を画面から隠し、View計算であることを説明する。（対応済み）
2. 残高超過を禁止する保存前検証を統一し、負数を丸めて隠すだけの挙動をなくす。
3. `carryForward`と`accrualFrequency`の互換データを調査し、不要ならAPI・Entity・DDLの順に廃止する。
4. `dailyDisplay`を読み取り専用の計算条件として日報へ接続し、根拠値を日報明細へ保存する。
5. 課税手当を月次Viewの課税対象額へ反映する。

## 6. 今回削除した旧境界

- 未使用の`AllowanceValueRequest`
- 未使用の`DeductionValueRequest`
- `AllowanceMaster`と同義だった`AllowanceSavePayload` alias
- `DeductionMaster`と同義だった`DeductionSavePayload` alias

保存APIは専用の`AllowanceSaveRequest`／`DeductionSaveRequest`、日報計算は共通の給与項目request経路を正とする。
