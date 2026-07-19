<script setup lang="ts">
import { computed } from 'vue'
import SimpleTable from '@/shared/components/table/simple_table/SimpleTable.vue'
import type {
  SimpleTableColumnDef,
  SimpleTableEditableRow,
} from '@/shared/components/table/simple_table/types/item/types'
import { createSimpleTableFilterRules } from '@/shared/components/table/simple_table/utils/createSimpleTableFilterRules'
import type { BackupHistoryResponse } from '@/features/system/backup/types/backupApiTypes'
import { useDownloadBackupHistoryFileMutation } from '@/features/system/backup/api/useDownloadBackupHistoryFileMutation'
import { downloadBlob } from '@/features/system/backup/utils/downloadBlob'

type Row = SimpleTableEditableRow & {
  id: number
  targetCodes: string
  fileName: string
  status: string
  statusColor: string
  zipOutputText: string
  fileSizeText: string
  storageType: string
  storedFileKey: string
  storedFileName: string
  executedBy: string
  executedAt: string
  errorMessage: string
  downloadable: boolean
  raw: BackupHistoryResponse
}

const props = defineProps<{
  items: BackupHistoryResponse[]
}>()

const downloadMutation = useDownloadBackupHistoryFileMutation()

const resolveStatusColor = (status: string): string => {
  if (status === 'SUCCESS') return 'success'
  if (status === 'FAILED') return 'error'
  return 'default'
}

const formatFileSize = (size: number | null): string => {
  if (size == null) return ''
  if (size < 1024) return `${size} B`
  if (size < 1024 * 1024) return `${Math.round(size / 1024)} KB`
  return `${Math.round(size / 1024 / 1024)} MB`
}

const resolveDownloadFileName = (row: Row): string => {
  if (row.storedFileName) return row.storedFileName
  if (row.fileName) return row.fileName
  return row.zipOutputText === 'ZIP'
    ? `backup_${row.id}.zip`
    : `backup_${row.id}.csv`
}

const rows = computed<Row[]>(() =>
  props.items.map(item => ({
    id: item.id,
    targetCodes: item.targetCodes,
    fileName: item.fileName ?? '',
    status: item.status,
    statusColor: resolveStatusColor(item.status),
    zipOutputText: item.zipOutput ? 'ZIP' : 'CSV',
    fileSizeText: formatFileSize(item.fileSize),
    storageType: item.storageType ?? '',
    storedFileKey: item.storedFileKey ?? '',
    storedFileName: item.storedFileName ?? '',
    executedBy: item.executedBy ?? '',
    executedAt: item.executedAt,
    errorMessage: item.errorMessage ?? '',
    downloadable: item.status === 'SUCCESS' && !!item.storedFileKey,
    raw: item,
  })),
)

const columns = computed(() => {
  const defs: SimpleTableColumnDef<Row>[] = [
    { title: '操作', key: 'downloadable', width: '200px', filter: { type: 'text' } },
    { title: 'ID', key: 'id', width: '200px', filter: { type: 'text' } },
    { title: '対象', key: 'targetCodes', width: '360px', filter: { type: 'text' } },
    { title: 'ファイル名', key: 'fileName', width: '400px', filter: { type: 'text' } },
    { title: '状態', key: 'status', width: '200px', filter: { type: 'text' } },
    { title: '形式', key: 'zipOutputText', width: '200px', filter: { type: 'text' } },
    { title: 'サイズ', key: 'fileSizeText', width: '200px', filter: { type: 'text' } },
    { title: '保存先', key: 'storageType', width: '200px', filter: { type: 'text' } },
    { title: '保存名', key: 'storedFileName', width: '300px', filter: { type: 'text' } },
    { title: '保存キー', key: 'storedFileKey', width: '400px', filter: { type: 'text' } },
    { title: '実行者', key: 'executedBy', width: '200px', filter: { type: 'text' } },
    { title: '実行日時', key: 'executedAt', width: '300px', filter: { type: 'text' } },
    { title: 'エラー', key: 'errorMessage', width: '300px', filter: { type: 'text' } },
  ]

  return defs
})

const filterRules = computed(() =>
  createSimpleTableFilterRules<Row>(
    columns.value.filter(col => col.key !== 'downloadable'),
  ),
)

const downloadHistoryFile = async (row: Row) => {
  if (!row.downloadable || downloadMutation.isPending.value) return

  try {
    const blob = await downloadMutation.mutateAsync(row.id) as Blob

    downloadBlob(
      blob,
      resolveDownloadFileName(row),
    )
  } catch (error) {
    console.error(error)
    alert('バックアップファイルの再ダウンロードに失敗しました。')
  }
}
</script>

<template>
  <SimpleTable
    table-key="backup-history-list"
    item-key="id"
    :items="rows"
    :columns="columns"
    :filter-rules="filterRules"
  >
    <template #[`item.downloadable`]="{ item }">
      <v-btn
        v-if="item.downloadable"
        size="small"
        variant="tonal"
        color="primary"
        prepend-icon="mdi-download"
        :loading="downloadMutation.isPending.value"
        @click.stop="downloadHistoryFile(item)"
      >
        再DL
      </v-btn>

      <span v-else class="empty-text">-</span>
    </template>

    <template #[`item.status`]="{ item }">
      <v-chip
        size="small"
        variant="tonal"
        :color="item.statusColor"
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

      <span v-else class="empty-text">-</span>
    </template>

    <template #[`item.storedFileKey`]="{ item }">
      <span
        v-if="item.storedFileKey"
        class="file-key"
        :title="item.storedFileKey"
      >
        {{ item.storedFileKey }}
      </span>

      <span v-else class="empty-text">-</span>
    </template>

    <template #[`item.errorMessage`]="{ item }">
      <span :class="{ 'error-text': item.errorMessage }">
        {{ item.errorMessage || '-' }}
      </span>
    </template>
  </SimpleTable>
</template>

<style scoped>
.empty-text {
  color: #94a3b8;
}

.error-text {
  color: #b91c1c;
}

.file-key {
  display: inline-block;
  max-width: 340px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  vertical-align: bottom;
}
</style>