# 帳票編集ワークスペース

Jasper Reportと、それに対応するView・ストアドを、アプリ本体の作業と分離して編集するための領域です。

## 編集する場所

- Jasper Report: `report-design/jasper/working/`
- View・ストアド: `report-design/sql/working/`

`backend/src/main/resources/` 配下はアプリが利用する正式ファイルです。通常の手作業では直接編集せず、上記のworking側を編集して反映コマンドを使います。

## 普段の流れ

1. 作業前に `npm run reports:workspace:status` で状態を確認します。
2. `report-design/**/working/` の対象ファイルを編集します。
3. `npm run reports:workspace:check` で反映予定と競合を確認します。
4. `npm run reports:deploy:local` でLocalへ反映して確認します。
5. Local確認後、`npm run reports:deploy:aws` でAWSへ反映します。

AWSコマンドは反映先を表示し、`DEPLOY AWS` の入力後に実行します。AWSのログイン状態、RDSとEC2の起動が必要です。

## 各コマンド

| コマンド | 用途 |
| --- | --- |
| `npm run reports:workspace:init` | 不足しているworkingファイルと比較基準を準備 |
| `npm run reports:workspace:status` | working・正式ファイル・前回同期時点の差分を表示 |
| `npm run reports:workspace:check` | 競合を確認し、反映内容を事前表示 |
| `npm run reports:workspace:sync` | workingを正式ファイルへ同期（環境には未反映） |
| `npm run reports:deploy:local` | 同期、Backend再構築、帳票用SQLのLocal DB反映を一括実行 |
| `npm run reports:deploy:aws` | 同期、帳票用SQLのAWS DB反映、JasperのS3反映を一括実行 |

AWS反映は帳票資産だけを対象にします。進行中のFrontend・Java・他ドメインの変更を一緒にデプロイしません。JasperはS3から実行時に読み込まれるため、アプリ全体のイメージ再構築も不要です。

## コンフリクト防止

`workspace-state.json` に前回同期時点のSHA-256を保持しています。

- workingだけ変更: 正式ファイルへ反映できます。
- 正式ファイルだけ変更: workingを自動的に最新化します。
- 正式ファイルとworkingが同じ内容: 同期済みとして基準を更新します。
- 両方を別内容へ変更: **競合として自動停止**し、どちらも上書きしません。

workingから正式ファイルへ反映する際は、反映前の正式ファイルを `report-design/.workspace-backups/` に自動退避します。このフォルダはGit管理外です。AWS上のJasperはS3バージョニングでも以前の版を保持します。

このチャットへ同じ帳票の修正を依頼するときは、可能なら編集中のファイル名を伝えてください。伝え忘れても、両側が変更された場合は反映コマンドが停止するため、無言で上書きされません。

## 注意事項

- Jasperに新しいfieldを追加する場合は、対応するView／ストアドが同名の列を返す必要があります。
- SQLの列名変更はHTML PreviewやJava DTOにも影響することがあります。レイアウトだけの変更か、データ契約の変更かを分けて確認してください。
- AWS DB反映は `workspace-assets.json` に登録した帳票用SQLだけを依存順に適用します。その他の未完成なSQL変更は含みません。
- 新しい帳票資産を追加した場合は `workspace-assets.json` へworkingと正式ファイルの対応を追加します。

## 既存workingの退避

仕組み導入前に正式版と差があった旧 `daily_pay_slip.jrxml` は、`report-design/jasper/archive/pre-workspace-2026-09-19/` に保存しています。現在のworkingは、現行の正式版から開始するよう揃えてあります。
