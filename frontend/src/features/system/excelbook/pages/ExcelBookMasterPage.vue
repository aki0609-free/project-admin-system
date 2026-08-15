<script setup lang="ts">
import { computed, defineAsyncComponent, ref } from 'vue'
import ListDetailPageLayout from '@/shared/templates/list-detail/ListDetailPageTemplate.vue'
import SimpleTable from '@/shared/components/table/simple_table/SimpleTable.vue'
import { createSimpleTableFilterRules } from '@/shared/components/table/simple_table/utils/createSimpleTableFilterRules'
import type { ToolbarItem } from '@/shared/components/toolbar/types/types'
import ExcelBookEditDialog from '../components/ExcelBookEditDialog.vue'
import { useExcelBookMastersQuery } from '../api/useExcelBookMastersQuery'
import { useCreateExcelBookMasterMutation } from '../api/useCreateExcelBookMasterMutation'
import { useUpdateExcelBookMasterMutation } from '../api/useUpdateExcelBookMasterMutation'
import { useDeleteExcelBookMasterMutation } from '../api/useDeleteExcelBookMasterMutation'
import { useExcelBookMasterColumns } from '../composables/useExcelBookMasterColumns'
import type {
  ExcelBookMasterForm,
  ExcelBookMasterResponse,
} from '../types/excelBookTypes'
import {
  createEmptyExcelBookForm,
  toExcelBookForm,
  toExcelBookRequest,
} from '../utils/excelBookFactory'

const SpreadsheetTemplateEditorDialog = defineAsyncComponent(
  () => import('../components/SpreadsheetTemplateEditorDialog.vue'),
)

const dialog = ref(false)
const spreadsheetDialog = ref(false)
const selectedItem = ref<ExcelBookMasterForm | null>(null)
const spreadsheetMaster = ref<ExcelBookMasterResponse | null>(null)

const excelBookMastersQuery = useExcelBookMastersQuery()
const createMutation = useCreateExcelBookMasterMutation()
const updateMutation = useUpdateExcelBookMasterMutation()
const deleteMutation = useDeleteExcelBookMasterMutation()

const { columns } = useExcelBookMasterColumns()

const items = computed(() => excelBookMastersQuery.excelBookMasters.value)

const loading = computed(() => excelBookMastersQuery.isLoading.value)
const saving = computed(
  () =>
    createMutation.isPending.value ||
    updateMutation.isPending.value ||
    deleteMutation.isPending.value,
)


const filterRules = computed(() =>
  createSimpleTableFilterRules<ExcelBookMasterResponse>(columns.value),
)

const leftToolbarItems = computed<ToolbarItem[]>(() => [
  {
    type: 'button',
    label: '新規',
    color: 'primary',
    onClick: openCreate,
  },
  {
    type: 'button',
    label: '再読込',
    color: 'secondary',
    disabled: loading.value,
    onClick: () => excelBookMastersQuery.refetch(),
  },
])

const rightToolbarItems = computed<ToolbarItem[]>(() => [])

function openCreate() {
  selectedItem.value = createEmptyExcelBookForm()
  dialog.value = true
}

function handleRowClick(row: ExcelBookMasterResponse) {
  selectedItem.value = toExcelBookForm(row)
  dialog.value = true
}

function handleEditSpreadsheetTemplate(form: ExcelBookMasterForm) {
  if (form._isNew) return

  spreadsheetMaster.value = form
  dialog.value = false
  spreadsheetDialog.value = true
}

async function handleSave(form: ExcelBookMasterForm) {
  const body = toExcelBookRequest(form)

  if (form._isNew) {
    await createMutation.mutateAsync(body)
  } else {
    await updateMutation.mutateAsync({
      id: form.id,
      request: body,
    })
  }

  dialog.value = false
  selectedItem.value = null
}

async function handleDelete(form: ExcelBookMasterForm) {
  if (form._isNew) return

  const ok = window.confirm(`「${form.bookName}」を削除しますか？`)
  if (!ok) return

  await deleteMutation.mutateAsync(form.id)

  dialog.value = false
  selectedItem.value = null
}

</script>

<template>
  <ListDetailPageLayout
    title="台帳マスタ"
    description="締め処理で使用するデータソースとSpreadsheetテンプレートを管理します。"
    :left-toolbar-items="leftToolbarItems"
    :right-toolbar-items="rightToolbarItems"
  >
    <template #search>
      <v-alert type="info" variant="tonal" density="compact">
        行を選択すると、マスター設定とSpreadsheetテンプレートを編集できます。
        台帳の生成は「締め処理 → 台帳」から実行します。
      </v-alert>
    </template>

    <v-alert
      v-if="excelBookMastersQuery.isError.value"
      type="error"
      variant="tonal"
    >
      台帳マスタの取得に失敗しました。
    </v-alert>

    <v-alert
      v-if="loading"
      type="info"
      variant="tonal"
    >
      読み込み中です。
    </v-alert>

    <SimpleTable
      table-key="system-excel-book-master"
      item-key="id"
      :items="items"
      :columns="columns"
      :filter-rules="filterRules"
      :enable-row-click="true"
      @row-click="handleRowClick"
    />

    <template #dialogs>
      <ExcelBookEditDialog
        v-model="dialog"
        :item="selectedItem"
        :saving="saving"
        @save="handleSave"
        @delete="handleDelete"
        @edit-template="handleEditSpreadsheetTemplate"
      />
      <SpreadsheetTemplateEditorDialog
        v-model="spreadsheetDialog"
        :master="spreadsheetMaster"
      />
    </template>
  </ListDetailPageLayout>
</template>
