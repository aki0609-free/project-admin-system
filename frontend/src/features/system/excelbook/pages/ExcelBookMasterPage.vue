<script setup lang="ts">
import { computed, ref } from 'vue'
import ListDetailPageLayout from '@/toolbox/pages/ListDetailPageLayout.vue'
import SimpleTable from '@/shared/components/table/simple_table/SimpleTable.vue'
import { createSimpleTableFilterRules } from '@/shared/components/table/simple_table/utils/createSimpleTableFilterRules'
import type { ToolbarItem } from '@/shared/components/toolbar/types/types'
import ExcelBookEditDialog from '../components/ExcelBookEditDialog.vue'
import { useExcelBookMastersQuery } from '../api/useExcelBookMastersQuery'
import { useCreateExcelBookMasterMutation } from '../api/useCreateExcelBookMasterMutation'
import { useUpdateExcelBookMasterMutation } from '../api/useUpdateExcelBookMasterMutation'
import { useDeleteExcelBookMasterMutation } from '../api/useDeleteExcelBookMasterMutation'
import { useUpdateExcelBookMutation } from '../api/useUpdateExcelBookMutation'
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
import { downloadBlob } from '@/shared/utils/BusinessUtils.js'

const dialog = ref(false)
const selectedItem = ref<ExcelBookMasterForm | null>(null)
const targetMonth = ref(new Date().toISOString().slice(0, 7))

const excelBookMastersQuery = useExcelBookMastersQuery()
const createMutation = useCreateExcelBookMasterMutation()
const updateMutation = useUpdateExcelBookMasterMutation()
const deleteMutation = useDeleteExcelBookMasterMutation()
const updateBookMutation = useUpdateExcelBookMutation()

const { columns } = useExcelBookMasterColumns()

const items = computed(() => excelBookMastersQuery.excelBookMasters.value)

const loading = computed(() => excelBookMastersQuery.isLoading.value)
const saving = computed(
  () =>
    createMutation.isPending.value ||
    updateMutation.isPending.value ||
    deleteMutation.isPending.value,
)

const updatingBook = computed(() => updateBookMutation.isPending.value)

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

const rightToolbarItems = computed<ToolbarItem[]>(() => [
  {
    type: 'button',
    label: '選択台帳を更新',
    color: 'primary',
    disabled:
      !selectedItem.value ||
      selectedItem.value._isNew ||
      updatingBook.value,
    onClick: handleUpdateBook,
  },
])

function openCreate() {
  selectedItem.value = createEmptyExcelBookForm()
  dialog.value = true
}

function handleRowClick(row: ExcelBookMasterResponse) {
  selectedItem.value = toExcelBookForm(row)
  dialog.value = true
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

async function handleUpdateBook() {
  if (!selectedItem.value || selectedItem.value._isNew) return

  const ok = window.confirm(
    `「${selectedItem.value.bookName}」の ${targetMonth.value} シートを更新しますか？`,
  )
  if (!ok) return

  const blob = await updateBookMutation.mutateAsync({
    bookCode: selectedItem.value.bookCode,
    request: {
      targetMonth: targetMonth.value,
    },
  }) as Blob

  downloadBlob(blob, `${selectedItem.value.bookCode}_${targetMonth.value}.xlsx`)
}
</script>

<template>
  <ListDetailPageLayout
    title="Excel台帳マスタ"
    description="テンプレートExcelとSnapshotを紐づけて、既存Excel台帳を更新します。"
    :left-toolbar-items="leftToolbarItems"
    :right-toolbar-items="rightToolbarItems"
  >
    <template #search>
      <v-card variant="tonal" class="pa-3">
        <div class="d-flex align-center ga-3">
          <v-text-field
            v-model="targetMonth"
            label="更新対象月"
            type="month"
            density="compact"
            hide-details
            style="max-width: 180px"
          />

          <div class="text-caption text-medium-emphasis">
            行を選択してから「選択台帳を更新」を実行してください。
          </div>
        </div>
      </v-card>
    </template>

    <v-alert
      v-if="excelBookMastersQuery.isError.value"
      type="error"
      variant="tonal"
    >
      Excel台帳マスタの取得に失敗しました。
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
      />
    </template>
  </ListDetailPageLayout>
</template>