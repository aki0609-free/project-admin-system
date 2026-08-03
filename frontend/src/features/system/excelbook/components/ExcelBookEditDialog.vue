<script setup lang="ts">
import { computed, reactive, watch } from 'vue'
import { z } from 'zod'
import DetailDialogLayout from '@/toolbox/dialog/DetailDialogLayout.vue'
import FormLayout from '@/shared/components/form/base/FormLayout.vue'
import GridBasedForm from '@/shared/components/form/grid_based_form/GridBasedForm.vue'
import type { GridFormFieldDef } from '@/shared/components/form/grid_based_form/types/types'
import type { ToolbarItem } from '@/toolbox/toolbar/types/types'
import type {
  ExcelBookMasterForm,
  ExcelBookVariableMapping,
} from '../types/excelBookTypes'
import { useExcelBookDataSourceCatalogsQuery } from '../api/useExcelBookDataSourceCatalogsQuery'
import { createEmptyExcelBookForm } from '../utils/excelBookFactory'

const props = defineProps<{
  modelValue: boolean
  item: ExcelBookMasterForm | null
  saving?: boolean
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (
    e: 'save' | 'delete' | 'edit-template',
    value: ExcelBookMasterForm,
  ): void
}>()

const visible = computed({
  get: () => props.modelValue,
  set: value => emit('update:modelValue', value),
})

const form = reactive<ExcelBookMasterForm>(createEmptyExcelBookForm())
const catalogsQuery = useExcelBookDataSourceCatalogsQuery()

const selectedCatalog = computed(() =>
  catalogsQuery.catalogs.value.find(
    catalog => catalog.sourceCode === form.dataSourceCode,
  ),
)

const selectionCatalog = computed(() =>
  catalogsQuery.catalogs.value.find(
    catalog => catalog.sourceCode === form.selection.dataSourceCode,
  ),
)

const selectionColumnItems = computed(() =>
  (selectionCatalog.value?.columns ?? []).map(column => ({
    title: `${column.displayName} (${column.columnName})`,
    value: column.columnName,
  })),
)

const schema = z.object({
  bookCode: z.string()
    .min(1, 'Book Codeは必須です')
    .regex(
      /^[A-Z0-9][A-Z0-9_-]{0,99}$/,
      '半角英大文字、数字、_、-で入力してください',
    ),
  bookName: z.string().min(1, '名称は必須です'),
  sourceType: z.literal('SNAPSHOT'),
  layoutType: z.enum([
    'REPEATING_ROW',
    'MONTHLY_SUMMARY',
    'DEDICATED',
  ]),
  rendererKey: z.string()
    .min(1, 'Renderer Keyは必須です')
    .regex(
      /^[A-Z0-9][A-Z0-9_-]{0,99}$/,
      '半角英大文字、数字、_、-で入力してください',
    ),
  selection: z.object({
    mode: z.enum(['NONE', 'SINGLE', 'MULTIPLE']),
    dataSourceCode: z.string().nullable(),
    valueColumn: z.string().nullable(),
    displayColumns: z.array(z.string()),
    allowSelectAll: z.boolean(),
    generationUnit: z.enum(['ONE_FILE', 'FILE_PER_SELECTION']),
  }),
  print: z.object({
    paperSize: z.enum(['A3', 'A4', 'B5']),
    orientation: z.enum(['PORTRAIT', 'LANDSCAPE']),
    fitToOnePage: z.boolean(),
  }),
  dataSourceCode: z.string().min(1, 'データソースは必須です'),
  templateSheetName: z.string().min(1, 'テンプレートシート名は必須です'),
  activeFlag: z.boolean(),
  variableMappings: z.array(z.object({
    variableKey: z.string()
      .min(1, '変数キーは必須です')
      .regex(
        /^[A-Za-z][A-Za-z0-9_.]{0,99}$/,
        '英字で始まる英数字・ピリオド・_で入力してください',
      ),
    sourceColumn: z.string().min(1, '参照項目は必須です'),
    scope: z.enum(['CONTEXT', 'ROW']),
    dataType: z.enum([
      'STRING',
      'NUMBER',
      'DATE',
      'DATETIME',
      'BOOLEAN',
    ]),
    orderNo: z.number().int().positive(),
  })),
})

