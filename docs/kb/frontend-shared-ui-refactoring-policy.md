# フロントエンド共通UIリファクタリング方針

## 1. 目的

ProjectAdminSystem V1の画面ごとに生じている入力欄、Dialog、Toolbar、一覧レイアウトの差異を、既存の共通基盤へ段階的に統一する。

既存画面は一括置換せず、1ドメインずつ動作確認して移行する。移行完了までは旧Componentを残し、全参照がなくなった時点で削除する。

## 2. 標準Component

| 用途 | 標準Component |
|---|---|
| 一覧・詳細ページ | `ListDetailPageTemplate` |
| Dialog | `AppDialog` |
| Toolbar | `AppToolbar` |
| FormとValidation | `FormLayout` |
| Grid Form | `GridBasedForm` |
| 一覧 | `SimpleTable` |

## 3. Formの原則

登録・更新対象となるフォーム項目は、原則として画面へ`v-text-field`、`v-textarea`、`v-select`を直接記述しない。

`FormLayout`と`GridBasedForm`を使用し、Composable側の`GridFormFieldDef`で次を定義する。

- key
- label
- type
- options
- gridColumn
- rows
- autoGrow
- editable
- formatter

既存フォーム基盤は次の入力種別をサポートしている。

```text
text
password
number
select
checkbox
date
month
time
textarea
sqlEditor
selectboxWithChips
dayrule
```

`textarea`は新しいComponentを増やさず、次のように定義する。

```ts
{
  key: 'description',
  label: '説明',
  type: 'textarea',
  rows: 4,
  autoGrow: true,
  gridColumn: '1 / span 4',
}
```

これにより、Vuetifyの表示密度、枠線、エラー表示、readonly、disabled、Zod Validationを共通化する。

## 4. 直書きを許容する例外

次は無理にCRUD Formへ統合しない。

- 検索条件だけの小さな入力欄
- ファイル選択
- JSON、SQL、Spreadsheetなどの特殊Editor
- 表内編集
- slotによる独自表示が必要なSelector
- Formの保存対象ではない一時的な操作パラメータ

例外の場合も、`variant`、`density`、エラー表示、余白を共通画面と合わせる。類似例外が3画面以上に増えた場合は、共通Component化を検討する。

## 5. Dialogのボタン配置

`AppDialog`のFooterを次の役割で分ける。

| 位置 | 操作 |
|---|---|
| 左 | 削除など破壊的操作 |
| 右 | 閉じる、キャンセル、プレビュー、保存、実行 |

右側は原則として、補助操作から主要操作の順に並べる。

```text
閉じる → 保存
キャンセル → 実行
閉じる → プレビュー → 保存
```

## 6. Toolbarのボタン配置

| 位置 | 操作 |
|---|---|
| 左 | 新規作成、追加など主要な開始操作 |
| 右 | 再読込、並べ替え、出力、削除など補助・対象操作 |

`ToolbarItem.intent`を使用し、画面ごとの色指定を減らす。

```text
primary
secondary
danger
warning
utility
```

## 7. 移行完了条件

- 旧`DetailDialogLayout`を参照していない
- 旧`GenericToolbar`を参照していない
- CRUD Formの入力項目がForm基盤へ定義されている
- Zod Validationが維持されている
- DialogとToolbarの左右配置が役割に合っている
- 対象Lintと本番ビルドが成功する
- Dockerへ反映して画面表示を確認する
- 対象ドメインのPlaywright E2Eが成功する
- 機能ロジック、API、保存データを意図せず変更していない

## 8. V1での進め方

```text
1. 現在の画面と共通Component参照を調査
2. 直書きFormと旧Dialog／Toolbarを分類
3. 既存Form定義へ移行
4. AppDialog／AppToolbarへ移行
5. 対象Lint・ビルド
6. Docker反映
7. Playwrightで主要操作と配置を確認
8. 1ドメイン単位でコミット
```

