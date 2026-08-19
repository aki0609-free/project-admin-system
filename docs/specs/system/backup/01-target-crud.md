# system/backup 詳細設計 01 — バックアップ対象設定 CRUD

## 1. 対象範囲

この文書は `system/backup` のうち、**バックアップ対象設定の新規登録・編集・削除**だけを対象とする。

対象経路:

```text
BackupTargetEditDialog.vue
→ useBackupTargetEditDialog.ts
→ useBackupPage.ts
→ toBackupTargetSaveRequest()
→ useCreate/Update/DeleteBackupTargetMutation.ts
→ BackupController#create/update/delete
→ BackupTargetCommandService
→ BackupTargetValidator
→ BackupTargetMapper
→ BackupTargetRepository
→ BackupTarget / BackupColumn
→ MySQL
```

基準コードは `main` / `12c91a72b409df16b9d4be0b416247a07a8f170a`。

---

## 2. 権限

**確定仕様**: `BackupController` はクラス単位で

```java
@PreAuthorize("hasRole('SYS_ADMIN')")
```

を持つため、対象設定の一覧・詳細・作成・更新・削除はいずれも `SYS_ADMIN` Roleが必要。

Frontend側の画面制御だけではなくBackend Method Securityでも制限される。

---

## 3. 画面

### 3.1 編集ダイアログ

主要ファイル:

`frontend/src/features/system/backup/components/BackupTargetEditDialog.vue`

ダイアログは2タブ。

- `基本情報`
- `Column`

`DetailDialogLayout` のFooterには `useBackupTargetEditDialog()` からボタン定義を渡す。

### 新規作成時

- タイトル: `バックアップ対象新規作成`
- `削除` ボタン: 非表示
- `閉じる`
- `保存`

### 編集時

- タイトル: `バックアップ対象編集`
- `削除`
- `閉じる`
- `保存`

**実装事実**: `formModel.id > 0` を編集モード判定に使用する。

---

## 4. 新規フォーム初期値

`backupFormFactory.ts#createEmptyBackupTargetForm()` の初期値:

| 項目 | 初期値 |
|---|---|
| id | `0` |
| targetCode | `''` |
| targetName | `''` |
| tableName | `''` |
| description | `''` |
| outputMode | `DOWNLOAD` |
| outputDir | `''` |
| fileNamePattern | `''` |
| zipRequired | `false` |
| backupEnabled | `true` |
| activeFlag | `true` |
| includeHeader | `true` |
| columns | `[]` |

Column新規行には一時的な負数IDを付与する。

`createEmptyBackupColumnForm()` はモジュールスコープの `tempColumnId` を `-1, -2, ...` と減算して使用する。

**実装事実**: この一時IDはFrontend表示・行識別用であり、Backend保存時には `id <= 0` のものを `null` に変換する。

---

## 5. 基本項目

`useBackupTargetBasicFields()` が画面項目を定義する。

### 編集可能項目

- 対象名 `targetName`
- 出力方法 `outputMode`
- 保存先サブフォルダ `outputDir`
- ファイル名パターン `fileNamePattern`
- ヘッダーを含める `includeHeader`
- ZIP出力 `zipRequired`
- バックアップ対象 `backupEnabled`
- 有効 `activeFlag`
- description

### 作成後にReadonlyになる項目

- `targetCode`
- `tableName`

Frontendでは `formModel.id > 0` のときReadonly。

Backendでも `BackupTargetValidator#validateImmutableFields()` が再確認するため、APIを直接呼んでも変更できない。

**確定仕様**: 作成後の `targetCode` と `tableName` は変更不可。

---

## 6. 出力方法

Frontendで選べる値:

- `DOWNLOAD`: ダウンロードのみ
- `SERVER_FILE`: ストレージ保存のみ
- `BOTH`: 保存＋ダウンロード

`outputMode === DOWNLOAD` の場合、Frontendでは `outputDir` 入力欄をdisabledにする。

`SERVER_FILE` または `BOTH` の場合は画面上に、書類管理の「バックアップ → system → テナント」配下のサブフォルダを指定する旨を表示する。

Backend Mapperでは `DOWNLOAD` の場合 `outputDir` を強制的に `null` にする。

**確定仕様**: `SERVER_FILE` / `BOTH` の場合 `outputDir` は必須。

---

## 7. Frontend Validation

`useBackupTargetSchema.ts` はZodで基本情報を検証する。

### targetCode

```regex
^[A-Z][A-Z0-9_]{1,99}$
```

したがって2〜100文字で、先頭英大文字、その後は英大文字・数字・underscore。

### targetName

- 必須
- 200文字以内

### tableName

Frontend regex:

