<script setup lang="ts">
import { toRef } from 'vue'
import SimpleTable from '@/shared/components/table/simple_table/SimpleTable.vue'
import type { BackupTargetResponse } from '@/features/system/backup/types/backupApiTypes'
import {
  type BackupTargetTableRow,
  useBackupTargetTableConfig,
} from '@/features/system/backup/composables/useBackupTargetTableConfig'

const props = defineProps<{
  items: BackupTargetResponse[]
  selectedCodes: string[]
}>()

const emit = defineEmits<{
  (e: 'row-click', row: BackupTargetResponse): void
  (e: 'update:selectedCodes', value: string[]): void
}>()

const { rows, columns, filterRules } = useBackupTargetTableConfig(
  toRef(props, 'items'),
  toRef(props, 'selectedCodes'),
)

const onCellUpdate = (payload: { id: number; field: keyof BackupTargetTableRow; value: unknown }) => {
  if (payload.field !== 'selected') return

  const row = rows.value.find((item) => item.id === payload.id)
  if (!row) return

  const checked = payload.value === true

  if (checked) {
    if (!props.selectedCodes.includes(row.targetCode)) {
      emit('update:selectedCodes', [...props.selectedCodes, row.targetCode])
    }
    return
  }

  emit(
    'update:selectedCodes',
    props.selectedCodes.filter((code) => code !== row.targetCode),
  )
}

const onRowClick = (row: BackupTargetTableRow) => {
  emit('row-click', row.raw)
}
</script>

<template>
  <SimpleTable
    table-key="backup-target-list"
    item-key="id"
    :items="rows"
    :columns="columns"
    :filter-rules="filterRules"
    enable-row-click
    @row-click="onRowClick"
    @update:items="onCellUpdate"
  />
</template>