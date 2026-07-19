<!-- eslint-disable @typescript-eslint/no-explicit-any -->
<script setup lang="ts">
import { computed, ref } from 'vue'

import type {
  OperationReportPreviewResponse,
  OperationType,
} from '../types/operationReportPreviewTypes'
import { useOperationReportPreviewsQuery } from '../api/useOperationReportPreviewsQuery'
import { useOperationReportPreviewUrl } from '../api/useOperationReportPreviewUrl'
import { useExecuteBatchMutation } from '@/features/system/batch/api/mutations/useExecuteBatchMutation'
import { useDownloadBatchLogFileMutation } from '@/features/system/batch/api/mutations/useDownloadBatchLogFileMutation'
import type { BatchExecuteResponse } from '@/features/system/batch/types/batchApiTypes'
import PdfPreviewDialog from '@/shared/components/pdf/PdfPreviewDialog.vue'

const props = defineProps<{
  operationType: OperationType
  targetDate?: string | null
  targetMonth?: string | null
}>()

const dialog = ref(false)
const selectedReport = ref<OperationReportPreviewResponse | null>(null)

const { reports, refetch } = useOperationReportPreviewsQuery(
  computed(() => props.operationType),
)

const { previewUrl } = useOperationReportPreviewUrl({
  operationType: computed(() => props.operationType),
  selectedReport,
  targetDate: computed(() => props.targetDate),
  targetMonth: computed(() => props.targetMonth),
})

const pdfPreviewDialog = ref(false)
const pdfPreviewUrl = ref<string | null>(null)
const pdfFileKey = ref<string | null>(null)
const pdfFileName = ref<string | null>(null)
const pdfStorageType = ref<'LOCAL' | 'S3'>('LOCAL')

const executeBatchMutation = useExecuteBatchMutation()
const downloadBatchLogFileMutation = useDownloadBatchLogFileMutation()

const selectReport = (report: OperationReportPreviewResponse) => {
  selectedReport.value = report
  dialog.value = true
}

const closeDialog = () => {
  dialog.value = false
}

const outputButtonLabel = computed(() => {
  switch (selectedReport.value?.outputType) {
    case 'PDF':
      return '印刷'
    case 'CSV':
      return 'CSV出力'
    case 'EXCEL':
      return 'Excel出力'
    case 'EXCEL_BOOK':
      return '台帳更新'
    case 'CUSTOM':
      return '実行'
    default:
      return ''
  }
})

const outputButtonIcon = computed(() => {
  switch (selectedReport.value?.outputType) {
    case 'PDF':
      return 'mdi-printer'
    case 'CSV':
      return 'mdi-file-delimited-outline'
    case 'EXCEL':
    case 'EXCEL_BOOK':
      return 'mdi-file-excel-outline'
    case 'CUSTOM':
      return 'mdi-play-circle-outline'
    default:
      return ''
  }
})

const canOutput = computed(() => {
  return !!selectedReport.value?.jobCode && selectedReport.value.outputType !== 'NONE'
})

const executeReport = async () => {
  if (!selectedReport.value?.jobCode) return
  if (selectedReport.value.outputType === 'NONE') return

  const params =
    props.operationType === 'MONTHLY'
      ? {
          target_month: props.targetMonth ?? null,
          history_version: null,
          view_name: selectedReport.value.tableName,
          work_table: selectedReport.value.templateName,
        }
      : {
          target_date: props.targetDate ?? null,
          view_name: selectedReport.value.tableName,
          work_table: selectedReport.value.templateName,
        }

  const result = (await executeBatchMutation.mutateAsync({
    jobCode: selectedReport.value.jobCode,
    params,
  })) as BatchExecuteResponse

  if (!result.outputFileKey || !result.logId) return

  const blob = (await downloadBatchLogFileMutation.mutateAsync(result.logId)) as Blob

  const fileName =
    result.outputFileName ||
    `${selectedReport.value.reportCode}.${resolveExtension(selectedReport.value.outputType)}`

  switch (selectedReport.value.outputType) {
    case 'CSV':
    case 'EXCEL':
    case 'EXCEL_BOOK':
    case 'CUSTOM':
      downloadBlob(blob, fileName)
      return

    case 'PDF':
      pdfPreviewUrl.value = URL.createObjectURL(blob)
      pdfFileKey.value = result.outputFileKey ?? ''
      pdfFileName.value = fileName
      pdfStorageType.value = result.storageType ?? 'LOCAL'
      pdfPreviewDialog.value = true
      return
  }
}

function resolveExtension(outputType: string): string {
  switch (outputType) {
    case 'CSV':
      return 'csv'
    case 'EXCEL':
    case 'EXCEL_BOOK':
      return 'xlsx'
    case 'PDF':
      return 'pdf'
    default:
      return 'dat'
  }
}