```regex
^[A-Za-z][A-Za-z0-9_]{0,199}$
```

### description

- 500文字以内

### outputDir

- 500文字以内
- 絶対パス不可
- 空segment不可
- `.` / `..` 不可
- `SERVER_FILE` / `BOTH` の場合必須

### fileNamePattern

- 200文字以内
- `/` `\` 不可
- `.csv` で終了
- `{timestamp}` 必須

### 注意

**実装事実**: `backupTargetSchema` の `columns` は `z.array(z.any())` であり、Column詳細の妥当性はこのSchemaでは保証しない。

Columnは別ComposableのSchemaとBackend `BackupTargetValidator` が最終防御を担う。

---

## 8. 保存前Frontend変換

`useBackupPage#onSaveTarget()` は、まず

`toBackupTargetSaveRequest(form)`

を呼ぶ。

この変換で:

- targetCode trim
- targetName trim
- tableName trim
- description blank → null
- outputDir blank → null
- fileNamePattern blank → null
- Columnを `orderNo ASC` でsort
- columnName trim
- csvHeaderName trim
- Columnの負数/0 ID → null

を行う。

その後:

```text
form.id > 0
  → updateTargetMutation
else
  → createTargetMutation
```

となる。

成功後はダイアログを閉じる。

---

## 9. 新規登録API

### Frontend

`useCreateBackupTargetMutation()`

```http
POST /api/system/backup/targets
```

Body: `BackupTargetSaveRequest`

成功時:

```text
queryKeys.backup.all をinvalidate
```

一覧等を再取得可能な状態にする。

### Backend Controller

`BackupController#create(BackupTargetSaveRequest request)`

→ `BackupTargetCommandService#create()`

### Service

処理順:

```text
1. BackupTargetValidator.validate(request, null)
2. new BackupTarget()
3. BackupTargetMapper.updateEntityFromRequest()
4. repository.save(entity)
5. mapper.toResponse(saved)
```

`@Transactional`。

---

## 10. 更新API

### Frontend

`useUpdateBackupTargetMutation()`

```http
PUT /api/system/backup/targets/{id}
```

成功後:

- `queryKeys.backup.all` invalidate
- `queryKeys.backup.detail(id)` invalidate

### Backend

`BackupTargetCommandService#update()`:

```text
1. BackupTargetLookupService.find(id)
2. BackupTargetValidator.validate(request, id)
3. BackupTargetMapper.updateEntityFromRequest(request, entity)
4. repository.save(entity)
5. mapper.toResponse(saved)
```

`@Transactional`。

### テナント境界

`BackupTargetLookupService#find()` は通常の `findById(id)` ではなく:

```text
findByIdAndTenantIdAndDeletedAtIsNull(id, TenantContext.tenantId)
```

を使用する。

**確定仕様**: 別テナントのBackupTarget IDを指定しても、このLookup経路では取得できない。

---

## 11. Backend Validation

中心は `BackupTargetValidator#validate(request, id)`。

順序:

```text
validateRequest
→ validateImmutableFields
→ validateDuplicate
→ validateOutputSetting
→ BackupSchemaInspector.inspect(tableName)
→ validateColumns
```

### 11.1 targetCode

Backend regex:

```regex
[A-Z][A-Z0-9_]{1,99}
```

`Matcher#matches()` のため全体一致。

### 11.2 重複

新規:

```text
existsByTenantIdAndTargetCodeAndDeletedAtIsNull
```

更新:

```text
existsByTenantIdAndTargetCodeAndIdNotAndDeletedAtIsNull
```

したがって重複判定はテナント単位。

### 11.3 tableName実在確認

`BackupSchemaInspector.inspect(request.tableName())` を呼ぶ。

**確定仕様**: 保存時に実DB Schemaを照会するため、存在しないtableNameは登録できない。

画面にも「保存時に実DBのテーブル・カラムと照合」と明示されている。

### 11.4 Column存在確認

各 `columnName` について:

```text
schema.containsColumn(columnName)
```

を確認する。

存在しないColumnは保存不可。

### 11.5 Column重複

同一Target内で以下を重複不可としている。

- `columnName`（大文字小文字を無視）
- `csvHeaderName`
- `orderNo`

### 11.6 最低1件

- columns自体が1件以上必要
- `exportFlag != false` のColumnが最低1件必要

### 11.7 orderNo

1以上。

### 11.8 outputDir

`SERVER_FILE` / `BOTH` 時必須。

相対パスのみ許可し:

- `/` 始まり不可
- 空segment不可
- `.` 不可
- `..` 不可
- ISO control character不可

### 11.9 fileNamePattern

指定する場合:

