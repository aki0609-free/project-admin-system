# 労務費支払一覧 Spreadsheet台帳 V1

## 1. 目的

対象月の従業員別総支給額と控除額を、支払い方法別に一覧表示する。

- 支払い方法は`employee_contract.payment_cycle`で分類する。
- 日払い、週払い、月払いをそれぞれ別シートにする。
- 1シートは最大10名とし、11名以上は自動的に次シートへ分割する。
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
| 所得税 | 日報控除明細の`INCOME_TAX` |
| 借金・引出 | 当月の承認済み`employee_loan.principal`をマイナス表示 |
| 貯金・返済 | `daily_report.saving_amount + loan_repayment_amount` |
| その他控除 | 所得税・貯金・借入返済以外の日報控除明細 |
| 控除合計 | 所得税＋借金・引出＋貯金・返済＋その他控除 |
| 差引支給額 | 月合計－控除合計 |

`借金・引出`は、新規貸付や引出によって従業員へ渡す金額をマイナス値として扱う。このため控除合計が減り、差引支給額が増える。

## 4. V1制約

貯金の引出履歴を保存する取引テーブルがないため、V1では貯金引出を0円とする。当月の承認済み新規貸付だけを`借金・引出`へ反映する。

将来、従業員金融取引履歴を追加した場合は、Viewの`borrow_withdrawal_amount`だけを差し替える。Rendererの変更は不要とする。

## 5. DB・Renderer資産

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

## 6. 保存先

```text
documents/generated-reports/
  ledgers/{tenantId}/LABOR_COST_PAYMENT/{yyyy-MM}/
    LABOR_COST_PAYMENT-{yyyy-MM}.json
```

締め前の再生成では同じ月次パスを更新する。締め後は確定済み台帳を参照・印刷する。

## 7. 本番適用前の確認

- 従業員契約の支払い方法が正しく設定されていること。
- 日報給与Ruleで日別総支給額が正しく保存されていること。
- 所得税、貯金、借入返済、その他控除の月合計を代表者で照合すること。
- 当月の新規貸付がマイナス表示されること。
- 10名・11名の場合の改ページを確認すること。
- A4横のブラウザ印刷で各シートが1ページに収まること。
