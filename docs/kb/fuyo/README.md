# Fuyo会社独自仕様 KB

## 目的

このフォルダでは、ProjectAdminSystemの汎用基盤とは分離して、Fuyo固有の給与、日報、締め、帳票および運用仕様を管理する。

汎用基盤へ適用できる設計と、Fuyo固有の計算・表示・運用ルールを混在させない。

## KB一覧

| KB | 内容 |
|---|---|
| `payroll-statement-specification-v1.md` | 日次・月次給与明細、法定預り、前借り、前払い、貯金、借入返済のV1仕様 |

## 手当・控除基盤との境界

- 寮費、携帯電話貸出料、Wi-Fiなどの項目コード・入力方式・残高方式は共通の手当・控除マスターで定義する。
- Fuyo固有の寮タイプ別日額は、Coreへ`DORMITORY_FEE`分岐を置かず、Resolverキー`DORMITORY_DAILY_AMOUNT`で会社固有サービスを呼び出す。
- Resolverは従業員別パラメーター`dormitoryType`とFuyoの寮費設定を参照し、Ruleへ`dormitoryDailyAmount`を渡す。
- 日報Ruleは汎用パラメーター`dormitoryDailyAmount`と`itemQuantity`だけを使用する。

## 管理方針

- 汎用的なテーブル、残高、Rule、帳票基盤は共通機能として実装する。
- Fuyo固有の項目構成、表示名、計算方式、締め運用はこのフォルダへ記録する。
- 仕様変更時は、実装前に本KBの影響範囲と決定事項を更新する。
- マスター初期値を変更した場合は、適用DDL、対象テナント、適用日も追記する。