- 200文字以内
- path separator不可
- `.csv` 必須
- `{timestamp}` 必須
- 使用可能なplaceholderは `{targetCode}`, `{timestamp}` のみ

**重要**: `{targetCode}` 自体は必須ではないが `{timestamp}` は必須。

---

## 12. Mapperのデフォルト値

`BackupTargetMapper#updateEntityFromRequest()` ではnull値に対し以下を補完する。

| 項目 | Default |
|---|---|
| backupEnabled | true |
| activeFlag | true |
| outputMode | DOWNLOAD |
| zipRequired | false |
| includeHeader | true |

Column:

| 項目 | Default |
|---|---|
| exportFlag | true |
| orderNo | 1 |

ただしValidatorが事前実行されるため、実際にはBackend APIからの不正nullがMapper defaultまで到達しない項目もある。

---

## 13. Column更新方式

これは本CRUDの重要実装。

`BackupTargetMapper#afterUpdateEntityFromRequest()` は更新時も:

```text
entity.clearColumns()
→ request.columns()を全件toColumnEntity()
→ entity.addColumn()
```

を行う。

`BackupTarget#columns` は:

```java
@OneToMany(
  mappedBy = "target",
  cascade = CascadeType.ALL,
  orphanRemoval = true
)
```

である。

したがって、**Columnは個別差分更新ではなく全置換方式**。

Frontend RequestにはColumn `id` が存在するが、`BackupTargetMapper#toColumnEntity()` は `id` をignoreする。

**確定仕様（実装上）**: 更新Requestに既存Column IDを送っても、Backend MapperはそのIDをEntityへ継承しない。

### 影響

- Column IDは更新のたび変わる可能性が高い。
- 他テーブルがBackupColumn IDを外部キーとして参照する設計とは相性が悪い。
- 履歴等でColumn IDを業務識別子として保存すると整合が崩れる。

**V2候補**: IDを維持する差分mergeが必要か、Columnを完全なvalue objectとして「ID非保証」と仕様化するか決める。

---

## 14. BackupTarget Entity

Table: `backup_target`

主な列:

- id
- tenant_id
- target_code
- target_name
- table_name
- description
- backup_enabled
- active_flag
- output_mode
- output_dir
- file_name_pattern
- zip_required
- include_header
- created_at
- updated_at
- deleted_at

Unique constraint:

```text
(tenant_id, target_code)
```

### 注意

Entityのunique constraint自体には `deleted_at` が含まれない。

一方Application Validationは `deletedAtIsNull` のみを重複扱いする。

この差は削除後の同一targetCode再登録に関係するため、DB実DDLを確認する必要がある。

**未決事項**: DBの実unique indexもEntity定義通りなら、soft delete後も同一tenant/targetCodeで物理行が残るため再登録時にDB unique violationとなる可能性がある。

これは次回DB実DDL確認で確定する。

---

## 15. BackupColumn Entity

Table: `backup_column`

主な列:

- id
- target_id
- tenant_id
- column_name
- csv_header_name
- data_type
- export_flag
- order_no
- created_at
- updated_at
- deleted_at

Unique constraint:

```text
(target_id, column_name)
(target_id, order_no)
```

Application Validatorはさらに `csvHeaderName` の重複も禁止するが、Entity上にはそのUnique constraintはない。

**実装事実**: csvHeaderName重複禁止はApplication Validationのみで保証している。

---

## 16. 削除

### Frontend

編集モードのみ削除ボタンを表示する。

`useBackupPage#onDeleteTarget()` は:

1. `id <= 0` ならAPIを呼ばず閉じる
2. `window.confirm("「targetCode」を削除しますか？")`
3. OKなら `DELETE /api/system/backup/targets/{id}`
4. 成功後ダイアログを閉じる

Mutation成功後は `queryKeys.backup.all` をinvalidate。

### Backend

`BackupTargetCommandService#delete()`:

```text
1. lookupService.find(id)
2. Instant now = Instant.now(clock)
3. target.deletedAt = now
4. 全columns.deletedAt = same now
5. Transaction終了時dirty checking
```

明示的 `repository.delete()` は呼ばない。

**確定仕様**: 物理削除ではなくsoft delete。

### Clock

削除処理は共通DI `Clock` を使用し:

```java
Instant.now(clock)
```

で時刻を作る。

Targetと全Columnに同じ `Instant` を設定する。

`BackupTargetCommandServiceTest#delete_shouldApplySameClockToTargetAndColumns()` がこの挙動を保証する。

---

## 17. backupEnabled / activeFlag の意味

`BackupDefinitionService#findBackupEnabledTargets()` と `getBackupTargetDefinition()` は:

