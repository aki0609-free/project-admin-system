# Rule Gateway 契約 V1

## 1. 目的

Rule管理の技術的な`ruleName`を、画面や各業務サービスから直接呼ばないための共通契約を定義する。

```text
Frontend / Business Service
        ↓ gatewayKey
Rule Gateway
        ↓ tenant・対象日から実Ruleを解決
RuleExecutionService
```

呼出側が指定するのは用途を表す安定した`gatewayKey`であり、実際のRule名は業務別Resolverが決定する。

## 2. InterfaceとAdapterの単位

Ruleを使うたびに個別InterfaceやAdapterは作成しない。

- 共通実行Interface：`RuleGatewayService`を1つだけ用意する
- 業務別登録：同じドメインの契約群を`RuleGatewayContractProvider`へまとめる
- Rule割当：同じドメインのKey群を`RuleGatewayBindingResolver`へまとめる
- 型付きGateway：給与確定など重要な業務集約でのみ薄く被せる

日報給与では、通常・残業・深夜・休日の4項目を1つのProviderと1つのResolverで扱う。

## 3. Gateway契約

各Keyは次を宣言する。

| 項目 | 内容 |
|---|---|
| `gatewayKey` | 呼出側が使用する安定Key |
| `resultShape` | `SCALAR` / `OBJECT` / `LIST` / `TABLE` |
| `requiredParameters` | 実行前に検査する必須入力 |
| `frontendExecutable` | 画面からの実行を許可するか |

Provider間で同じKeyが重複した場合、アプリケーション起動時に拒否する。

マスター追加によってKeyが増える用途は、静的契約を1件ずつ登録せず、`RuleGatewayDynamicContractProvider`でKey形式を検証して契約を生成する。

## 4. API

認証済みユーザーが利用できる。任意の`ruleName`は受け付けず、`frontendExecutable=true`のKeyだけを実行する。

| Method | Path | 用途 |
|---|---|---|
| POST | `/api/system/rule-gateway/execute` | 1契約を実行 |
| POST | `/api/system/rule-gateway/batch` | 複数行・複数列を一括計算 |

### 単体実行

```json
{
  "gatewayKey": "DAILY_PAY.NORMAL_PAY",
  "targetDate": "2026-11-05",
  "parameters": {
    "componentType": "NORMAL_PAY",
    "employeeId": 10
  }
}
```

`targetDate`はRuleパラメータにも自動追加する。入力側が明示した`parameters.targetDate`は上書きしない。

### 一括実行

```json
{
  "targetDate": "2026-11-05",
  "failureMode": "PARTIAL",
  "rows": [
    { "rowKey": "employee-10", "parameters": { "employeeId": 10 } },
    { "rowKey": "employee-11", "parameters": { "employeeId": 11 } }
  ],
  "calculations": [
    {
      "outputKey": "normalAmount",
      "gatewayKey": "DAILY_PAY.NORMAL_PAY",
      "parameters": { "componentType": "NORMAL_PAY" }
    },
    {
      "outputKey": "overtimeAmount",
      "gatewayKey": "DAILY_PAY.OVERTIME_PAY",
      "parameters": { "componentType": "OVERTIME_PAY" }
    }
  ]
}
```

- 1回のHTTP通信で最大500行・50計算列を受け付ける
- 行パラメータへ計算列パラメータを重ねて実行する
- Rule割当はKey単位でリクエスト冒頭に一括解決し、セルごとに設定DBを検索しない
- Rule本体も使用するRule名を一括ロードし、セルごとにRuleマスターを検索しない
- `bindings`へ出力列ごとのKey・Rule名・バージョン・結果形を一度だけ返す
- `FAIL_FAST`は最初の失敗で全体を失敗させる
- `PARTIAL`は成功セルを保持し、失敗セルだけ`errors`へ返す
- 部分失敗の応答へ内部例外メッセージは露出しない

## 5. フロントエンド利用方針

共通Clientは次を提供する。

```text
executeRuleGateway<TResult, TParameters>()
executeRuleGatewayBatch<TResult>()
```

テーブルの可変自動計算は、1セルごとにAPIを呼ばず、表示対象行と必要列をまとめて`batch`へ送る。

画面計算は入力補助・プレビューである。保存、締め、請求、給与確定ではバックエンドが同じGatewayまたは業務サービスを使って再計算し、画面の計算値を正として保存しない。

## 6. 日報給与の初回移行

既存の日報給与4項目をGateway契約へ登録した。

```text
DAILY_PAY.NORMAL_PAY
DAILY_PAY.OVERTIME_PAY
DAILY_PAY.NIGHT_PAY
DAILY_PAY.HOLIDAY_PAY
```

既存業務サービスは、従来どおり必要なRule設定をまとめて取得した後、計算実行だけを`RuleGatewayService.executeBound`経由へ変更した。したがって既存の金額計算順序と丸め処理は変更しない。

## 7. バージョン管理

応答契約には`ruleVersion`を用意したが、現在のRuleマスターには不変の版番号と適用期間がないため、現段階では`null`を返す。

Ruleマスターへ履歴・適用開始日を導入した後、Resolverが対象日に有効な版を決定して`ruleVersion`を返す。版管理前に擬似的な番号を返し、再計算の再現性があるように見せることはしない。

## 8. 手当・控除AUTO計算の移行

手当・控除マスターのAUTO計算は、次の動的Keyを使用する。

```text
PAYROLL_ITEM.ALLOWANCE.{masterId}
PAYROLL_ITEM.DEDUCTION.{masterId}
```

- マスターごとのProvider・Interface・Adapterは作成しない
- Resolverがテナント内の手当／控除マスターを取得し、設定済みRuleへ割り当てる
- `MANUAL`または`FIXED`の項目をGatewayから実行しようとした場合は拒否する
- `targetType`、`targetMasterId`、`targetCode`はResolverが信頼値としてRuleパラメータへ設定し、画面からの同名入力で上書きさせない
- 既存の日報保存処理も`PayrollItemValueService`から同じGatewayを経由する

フロントエンドは`createPayrollItemRuleGatewayKey`を使い、Key文字列を画面へ直書きしない。

可変テーブルでは`calculatePayrollItemRuleTable`を使用する。このAdapterは、画面の行を`rows`、AUTO計算項目を`columns`として受け取り、全セルを1回のbatch通信へまとめる。

```text
2従業員 × 5自動計算列
  × 10回の単体API通信
  ○ 1回のbatch API通信（10セルをサーバー内で計算）
```

Adapterの返却値は`values[rowKey][outputKey]`と`errors[rowKey][outputKey]`で参照する。画面はRule名・Gateway Key形式・部分失敗レスポンスの構造を直接扱わない。

日報の手当・控除プレビューは、手入力・固定額・残高・従業員別設定も扱うため、フロントから直接batch APIへ置き換えない。既存の業務APIを維持し、その内部の`PayrollItemCalculationService`で対象AUTO項目のBindingとRuleを一括準備してから各項目へ再利用する。これにより画面仕様と保存時再計算を維持したまま、項目ごとのRuleマスター再読込を避ける。

## 9. 追加時の判断基準

新しいRule利用箇所では次の順で判断する。

1. 既存Gateway Keyで表現できる場合は再利用する
2. 同じ業務ドメインのProvider・ResolverへKeyを追加する
3. 複数画面・複数サービスで共有する重要な業務契約だけ型付きGatewayを追加する
4. 単発表示項目のためだけに個別InterfaceやAdapterを増やさない