const fields = computed<GridFormFieldDef<ExcelBookMasterForm>[]>(() => [
  {
    key: 'bookCode',
    label: 'Book Code',
    type: 'text',
    gridColumn: '1 / span 2',
    disabled: !form._isNew,
  },
  {
    key: 'bookName',
    label: '名称',
    type: 'text',
    gridColumn: '3 / span 2',
  },
  {
    key: 'sourceType',
    label: 'Source Type',
    type: 'select',
    options: [{ title: 'SNAPSHOT', value: 'SNAPSHOT' }],
    gridColumn: '1 / span 2',
  },
  {
    key: 'dataSourceCode',
    label: 'データソース',
    type: 'select',
    options: catalogsQuery.catalogs.value.map(catalog => ({
      title: `${catalog.displayName} (${catalog.sourceCode})`,
      value: catalog.sourceCode,
    })),
    gridColumn: '3 / span 2',
  },
  {
    key: 'layoutType',
    label: 'レイアウト方式',
    type: 'select',
    options: [
      { title: '明細行の繰り返し', value: 'REPEATING_ROW' },
      { title: '月間集計表（固定配置）', value: 'MONTHLY_SUMMARY' },
      { title: '帳票専用Renderer', value: 'DEDICATED' },
    ],
    gridColumn: '1 / span 2',
  },
  {
    key: 'rendererKey',
    label: 'Renderer Key',
    type: 'text',
    gridColumn: '3 / span 2',
  },
  {
    key: 'templateSheetName',
    label: 'テンプレートシート名',
    type: 'text',
    gridColumn: '1 / span 2',
  },
  {
    key: 'activeFlag',
    label: '有効',
    type: 'checkbox',
    gridColumn: '3 / span 1',
  },
])

const availableColumns = computed(() =>
  selectedCatalog.value?.columns ?? [],
)

const columnItems = computed(() =>
  availableColumns.value.map(column => ({
    title: `${column.displayName} (${column.columnName})`,
    value: column.columnName,
  })),
)

function addVariableMapping() {
  const column = availableColumns.value[0]
  const nextOrder = form.variableMappings.length + 1
  form.variableMappings.push({
    id: null,
    variableKey: `rows.value${nextOrder}`,
    sourceColumn: column?.columnName ?? '',
    scope: 'ROW',
    dataType: column?.dataType ?? 'STRING',
    orderNo: nextOrder,
  })
}

function removeVariableMapping(index: number) {
  form.variableMappings.splice(index, 1)
  form.variableMappings.forEach((mapping, mappingIndex) => {
    mapping.orderNo = mappingIndex + 1
  })
}

function updateMappingColumn(
  mapping: ExcelBookVariableMapping,
  columnName: string,
) {
  const column = availableColumns.value.find(
    item => item.columnName === columnName,
  )
  if (column) {
    mapping.dataType = column.dataType
  }
}

const title = computed(() =>
  form._isNew ? '台帳マスタ 新規作成' : `台帳マスタ：${form.bookName}`,
)

const leftFooterItems = computed<ToolbarItem[]>(() => [
  {
    type: 'button',
    label: 'Spreadsheetテンプレート編集',
    color: 'primary',
    disabled: form._isNew || props.saving,
    onClick: () => emit('edit-template', { ...form }),
  },
  {
    type: 'button',
    label: '削除',
    color: 'error',
    disabled: form._isNew || props.saving,
    onClick: () => emit('delete', { ...form }),
  },
])

const rightFooterItems = computed<ToolbarItem[]>(() => [
  {
    type: 'button',
    label: 'キャンセル',
    color: 'secondary',
    disabled: props.saving,
    onClick: () => {
      visible.value = false
    },
  },
  {
    type: 'button',
    label: '保存',
    color: 'primary',
    disabled: props.saving,
    onClick: () => emit('save', { ...form }),
  },
])

watch(
  () => props.item,
  value => {
    const next = value ?? createEmptyExcelBookForm()
    Object.assign(form, {
      ...next,
      selection: {
        ...next.selection,
        displayColumns: [...next.selection.displayColumns],
      },
      print: { ...next.print },
      variableMappings: next.variableMappings.map(mapping => ({
        ...mapping,
      })),
    })
  },
  { immediate: true },
)
</script>

