# 台帳管理 未使用・未連携・不整合調査 V1

## 1. 会計年度開始月が画面固定

対応済み。`BookOperationPage.vue`は`GET /api/operation/excel-books/settings`から業務管理の`fiscal_year_start_month`を取得し、年度と12か月の候補を構築する。

## 2. 台帳マスターと月次締めは別設定

台帳一覧への登録と、月次締め出力定義への登録は別である。Template・Catalog・Rendererが揃っても出力定義がなければ締め時に生成されない。

一覧取得時のReadiness判定でTemplate・Catalog・Renderer・変数Mapping・選択設定を検証し、月次締め定義との一致も画面へ表示するよう対応済み。配置資産そのものの起動時検証は、最終自動配置スクリプトと合わせて実装する。

## 3. 固有Renderer依存

単純な`${列名}`差込みは汎用Rendererで吸収できるが、複数Sheet、可変行、結合セル、手入力保持、1人1ファイル等は固有Rendererが残る。V1ではRenderer Registry境界に閉じ込める方針を維持し、生成Serviceへ帳票固有条件を追加しない。

## 4. 複数生成結果の結合はPreview限定

画面の全員分生成では複数WorkbookのSheetをブラウザ上で結合して表示するが、Storageには従業員別ファイルのまま保存する。結合結果は編集不可で、表示上の`storagePath`も実パスではない。

大量従業員ではWorkbook JSONサイズ・ブラウザメモリが増える。全員印刷はサーバー側PDF/Excel結合を別機能として扱う方が安全である。

## 5. `output_file_path`の実利用

生成Serviceは`DocumentStorageKeyResolver`で保存先を組み立てる。マスターの`output_file_path`が実際の生成先へ反映されていない場合、誤解を招く設定となる。利用箇所を再確認し、未使用なら廃止または説明変更する。

## 6. `source_type`の未対応値

V1生成は`SNAPSHOT`だけで、他Typeは例外になる。画面上で選択できる場合は`SNAPSHOT`へ制限し、将来値を登録できないようにする。

## 7. 台帳からPDFへの変換

Spreadsheet PreviewとJSON保存は存在するが、全台帳に共通する高精度なサーバーPDF変換は保証されていない。印刷要件が厳密な帳票はJasper、編集可能台帳はSpreadsheetと役割を分ける。

## 8. 締め確定項目と手入力保持

手入力保持はRenderer実装に依存する。新しい台帳で手入力セルを追加した場合、`preserveManualInputs`を実装しなければ再生成・再締めで失われる。Template変更時の互換テストも必要である。

## 9. V1判断

| 優先度 | 課題 | 推奨 |
|---|---|---|
| 対応済み | 一覧での台帳・Template・Catalog・締め定義の不一致確認 | 生成可否と締め連携を表示 |
| 対応済み | 年度開始月固定 | 業務管理設定へ接続 |
| 最終配置時 | SQL・Templateの配置漏れ | 自動配置スクリプトで起動前検証 |
| 高 | 手入力保持の帳票別検証 | 代表台帳E2E/Testcontainers |
| 中 | 未使用可能性のあるパス設定 | 調査後整理 |
| 中 | 全員分の大容量Preview | サイズ計測と上限表示 |
| 仕様確定 | 特殊台帳Renderer | Registry内に限定して維持 |
