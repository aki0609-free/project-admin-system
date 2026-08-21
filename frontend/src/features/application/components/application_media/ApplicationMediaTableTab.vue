<script setup lang="ts">
import { computed } from 'vue'
import MultiLevelHeaderTable from '@/shared/components/table/multi_level_header_table/MultiLevelHeaderTable.vue'
import AppDialog from '@/shared/ui/dialog/AppDialog.vue'
import AppToolbar from '@/shared/ui/toolbar/AppToolbar.vue'
import type { ToolbarItem } from '@/shared/ui/toolbar/types'
import type { ApplicationMediaPivotRow, FilterState } from '@/features/application/types/applicationMediaTypes'
import { useApplicationMediaTable } from '@/features/application/composables/application_media/useApplicationMediaTable'
import { useApplicationMediaTableDialog } from '@/features/application/composables/application_media/useApplicationMediaDialog'
import { useApplicationMediaTableToolbar } from '@/features/application/composables/application_media/useApplicationMediaTableToolbar'
import { yearOptions, monthOptions } from '@/shared/utils/DateUtils'

const TABLE_KEY = 'application-media-table'

const props = defineProps<{
  yearMonths: string[]
  allYearMonthOptions: { label: string; value: string }[]
  mediaNames: string[]
  filter: FilterState
  pivotTableData: ApplicationMediaPivotRow[]
  isDirty: boolean
  saving?: boolean
  onSave: () => void | Promise<void>
}>()

const emit = defineEmits<{
  (e: 'update-cell', payload: {
    id: number
    field: keyof ApplicationMediaPivotRow
    value: unknown
  }): void
  (
    e: 'add-media' | 'add-year-month' | 'delete-media' | 'delete-year-month',
    value: string,
  ): void
}>()

const dialogState = useApplicationMediaTableDialog()

const table = useApplicationMediaTable(
  computed(() => props.yearMonths),
)

const toolbar = useApplicationMediaTableToolbar({
  allYearMonthOptions: computed(() => props.allYearMonthOptions),
  filter: props.filter,
  openMediaDialog: dialogState.openMediaDialog,
  openYearMonthDialog: dialogState.openYearMonthDialog,
  openDeleteMediaDialog: dialogState.openDeleteMediaDialog,
  openDeleteYearMonthDialog: dialogState.openDeleteYearMonthDialog,
  isDirty: computed(() => props.isDirty),
  saving: computed(() => props.saving ?? false),
  onSave: props.onSave,
})

const canAddMedia = computed(() => dialogState.newMediaName.value.trim().length > 0)
const canAddYearMonth = computed(() => !!dialogState.newYear.value && !!dialogState.newMonth.value)
const canDeleteMedia = computed(() => !!dialogState.deleteMediaName.value)
const canDeleteYearMonth = computed(() => !!dialogState.deleteYearMonthValue.value)

const createMediaFooterItems = computed<ToolbarItem[]>(() => [
  {
    id: 'cancel-media',
    type: 'button',
    label: 'キャンセル',
    intent: 'secondary',
    onClick: dialogState.closeMediaDialog,
  },
  {
    id: 'add-media',
    type: 'button',
    label: '追加',
    intent: 'primary',
    disabled: !canAddMedia.value,
    onClick: addMedia,
  },
])

const createYearMonthFooterItems = computed<ToolbarItem[]>(() => [
  {
    id: 'cancel-year-month',
    type: 'button',
    label: 'キャンセル',
    intent: 'secondary',
    onClick: dialogState.closeYearMonthDialog,
  },
  {
    id: 'add-year-month',
    type: 'button',
    label: '追加',
    intent: 'primary',
    disabled: !canAddYearMonth.value,
    onClick: addYearMonth,
  },
])

const deleteMediaFooterItems = computed<ToolbarItem[]>(() => [
  {
    id: 'cancel-delete-media',
    type: 'button',
    label: 'キャンセル',
    intent: 'secondary',
    onClick: dialogState.closeDeleteMediaDialog,
  },
  {
    id: 'delete-media',
    type: 'button',
    label: '削除',
    intent: 'danger',
    disabled: !canDeleteMedia.value,
    onClick: deleteMedia,
  },
])