共通基盤へ機能を追加する前に、既存のfield typeやpropsで吸収できないか確認する。単一画面の都合だけで共通Componentへ特殊仕様を追加しない。

## 9. 非同期取得を伴うDialogの状態管理

編集Dialogを開く際は、表示状態の初期化とAPI取得結果の反映を分離する。

- 選択タブ、スクロール位置、開閉状態などのUI状態は、Dialogを開いた直後に一度だけ初期化する
- API応答後はForm値だけを反映し、その間に利用者が変更したタブを上書きしない
- `await`の後に`activeTab = 'basic'`のようなUI初期化を置かない
- 再読込が必要な場合も、利用者が操作中の表示状態を維持できるか確認する

Playwrightの一括回帰テストでは、同じ固定データを更新する業務シナリオを直列実行する。並列実行するテストは、テストごとに独立したデータを生成・削除できるものに限定する。

## 10. 移行状況と保留資産

ダッシュボードのお知らせボードは、共通Toolbar、`AppDialog`、`FormLayout`、`GridBasedForm`へ移行済み。本文のリッチテキストEditorとプレビューは特殊入力のため専用Componentを維持する。お知らせ本文・種別などの詳細内容は閲覧専用レイアウトとして維持する。

ダッシュボードのカレンダー日別Dialogとお知らせ詳細Dialogは、独自`v-dialog`から`AppDialog`へ移行済み。詳細Dialogの編集・削除・閉じる操作は共通Footer Toolbarへ統一した。日別のお知らせ一覧、種別・生成元表示、HTML／Markdown対応の本文Viewerは閲覧専用の業務UIとして維持する。

ユーザー管理・権限管理は、`ListDetailPageTemplate`、共通Toolbar、`SimpleTable`、`FormLayout`、`GridBasedForm`、`AppDialog`へ移行済み。ロール権限選択は複数権限を左右で確認する専用UIのため、`PermissionSelector`をForm内の拡張領域として維持する。

管理者メニューの業務管理は、ページ外枠、退職時文言、年度帳票バックアップ設定、外部サポートリンク、退職TODO Dialogを共通UIへ移行済み。次は業務固有の入力として維持する。

- 給与締日・支払日の`DayRuleField`
- 月次締め帳票の表内編集
- 寮費設定の表内編集
- 手動バックアップ実行時の対象年度

システム運用の台帳管理は、ページ外枠、Toolbar、編集Dialogを共通化済み。Spreadsheet Editor、セル割当、行生成、対象選択は台帳固有機能として専用UIを維持する。

顧客管理・取引管理は、`ListDetailPageTemplate`、共通Toolbar、`SimpleTable`、`AppDialog`、`FormLayout`、`GridBasedForm`へ移行済み。封筒宛名印刷の印刷イメージと入金確認の請求・回収額集計は業務固有の表示として専用領域を維持する。取引管理の顧客絞り込みは保存対象ではない検索条件のため、直書きを許容する例外として扱う。

ヘッダーメニューの会社情報は、`AppDialog`、共通Footer Toolbar、`FormLayout`、`GridBasedForm`へ移行済み。請求書備考、事業内容、許認可・資格情報もForm定義の`textarea`へ統一した。会社概要・振込先・事業内容の閲覧カードは、保存Formではない表示専用領域として維持する。

従業員貸付・貯蓄は、ページ、Toolbar、Table、Dialog、Formを共通UIへ移行済み。承認コメントもForm定義の`textarea`へ統一した。貯蓄の新規作成時は貸付と同様にFormを初期化し、直前に開いた未保存値が残らないようにする。V1対象外の従業員勤務表は変更しない。

従業員の退職処理は、`AppDialog`と共通Footer Toolbarに加え、退職日・備考を`FormLayout`、`GridBasedForm`へ移行済み。管理者設定から取得する警告文、従業員概要、必須TODOチェックリストは退職業務固有の表示・操作として維持する。退職日は必須であり、必須TODOがすべて確認されるまで実行ボタンを無効にする。