```text
backupEnabled = true
AND activeFlag = true
AND deletedAt IS NULL
```

のみを対象とする。

そのためどちらかをfalseにすると、実行可能なバックアップ定義から除外される。

### 重要な画面挙動

**実装事実**: `BackupTargetQueryService#findAll()` は `findBackupEnabledTargets()` を使用しているため、一覧画面自体も `backupEnabled=true AND activeFlag=true` のTargetだけ返す。

したがって管理画面で `backupEnabled=false` または `activeFlag=false` にして保存すると、そのTargetは通常一覧から見えなくなる。

**未決事項**: これは「無効設定を画面から再有効化できなくなる」ため、業務意図なのか要確認。

**V2候補**: 管理一覧はdeletedAtのみ除外し、実行対象一覧だけenabled/active filterをかける責務分離。

---

## 18. Error処理

`BackupController#create/update` のRequestには `@Valid` が付与されていない。

DTO recordにもJakarta Validation annotationは付いていない。

この機能では `BackupTargetValidator` が明示Validationを担当する。

ただしValidatorの例外は多くがplain `RuntimeException`。

共通 `GlobalExceptionHandler` ではplain RuntimeExceptionは汎用Exception handlerへ入り、通常 `COMMON_INTERNAL_ERROR` 扱いになる。

**実装事実**: 入力不備でもHTTP上Business/Validation ErrorではなくInternal Errorとして扱われる可能性がある。

**V2候補**:

- 専用BusinessException/ErrorCode化
- またはDTO Bean Validationとの責務整理

今回は修正しない。

---

## 19. Transaction

### create

`@Transactional`

Validation → Target/Column生成 → saveまで同一Transaction。

### update

`@Transactional`

Target取得 → Validation → Column全置換 → saveまで同一Transaction。

Column入替途中で例外が発生した場合はTransaction rollback対象。

### delete

`@Transactional`

Target + 子Columnのsoft deleteを同一Transactionで反映。

**確定仕様**: Targetだけ削除されColumnだけ残る、またはその逆になることをTransaction境界で防いでいる。

---

## 20. 現在のテスト

### `BackupTargetCommandServiceTest`

現在確認できるテストは削除Clockのみ。

保証内容:

- Target.deletedAtにClockの固定Instantを使用
- 子Columnにも同じInstantを使用

### `BackupTargetValidatorTest`

Validator専用テストが存在する。

対象ファイル:

`backend/src/test/java/com/project/backend/features/system/backup/service/validation/BackupTargetValidatorTest.java`

詳細ケース一覧は次の「Column/Validation詳細」で精査する。

### 不足しているテスト

今回のCRUD範囲では少なくとも以下が不足。

- create Service成功系
- update Service成功系
- Column全置換挙動
- Target/Column cascade + orphanRemoval DB統合テスト
- soft delete後の同一targetCode再登録
- 別Tenant IDへのupdate/delete拒否
- `backupEnabled=false` 保存後の一覧非表示挙動
- `activeFlag=false` 保存後の一覧非表示挙動
- Controller `SYS_ADMIN` Role contract
- API error status / ErrorCode contract
- Frontend新規・編集・削除Composableテスト

---

## 21. 今回見つかった既知事項

### A. Column IDは更新時に維持されない

重要度: 中〜高

MapperがColumn IDをignoreし全置換する。

今回は修正しない。

### B. 無効化したTargetが管理一覧から消える

重要度: 高

一覧取得自体が `backupEnabled=true && activeFlag=true` に限定されるため、無効化後に画面から再編集できない可能性がある。

今回は修正しない。

### C. soft deleteとDB Unique constraintの整合

重要度: 要確認

Applicationでは削除済みコードを重複対象外とするが、Entity unique `(tenant_id,target_code)` はdeletedAtを考慮しない。

DB実DDL確認が必要。

### D. Validation ErrorのHTTP分類

重要度: 中

専用Validatorがplain RuntimeExceptionを投げるため、利用者入力不備がInternal Errorになる可能性。

今回は修正しない。

---

## 22. 次に掘る範囲

次はさらに小さく、**Column編集とSchema Validation**だけを掘る。

```text
useBackupColumnEditor.ts
→ Column frontend schema
→ BackupTargetValidator#validateColumns/validateColumn
→ BackupSchemaInspector
→ information_schema / DB metadata
→ BackupColumn mapping
```

ここで以下を確定する。

- Column追加・削除・並べ替え
- dataTypeの意味
- exportFlag
- DB型との整合確認の有無
- tenant_id等のシステムColumnを選択可能か
- Schema InspectorがどのDB metadataを参照するか
- Validatorテストで何を保証しているか