function downloadBlob(blob: Blob, fileName: string) {
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')

  link.href = url
  link.download = fileName
  link.click()

  URL.revokeObjectURL(url)
}
</script>

<template>
  <div class="operation-report-tab">
    <div class="report-header">
      <div>
        <div class="title">帳票一覧</div>
        <div class="description">行をクリックするとプレビューを表示します。</div>
      </div>

      <v-btn
        size="small"
        color="secondary"
        variant="tonal"
        prepend-icon="mdi-refresh"
        @click="refetch"
      >
        再読込
      </v-btn>
    </div>

    <v-table density="comfortable" class="report-table">
      <thead>
        <tr>
          <th class="name-col">帳票名</th>
          <th>帳票コード</th>
          <th>出力種別</th>
          <th>処理コード</th>
        </tr>
      </thead>

      <tbody>
        <tr
          v-for="report in reports"
          :key="report.reportCode"
          @click="selectReport(report)"
        >
          <td class="name-cell">
            <v-icon size="18" class="mr-2">mdi-file-document-outline</v-icon>
            {{ report.reportName }}
          </td>

          <td>
            <v-chip size="small" variant="tonal">
              {{ report.reportCode }}
            </v-chip>
          </td>

          <td>
            <v-chip
              size="small"
              variant="tonal"
              :color="report.outputType === 'NONE' ? 'grey' : 'primary'"
            >
              {{ report.outputType }}
            </v-chip>
          </td>

          <td>
            {{ report.jobCode ?? '-' }}
          </td>
        </tr>

        <tr v-if="reports.length === 0">
          <td colspan="4" class="empty-cell">帳票が登録されていません。</td>
        </tr>
      </tbody>
    </v-table>

    <v-dialog v-model="dialog" max-width="1280" height="90vh" scrollable>
      <v-card class="preview-dialog">
        <v-card-title class="dialog-title">
          <div>
            <div class="dialog-report-name">
              {{ selectedReport?.reportName }}
            </div>
            <div class="dialog-report-code">
              {{ selectedReport?.reportCode }}
            </div>
          </div>

          <v-spacer />

          <v-btn
            v-if="selectedReport?.outputType !== 'NONE'"
            color="primary"
            :prepend-icon="outputButtonIcon"
            :disabled="!canOutput"
            @click="executeReport"
          >
            {{ outputButtonLabel }}
          </v-btn>

          <v-btn icon="mdi-close" variant="text" @click="closeDialog" />
        </v-card-title>

        <v-divider />

        <v-card-text class="preview-body">
          <iframe
            v-if="selectedReport"
            class="preview-iframe"
            :src="previewUrl"
          />

          <div v-else class="empty-preview">
            帳票を選択してください。
          </div>
        </v-card-text>
      </v-card>
    </v-dialog>

    <PdfPreviewDialog
      v-model="pdfPreviewDialog"
      :title="selectedReport?.reportName"
      :pdf-url="pdfPreviewUrl"
      :pdf-file-key="pdfFileKey"
      :pdf-file-name="pdfFileName"
      :storage-type="pdfStorageType"
    />
  </div>
</template>

<style scoped>
.operation-report-tab {
  display: grid;
  gap: 12px;
}

.report-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.title {
  font-size: 16px;
  font-weight: 800;
  color: #0f172a;
}

.description {
  margin-top: 2px;
  font-size: 12px;
  color: #64748b;
}

.report-table {
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  overflow: hidden;
  background: #fff;
}

.report-table tbody tr {
  cursor: pointer;
  transition: background 0.15s ease;
}

.report-table tbody tr:hover {
  background: #f8fafc;
}

.name-col {
  width: 38%;
}

.name-cell {
  font-weight: 700;
  color: #0f172a;
}

.empty-cell {
  padding: 32px;
  text-align: center;
  color: #64748b;
}

.preview-dialog {
  height: 90vh;
  display: flex;
  flex-direction: column;
}

.dialog-title {
  display: flex;
  align-items: center;
  gap: 12px;
  min-height: 64px;
}

.dialog-report-name {
  font-size: 17px;
  font-weight: 800;
}

.dialog-report-code {
  margin-top: 2px;
  font-size: 12px;
  color: #64748b;
}

.preview-body {
  flex: 1;
  padding: 0;
  background: #f8fafc;
}

.preview-iframe {
  width: 100%;
  height: 100%;
  min-height: calc(90vh - 72px);
  border: 0;
  background: #fff;
}

.empty-preview {
  display: grid;
  place-items: center;
  height: 100%;
  color: #64748b;
}
</style>