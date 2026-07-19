<script setup lang="ts">
import SimpleTable from '@/shared/components/table/simple_table/SimpleTable.vue'
import { useReportHistoriesQuery } from '@/features/system/report/api/queries/useReportHistoriesQuery'
import { useDownloadReportHistoryMutation } from '@/features/system/report/api/mutations/useDownloadReportHistoryMutation'
import {
  type ReportHistoryTableRow,
  useReportHistoryTableConfig,
} from '@/features/system/report/composables/useReportHistoryTableConfig'

const { reportHistories } = useReportHistoriesQuery()
const downloadMutation = useDownloadReportHistoryMutation()

const { rows, columns, filterRules } =
  useReportHistoryTableConfig(reportHistories)

const download = async (
  row: ReportHistoryTableRow,
) => {
  if (!row.downloadable) {
    alert('ダウンロード対象のファイルがありません。')
    return
  }

  try {
    await downloadMutation.mutateAsync(row.id)
  } catch (e) {
    const message =
      e instanceof Error
        ? e.message
        : 'ファイル取得に失敗しました。'

    alert(message)
  }
}
</script>

<template>
  <div class="tab-page">
    <SimpleTable
      table-key="report-history-admin"
      item-key="id"
      :items="rows"
      :columns="columns"
      :filter-rules="filterRules"
    >
      <template #[`item.fileName`]="{ item }">
        <v-btn
          v-if="item.downloadable"
          variant="text"
          color="primary"
          size="small"
          class="download-btn"
          :loading="downloadMutation.isPending.value"
          @click.stop="download(item)"
        >
          {{ item.fileName }}
        </v-btn>

        <span
          v-else
          class="empty-text"
        >
          {{ item.fileName || '-' }}
        </span>
      </template>

      <template #[`item.status`]="{ item }">
        <v-chip
          size="small"
          :color="item.statusColor"
          variant="tonal"
        >
          {{ item.status }}
        </v-chip>
      </template>

      <template #[`item.storageType`]="{ item }">
        <v-chip
          v-if="item.storageType"
          size="small"
          variant="outlined"
        >
          {{ item.storageType }}
        </v-chip>

        <span
          v-else
          class="empty-text"
        >
          -
        </span>
      </template>
    </SimpleTable>
  </div>
</template>

<style scoped>
.tab-page {
  display: grid;
  gap: 12px;
}

.download-btn {
  justify-content: flex-start;
  padding-inline: 0;
  text-transform: none;
}

.empty-text {
  color: #94a3b8;
}
</style>