const deleteYearMonthFooterItems = computed<ToolbarItem[]>(() => [
  {
    id: 'cancel-delete-year-month',
    type: 'button',
    label: 'キャンセル',
    intent: 'secondary',
    onClick: dialogState.closeDeleteYearMonthDialog,
  },
  {
    id: 'delete-year-month',
    type: 'button',
    label: '削除',
    intent: 'danger',
    disabled: !canDeleteYearMonth.value,
    onClick: deleteYearMonth,
  },
])

function addMedia() {
  const name = dialogState.newMediaName.value.trim()
  if (!name) return

  emit('add-media', name)
  dialogState.newMediaName.value = ''
  dialogState.closeMediaDialog()
}

function addYearMonth() {
  if (!dialogState.newYear.value || !dialogState.newMonth.value) return

  const year = String(dialogState.newYear.value)
  const month = String(dialogState.newMonth.value).padStart(2, '0')
  const value = `${year}-${month}`

  emit('add-year-month', value)

  dialogState.newYear.value = ''
  dialogState.newMonth.value = ''
  dialogState.closeYearMonthDialog()
}

function deleteMedia() {
  if (!dialogState.deleteMediaName.value) return
  emit('delete-media', dialogState.deleteMediaName.value)
  dialogState.deleteMediaName.value = ''
  dialogState.closeDeleteMediaDialog()
}

function deleteYearMonth() {
  if (!dialogState.deleteYearMonthValue.value) return
  emit('delete-year-month', dialogState.deleteYearMonthValue.value)
  dialogState.deleteYearMonthValue.value = ''
  dialogState.closeDeleteYearMonthDialog()
}
</script>

<template>
  <div class="d-flex flex-column ga-4">
    <AppToolbar
      :left-items="toolbar.leftToolbarItems.value"
      :right-items="toolbar.rightToolbarItems.value"
      surface="page"
    />

    <MultiLevelHeaderTable
      :table-key="TABLE_KEY"
      :data="pivotTableData"
      :def="table.pivotTableDef.value"
      @update-cell="emit('update-cell', $event)"
    />

    <AppDialog
      v-model="dialogState.createMediaDialog.value"
      title="媒体追加"
      size="sm"
      :right-footer-items="createMediaFooterItems"
    >
      <v-text-field
        v-model="dialogState.newMediaName.value"
        label="応募媒体名"
        placeholder="例：Indeed"
        density="comfortable"
        hide-details
        clearable
        @keydown.enter="addMedia"
      />
    </AppDialog>

    <AppDialog
      v-model="dialogState.createYearMonthDialog.value"
      title="年月追加"
      size="sm"
      :right-footer-items="createYearMonthFooterItems"
    >
      <div class="d-flex ga-2">
        <v-select
          v-model="dialogState.newYear.value"
          :items="yearOptions()"
          item-title="label"
          item-value="value"
          label="年"
          density="comfortable"
          hide-details
        />
        <v-select
          v-model="dialogState.newMonth.value"
          :items="monthOptions"
          item-title="label"
          item-value="value"
          label="月"
          density="comfortable"
          hide-details
        />
      </div>
    </AppDialog>

    <AppDialog
      v-model="dialogState.deleteMediaDialog.value"
      title="メディア削除"
      size="sm"
      :right-footer-items="deleteMediaFooterItems"
    >
      <v-select
        v-model="dialogState.deleteMediaName.value"
        :items="mediaNames"
        label="削除するメディア"
        density="comfortable"
        hide-details
      />
    </AppDialog>

    <AppDialog
      v-model="dialogState.deleteYearMonthDialog.value"
      title="年月削除"
      size="sm"
      :right-footer-items="deleteYearMonthFooterItems"
    >
      <v-select
        v-model="dialogState.deleteYearMonthValue.value"
        :items="allYearMonthOptions"
        item-title="label"
        item-value="value"
        label="削除する年月"
        density="comfortable"
        hide-details
      />
    </AppDialog>
  </div>
</template>
