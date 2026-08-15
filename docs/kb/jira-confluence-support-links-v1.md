# Jira Incident報告・Confluenceマニュアル連携 V1

## 1. 目的

ProjectAdminのユーザーメニューから、ConfluenceのマニュアルとJiraの不具合報告フォームを安全に別タブで開けるようにする。

V1ではAtlassian API、iframe、Jira Work item collectorのJavaScriptは利用しない。Atlassian Cloudが提供する正式URLへ遷移するだけとし、ProjectAdminの認証・業務処理から分離する。

## 2. ユーザーメニュー

```text
会社情報
マニュアル
不具合・Incident報告
ログアウト
```

### マニュアル

- ConfluenceのProjectAdminSystemマニュアルトップを開く
- 新しいタブで表示する
- Confluence Freeでは匿名アクセスできないため、必要に応じてAtlassianへログインする

### 不具合・Incident報告

- Jira Formsで作成したFUYOプロジェクトのフォームを開く
- 新しいタブで表示する
- フォームの項目・必須入力・作成先はJira側で管理する

## 3. 管理画面からの設定

SYS_ADMINで次を開く。

```text
管理者メニュー
→ 業務管理
→ その他設定
→ サポートリンク
```

設定項目：

- インシデント報告のURL
- マニュアルのURL

「その他設定を保存」を押すとDBへ保存され、次に画面を読み込んだ時点からログイン済みの全ユーザーへ反映される。URL変更のためにFrontendを再ビルドする必要はない。

保存できるのはSYS_ADMINだけとする。ヘッダーメニューからの参照はログイン済みの全ユーザーに許可する。

## 4. 既定値と環境変数

DB設定がまだ存在しない場合や一時的に取得できない場合は、Frontendの既定値を使用する。環境ごとに既定値を変更する場合は次の環境変数を使用する。

```text
VITE_CONFLUENCE_MANUAL_URL
VITE_JIRA_INCIDENT_FORM_URL
```

URLは秘密情報ではない。通常運用では管理画面から変更する。

現在のProjectAdmin用URLは共通設定ファイルの既定値としてGit管理する。会社別環境では環境変数で上書きできる。

DBにはHTTPS URLだけを保存できる。

## 5. セキュリティ

- `https:`のみ許可する
- `target="_blank"`で別タブ表示する
- `rel="noopener noreferrer"`を指定する
- JWT、ユーザーの個人情報、給与情報をURLへ付加しない
- Jiraフォームへスクリーンショットを添付する場合、個人情報を確認してから送信する

## 6. V1で行わないこと

- Confluenceページのiframe表示
- Confluence REST APIによる本文取得
- Jira Collector JavaScriptの埋め込み
- ProjectAdminからJira APIを直接呼び出す処理
- Jiraフォームへログインユーザー情報を自動送信する処理

## 7. 確認項目

1. ユーザーメニューに2項目が表示される
2. マニュアルを押すとConfluenceが別タブで開く
3. Incident報告を押すとJira Formsが別タブで開く
4. 元のProjectAdmin画面が維持される
5. Jiraフォーム送信後、FUYOプロジェクトへ作業項目が作成される
6. SYS_ADMINがその他設定からURLを変更できる
7. 設定変更後に再ログインまたは画面再読込すると新しいURLが使用される
8. HTTPS以外のURLを保存できない
