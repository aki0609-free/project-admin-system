<script setup lang="ts">
import { computed, ref } from 'vue'
import SimpleTable from '@/shared/components/table/simple_table/SimpleTable.vue'
import AppToolbar from '@/shared/ui/toolbar/AppToolbar.vue'
import type { ToolbarItem } from '@/shared/ui/toolbar/types'
import { useReportMastersQuery } from '@/features/system/report/api/queries/useReportMastersQuery'
import ReportMasterEditDialog from '@/features/system/report/components/ReportMasterEditDialog.vue'
import {
  type ReportMasterTableRow,
  useReportMasterTableConfig,
} from '@/features/system/report/composables/useReportMasterTableConfig'

const reportMastersQuery = useReportMastersQuery()

const dialogVisible = ref(false)
const editingId = ref<number | null>(null)

const { rows, columns, filterRules } = useReportMasterTableConfig(
  reportMastersQuery.reportMasters,
)

const openCreate = () => {
  editingId.value = null
  dialogVisible.value = true
}

const openEdit = (row: ReportMasterTableRow) => {
  editingId.value = row.id
  dialogVisible.value = true
}

const leftToolbarItems = computed<ToolbarItem[]>(() => [
  {
    type: 'button',
    label: '新規作成',
    intent: 'primary',
    onClick: openCreate,
  },
])
</script>

<template>
  <div class="tab-page">
    <AppToolbar :left-items="leftToolbarItems" />

    <SimpleTable
      table-key="report-master-admin"
      item-key="id"
      :items="rows"
      :columns="columns"
      :filter-rules="filterRules"
      enable-row-click
      @row-click="openEdit"
    />

    <ReportMasterEditDialog
      :key="editingId ?? 'new'"
      v-model="dialogVisible"
      :report-master-id="editingId"
    />
  </div>
</template>

<style scoped>
.tab-page {
  display: grid;
  gap: 12px;
}
</style>