<template>
  <DetailDialogLayout
    v-model="visible"
    :title="title"
    max-width="1100"
    :left-footer-items="leftFooterItems"
    :right-footer-items="rightFooterItems"
  >
    <FormLayout v-model="form" :schema="schema">
      <GridBasedForm
        v-model="form"
        :fields="fields"
      />

      <v-divider class="my-5" />

      <div class="text-subtitle-1 font-weight-bold mb-3">
        生成対象の選択
      </div>
      <div class="selection-grid">
        <v-select
          v-model="form.selection.mode"
          :items="[
            { title: '選択なし', value: 'NONE' },
            { title: '単一選択', value: 'SINGLE' },
            { title: '複数選択', value: 'MULTIPLE' },
          ]"
          label="選択方式"
          variant="outlined"
          density="compact"
        />
        <v-select
          v-model="form.selection.generationUnit"
          :items="[
            { title: '1ファイルへ集約', value: 'ONE_FILE' },
            { title: '対象ごとに1ファイル', value: 'FILE_PER_SELECTION' },
          ]"
          label="生成単位"
          variant="outlined"
          density="compact"
          :disabled="form.selection.mode === 'NONE'"
        />
        <v-select
          v-model="form.selection.dataSourceCode"
          :items="catalogsQuery.catalogs.value.map(catalog => ({
            title: `${catalog.displayName} (${catalog.sourceCode})`,
            value: catalog.sourceCode,
          }))"
          label="選択一覧データソース"
          variant="outlined"
          density="compact"
          clearable
          :disabled="form.selection.mode === 'NONE'"
        />
        <v-select
          v-model="form.selection.valueColumn"
          :items="selectionColumnItems"
          label="選択値の項目"
          variant="outlined"
          density="compact"
          clearable
          :disabled="form.selection.mode === 'NONE' || !selectionCatalog"
        />
        <v-select
          v-model="form.selection.displayColumns"
          :items="selectionColumnItems"
          label="一覧の表示項目"
          variant="outlined"
          density="compact"
          multiple
          chips
          closable-chips
          :disabled="form.selection.mode === 'NONE' || !selectionCatalog"
        />
        <v-checkbox
          v-model="form.selection.allowSelectAll"
          label="全件選択を許可"
          density="compact"
          :disabled="form.selection.mode === 'NONE'"
        />
      </div>

      <div class="text-subtitle-1 font-weight-bold mb-3 mt-2">
        印刷設定
      </div>
      <div class="print-grid">
        <v-select
          v-model="form.print.paperSize"
          :items="['A3', 'A4', 'B5']"
          label="用紙サイズ"
          variant="outlined"
          density="compact"
        />
        <v-select
          v-model="form.print.orientation"
          :items="[
            { title: '縦', value: 'PORTRAIT' },
            { title: '横', value: 'LANDSCAPE' },
          ]"
          label="用紙方向"
          variant="outlined"
          density="compact"
        />
        <v-checkbox
          v-model="form.print.fitToOnePage"
          label="1ページに収める"
          density="compact"
        />
      </div>

      <v-alert
        v-if="form.layoutType !== 'REPEATING_ROW'"
        type="info"
        variant="tonal"
        density="compact"
        class="mt-5"
      >
        固定配置台帳は専用ViewとRendererで配置します。
        テンプレート変数の登録は不要です。
      </v-alert>

      <v-divider v-if="form.layoutType === 'REPEATING_ROW'" class="my-5" />

      <div v-if="form.layoutType === 'REPEATING_ROW'" class="mapping-header">
        <div>
          <div class="text-subtitle-1 font-weight-bold">
            テンプレート変数
          </div>
          <div class="text-body-2 text-medium-emphasis">
            Spreadsheet内の ${name} と許可済みデータ項目を対応付けます。
          </div>
        </div>
        <v-btn
          color="primary"
          variant="tonal"
          prepend-icon="mdi-plus"
          :disabled="!selectedCatalog"
          @click="addVariableMapping"
        >
          変数を追加
        </v-btn>
      </div>

      <v-alert
        v-if="catalogsQuery.isError.value"
        type="error"
        variant="tonal"
        density="compact"
        class="mt-3"
      >
        データソースカタログの取得に失敗しました。
      </v-alert>

      <div v-if="form.layoutType === 'REPEATING_ROW'" class="mapping-list">
        <div
          v-for="(mapping, index) in form.variableMappings"
          :key="mapping.id ?? `new-${index}`"
          class="mapping-row"
        >
          <v-text-field
            v-model="mapping.variableKey"
            label="変数キー"
            placeholder="rows.employeeName"
            density="comfortable"
            variant="outlined"
            hide-details
          />
          <v-select
            v-model="mapping.sourceColumn"
            label="参照項目"
            :items="columnItems"
            density="comfortable"
            variant="outlined"
            hide-details
            @update:model-value="
              value => updateMappingColumn(mapping, String(value))
            "
          />
          <v-select
            v-model="mapping.scope"
            label="単位"
            :items="['CONTEXT', 'ROW']"
            density="comfortable"
            variant="outlined"
            hide-details
          />
          <v-text-field
            v-model="mapping.dataType"
            label="型"
            density="comfortable"
            variant="outlined"
            disabled
            hide-details
          />
          <v-btn
            icon="mdi-delete-outline"
            color="error"
            variant="text"
            aria-label="変数を削除"
            @click="removeVariableMapping(index)"
          />
        </div>
        <v-alert
          v-if="form.variableMappings.length === 0"
          type="info"
          variant="tonal"
          density="compact"
        >
          データソースを選択し、テンプレートで使う変数を追加してください。
        </v-alert>
      </div>
    </FormLayout>
  </DetailDialogLayout>
</template>

<style scoped>
.mapping-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.selection-grid,
.print-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(240px, 1fr));
  gap: 4px 16px;
}

.print-grid {
  grid-template-columns: repeat(3, minmax(180px, 1fr));
}

.mapping-list {
  display: grid;
  gap: 10px;
  margin-top: 14px;
}

.mapping-row {
  display: grid;
  grid-template-columns:
    minmax(180px, 1.1fr)
    minmax(220px, 1.4fr)
    130px
    120px
    48px;
  gap: 10px;
  align-items: center;
}

@media (max-width: 900px) {
  .mapping-row {
    grid-template-columns: 1fr;
  }

  .selection-grid,
  .print-grid {
    grid-template-columns: 1fr;
  }
}
</style>
