# 労務費支払一覧 Spreadsheet台帳 V1

## 1. 目的

対象月の従業員別総支給額と控除額を、支払い方法別に一覧表示する。

- 支払い方法は`employee_contract.payment_cycle`で分類する。
- 日払い、週払い、月払いをそれぞれ別シートにする。
- 同じ支払周期の従業員は人数にかかわらず1シートへまとめる。
- 1ファイルに対象月の全支払周期・全従業員を格納する。
- A4横、各シート1ページ印刷を想定する。

## 2. 画面操作

1. 「締め処理 → 台帳」を開く。
2. 対象月を選択する。
3. 「労務費支払一覧」を生成する。
4. Spreadsheet上で日別金額、月合計、控除合計、差引支給額を確認する。
5. 必要に応じて印刷する。

従業員選択は行わない。対象月の承認済み日報が存在する従業員を全員出力する。

## 3. 集計仕様

| 原本項目 | 取得元・計算方法 |
|---|---|
| 日別総支給 | `daily_report.estimated_gross_pay_amount` |
| 月合計 | 日別総支給の月合計 |
| 法定準備金 | 日報控除明細の`LEGAL_DEPOSIT` |
| 貯金 | `daily_report.saving_amount` |
| 引出 | `daily_report.saving_withdrawal_amount`をマイナス表示 |
| 前借り | 日報控除明細の`SHORT_TERM_ADVANCE`（短期貸付の返済額） |
| 借入金返済額 | `daily_report.loan_repayment_amount` |
| 寮費 | 日報控除明細の`DORMITORY_FEE` |
| 携帯電話貸出料 | 日報控除明細の`MOBILE_RENTAL` |
| Wi-Fi使用料 | 日報控除明細の`WIFI_FEE` |
| その他 | 上記項目、所得税、貯金、借入金返済額以外の日報控除明細 |
| 控除合計 | 法定準備金＋貯金－引出＋前借り＋借入金返済額＋寮費＋携帯電話貸出料＋Wi-Fi使用料＋その他 |
| 差引支給額 | 月合計－控除合計 |

`引出`は従業員へ支払う金額のためマイナス値で表示する。このため控除合計が減り、差引支給額が増える。独立表示する項目は`その他`へ重複計上しない。

## 4. DB・Renderer資産

```text
backend/src/main/resources/sql/system/excelbook/labor_cost_payment_v1.sql
backend/src/main/java/com/project/backend/features/operation/book/service/LaborCostPaymentSpreadsheetRenderer.java
```

設定値：

| 設定 | 値 |
|---|---|
| bookCode | `LABOR_COST_PAYMENT` |
| rendererKey | `LABOR_COST_PAYMENT_V1` |
| dataSourceCode | `LABOR_COST_PAYMENT_LEDGER` |
| selectionMode | `NONE` |
| generationUnit | `ONE_FILE` |
| paperSize | `A4` |
| orientation | `LANDSCAPE` |
| fitToOnePage | `true` |

## 5. 保存先

```text
documents/generated-reports/
  ledgers/{tenantId}/LABOR_COST_PAYMENT/{yyyy-MM}/
    LABOR_COST_PAYMENT-{yyyy-MM}.json
```

締め前の再生成では同じ月次パスを更新する。締め後は確定済み台帳を参照・印刷する。

## 6. 本番適用前の確認

- 従業員契約の支払い方法が正しく設定されていること。
- 日報給与Ruleで日別総支給額が正しく保存されていること。
- 法定準備金、貯金、前借り、借入金返済額、寮費、携帯電話貸出料、Wi-Fi使用料、その他の月合計を代表者で照合すること。
- 引出がマイナス表示され、差引支給額を増やすこと。
- 独立表示した控除が`その他`へ重複計上されないこと。
- 10名・11名の場合でも1つの支払周期シートへまとまること。
- A4横のブラウザ印刷で各シートが1ページに収まること。
