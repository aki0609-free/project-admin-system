<script setup lang="ts">
import { computed, ref } from 'vue'
import SimpleTable from '@/shared/components/table/simple_table/SimpleTable.vue'
import GenericToolbar from '@/shared/components/toolbar/GenericToolbar.vue'
import type { ToolbarItem } from '@/shared/components/toolbar/types/types'
import { useReportSignaturesQuery } from '@/features/system/report/api/queries/useReportSignaturesQuery'
import ReportSignatureEditDialog from '@/features/system/report/components/ReportSignatureEditDialog.vue'
import {
  type ReportSignatureTableRow,
  useReportSignatureTableConfig,
} from '@/features/system/report/composables/useReportSignatureTableConfig'

const { reportSignatures } = useReportSignaturesQuery()

const dialogVisible = ref(false)
const editingId = ref<number | null>(null)

const { rows, columns, filterRules } =
  useReportSignatureTableConfig(reportSignatures)

const openCreate = () => {
  editingId.value = null
  dialogVisible.value = true
}

const openEdit = (row: ReportSignatureTableRow) => {
  editingId.value = row.id
  dialogVisible.value = true
}

const toolbarItems = computed<ToolbarItem[]>(() => [
  {
    type: 'button',
    label: '新規作成',
    color: 'primary',
    onClick: openCreate,
  },
])
</script>

<template>
  <div class="tab-page">
    <GenericToolbar :items="toolbarItems" />

    <SimpleTable
      table-key="report-signature-admin"
      item-key="id"
      :items="rows"
      :columns="columns"
      :filter-rules="filterRules"
      enable-row-click
      @row-click="openEdit"
    >
      <template #[`item.width`]="{ item }">
        {{ item.width ?? '-' }} × {{ item.height ?? '-' }}
      </template>

      <template #[`item.defaultFlag`]="{ item }">
        <v-chip
          v-if="item.defaultFlag"
          size="small"
          color="primary"
          variant="tonal"
        >
          既定
        </v-chip>

        <span
          v-else
          class="empty-text"
        >
          -
        </span>
      </template>

      <template #[`item.activeFlag`]="{ item }">
        <v-chip
          size="small"
          :color="item.activeFlag ? 'success' : 'grey'"
          variant="tonal"
        >
          {{ item.activeFlag ? '有効' : '無効' }}
        </v-chip>
      </template>
    </SimpleTable>

    <ReportSignatureEditDialog
      v-model="dialogVisible"
      :report-signature-id="editingId"
    />
  </div>
</template>

<style scoped>
.tab-page {
  display: grid;
  gap: 12px;
}

.empty-text {
  color: #94a3b8;
}
</style>