従業員編集Dialogの基本情報、給与・税金、契約情報は、旧`toolbox`のFormラッパー依存を外し、`FormLayout`と`GridBasedForm`を直接使用する構成へ移行済み。契約メモもForm定義の`textarea`へ統一した。手当・控除設定タブは`EmployeePayrollItemSettingsPanel`へ分離し、マスター定義から入力欄、残高、明細取引を動的生成する。

外部データ取込は、取込定義の編集DialogとColumn編集に加え、取込履歴のエラー行Dialogを`AppDialog`と共通Footer Toolbarへ移行済み。エラー行は保存Formではなく取込結果の閲覧専用一覧であるため、`SimpleTable`をそのまま使用する。

応募者管理は、ページ外枠、一覧Toolbar、編集DialogとFooterを共通UIへ移行済み。応募者の基本・応募・媒体・属性・入退社情報は既存のForm定義とValidationを維持する。チャートと分析はV1の既存機能を維持し、フィルターToolbarのみ共通化する。

応募媒体管理は、媒体名を行、年月を多段Headerとする編集可能なピボット表を業務固有Componentとして維持する。`SimpleTable`への置換は行わない。ページ外枠、検索・操作Toolbar、媒体／年月の追加・削除Dialogは共通UIへ移行する。移行時は次を回帰確認する。

- 年月別の掲載地域、掲載枠、コストをセル編集できる
- 採用数と単価は参照値として表示される
- 合計行が表示される
- セル編集、媒体追加、年月追加後に未保存状態となる
- チャートが媒体データを基に表示される

V1公開画面のTableは、主要なCRUD一覧について`SimpleTable`への移行が概ね完了している。次の表は専用用途のため、単純に`SimpleTable`へ置換しない。

- 帳票・HTMLプレビュー
- Spreadsheet台帳と選択Dialog
- Form内の明細行編集
- 権限・設定値の小規模な表内編集
- 複数階層HeaderやPivot表示

今後の主な整理対象は、Tableそのものよりも、画面内に直接記述されたCRUD Form、旧Dialog、旧Toolbarである。

`EmployeeTimesheetPage`はコードが存在するが、V1のメニューとRouterには登録されていない。機能公開は仕様変更に当たるため、共通UI移行やメニュー追加を行わず未使用資産として保留する。

手当・控除の日報入力画面は`DailyReportPayrollItemPanel`へ統合し、手当と控除で重複していた金額、Rule基準額、数量、残高、変更理由の表示処理を共通化した。

## 11. 全体監査結果

通常のV1公開画面について、旧Dialog、旧Toolbar、旧ページLayoutの参照を監査した。

### 11.1 通常画面で移行を保留する箇所

| 対象 | 理由 | 対応方針 |
|---|---|---|
| 住民税入力 | 控除マスターと一体で扱う必要がある | 控除仕様確定後に確認 |
| 従業員勤務表 | V1では非公開 | V1の移行対象外 |

### 11.2 専用UIとして維持する箇所

次は共通CRUD画面へ置換しない。

- SpreadsheetテンプレートEditor
- 生成済み台帳のSpreadsheet Viewer／Editor
- 台帳の対象選択Dialog
- JasperReports／HTML帳票プレビュー
- 月次帳票ファイル選択Dialog
- 応募媒体の多段Headerピボット表

専用UIであっても、呼び出し元ページのタイトル、通常Toolbar、余白は共通規約へ合わせる。Editor、Viewer、印刷領域、複数階層Headerなど、業務上必要な内部構造は維持する。

### 11.3 削除済み互換資産

全参照がゼロであることを確認し、次の旧資産を削除した。

- 旧`BaseDialog`とStorybook Story
- 旧`DetailDialogLayout`
- 旧`toolbox/pages/ListDetailPageLayout`
- 重複していた`MultiPositionGenericToolbar`
- 未使用の`DetailPanel`
- 未使用の`SplitEditorTab`
- 旧`BatchGenericToolbar`
- 旧`toolbox/dialog/BatchParameterDialog`
- Storybook初期生成のButton／Header／Page Storyと案内用MDX

