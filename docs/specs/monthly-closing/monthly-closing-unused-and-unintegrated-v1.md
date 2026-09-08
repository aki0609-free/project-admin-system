# 月次締め 未使用・未連携・不整合調査 V1

## 1. 概要画面の期間が暦月固定

締め実行は会社給与締日から期間を解決するが、`MonthlySummaryService`は対象月1日～月末の日報・日次支払を集計する。15日締め等では画面概算と実際の締め対象が一致しない。概要も`MonthlyClosingPeriodService`を使う必要がある。

## 2. 概要計算と確定Viewの二重ロジック

概要はJavaで総支給・控除・日払い・月末見込を計算し、確定帳票は月次View/historyを使用する。控除・税・残高・丸めが増えるほど差異が生じるため、概要も月次Viewの集計を正本に寄せる。

## 3. 出力項目が一括完了

`monthly_closing_item`はhistory件数やファイル情報を持てるが、現行Jobは項目ごとの開始・完了・metadata更新を行わず、全Job成功後に`completeItems`が残りをまとめて完了する。どの帳票で失敗したか、何行・どのファイルを確定したかを項目から追えない。

## 4. 必須・任意出力の扱い

業務Workflow内で任意出力も例外を投げればTransaction全体が失敗する。`required_flag=false`を本当に任意にするなら、項目単位で例外を記録して次へ進む制御が必要である。

## 5. 未接続の`open`フロントAPI

`useOpenClosingMutation.ts`は`POST /api/operation/monthly/open`を呼ぶが、Controllerに対応Endpointがなく、現行Pageでも使われていない。旧締め解除機能の残存コードとして削除候補である。

## 6. Storage副作用のrollback

DB Transaction失敗時も、既にS3/Localへ保存されたファイルは自動rollbackされない。DBファイル履歴がrollbackして孤立Objectが残る可能性がある。Version一時領域→成功後確定、または失敗時cleanupを検討する。

## 7. 台帳ファイルと`monthly_closing_report_files`

REPORTは保存ファイル履歴へ記録するが、LEDGERは台帳Storageへ直接保存し、同じ`monthly_closing_report_files`へは登録しない。締め項目にfile metadataも入らないため、確定台帳の一覧・監査経路を台帳パス規則に依存している。

## 8. 出力定義の整合性

有効定義が0件なら締めを拒否するが、REPORT定義とPreview/Job、LEDGER定義と台帳Master/Template/Catalog/Rendererの全整合性は実行時に初めて判明する。締め前Readiness APIを追加する。

## 9. 締日設定の再締め時変更

再締めは現在の締日設定で期間を再解決する。締日設定を後から変えると旧Versionと新Versionで対象期間自体が変わる。意図した仕様かを画面確認に表示し、変更時警告を行う。

## 10. V1判断

| 優先度 | 課題 | 推奨 |
|---|---|---|
| 高 | 概要と締め期間の不一致 | 共通Period Serviceへ統一 |
| 高 | 概要と確定Viewの二重計算 | View基準へ統一 |
| 高 | 項目個別状態・ファイルmetadata未更新 | Job単位更新を実装 |
| 高 | 締め前Readiness | 全出力定義を事前検査 |
| 中 | 孤立Storage | 一時領域/cleanup設計 |
| 対応候補 | 未使用`open` mutation | 参照確認後削除 |
| 仕様確認 | 再締め時の期間変更 | 警告・監査方針を決定 |