`GenericToolbar`、`CardLayout`は手当・控除マスターが使用中のため、現時点では削除しない。

Batch Toolbarは`shared/ui/toolbar/BatchPageToolbar`へ実行・ダウンロード・プレビュー処理を集約し、入力Dialogを`shared/ui/dialog/BatchParameterDialog`へ移行した。Batchパラメータも`FormLayout`、`GridBasedForm`、`AppDialog`を使用し、text、number、date、month、select、checkboxを共通Form基盤で扱う。これにより、通常画面のBatch実行から`toolbox`のToolbar／Dialog依存を除去した。

Storybookは汎用的な初期サンプルを削除し、V1の標準Componentである`AppToolbar`、`AppDialog`、`ListDetailPageTemplate`のStoryを追加した。手当・控除マスターが稼働中の`GenericToolbar`、`CardLayout`のStoryは移行完了まで維持する。

追加監査では、実画面から参照がなくStorybook内だけで完結していた次の試作基盤を削除した。

- `PivotTable`一式
- `GroupedTable`一式
- `GridLayout`、`SplitLayout`、`ToolbarLayout`
- `WizardForm`、`DynamicForm`、`HorizontalForm`
- `NestedObjectForm`、`NestedRepeatableForm`と専用Provider
- 旧`SearchBar`
- 未使用の`LoadingComponent`、`ErrorPage`
- 未使用の`toolbox/toolbar/types`再export

現役の`SimpleTable`、`MultiLevelHeaderTable`、`TabbedForm`、`SectionedForm`、`GridBasedForm`、`CardLayout`、Spreadsheet／帳票専用UIは維持する。

最終監査で`toolbox`に残っていた現役3資産も正式な配置先へ移動した。

| 旧配置 | 新配置 |
|---|---|
| `toolbox/tab/FormGridTab.vue` | `shared/components/form/grid_based_form/FormGridTab.vue` |
| `toolbox/panel/DetailPanelLayout.vue` | `shared/components/layout/detail_panel/DetailPanelLayout.vue` |
| `toolbox/toolbar/utils/toolbarItemFactory.ts` | `shared/ui/toolbar/toolbarItemFactory.ts` |

全importの置換後に`frontend/src/toolbox`配下のファイルが0件であることを確認した。今後、汎用UIを`toolbox`へ追加せず、Form、Layout、Toolbarの責務に応じて`shared`配下へ配置する。

レスポンシブ調整では、720px以下でページHeaderを縦積み、Toolbarの左右領域を折返し、DialogのHeader・本文・Footerの余白を縮小する。Dialogは`100dvh`を基準に最大高さを制限し、本文だけをスクロールさせてFooter操作を画面内に維持する。

### 11.4 現時点の進捗

通常画面への共通UI適用と未使用試作基盤の削除は概ね完了している。フロントエンド整理全体の進捗は約95%とする。残作業は次のとおり。

1. 手当・控除を含む全主要画面の最終E2E回帰
2. 実データ量・実端末を用いた表示確認と軽微な余白調整
3. 現役TableのStorybook型定義と既存の全体型エラー整理

監査時点の主要E2Eは、応募者・応募媒体、業務管理、会社情報、顧客管理、ダッシュボード、従業員編集・貸付・貯蓄・退職、台帳管理入口、外部データ取込、メール、お知らせ、帳票、Rule、ユーザー・権限管理を対象として直列実行し、22件すべて成功した。

Batch Toolbar移行後は、従業員画面の個別日別給与明細を390×844pxで開き、Batch Dialog、支払日、従業員選択、Footer操作がViewport内に収まることをPlaywrightで確認した。従業員編集の既存回帰を含む3件が成功している。本番ビルドとStorybook静的ビルドも成功した。
