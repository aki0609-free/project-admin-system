<!-- eslint-disable @typescript-eslint/no-explicit-any -->
<script setup lang="ts">
import { computed, ref } from 'vue'

import type {
  OperationReportPreviewResponse,
  OperationType,
} from '../types/operationReportPreviewTypes'
import { useOperationReportPreviewsQuery } from '../api/useOperationReportPreviewsQuery'
import { useOperationReportPreviewUrl } from '../api/useOperationReportPreviewUrl'
import { useOperationReportPreviewHtml } from '../api/useOperationReportPreviewHtml'
import { useExecuteBatchMutation } from '@/features/system/batch/api/mutations/useExecuteBatchMutation'
import { useDownloadBatchLogFileMutation } from '@/features/system/batch/api/mutations/useDownloadBatchLogFileMutation'
import type { BatchExecuteResponse } from '@/features/system/batch/types/batchApiTypes'
import PdfPreviewDialog from '@/shared/components/pdf/PdfPreviewDialog.vue'
import { getMonthlyClosingReportFiles } from '@/features/operation/monthly/api/getMonthlyClosingReportFiles'
import type { MonthlyClosingReportFileResponse } from '@/features/operation/monthly/types/monthlyReportFileTypes'

const props = defineProps<{
  operationType: OperationType
  targetDate?: string | null
  targetMonth?: string | null
  closingVersion?: number | null
  allowMixedClosingVersions?: boolean
  allowedReportCodes?: string[]
}>()

const dialog = ref(false)
const selectedReport = ref<OperationReportPreviewResponse | null>(null)

const { reports, refetch } = useOperationReportPreviewsQuery(
  computed(() => props.operationType),
)
const visibleReports = computed(() => {
  if (!props.allowedReportCodes?.length) return reports.value
  return reports.value.filter((report) =>
    props.allowedReportCodes?.includes(report.reportCode),
  )
})

const { previewUrl } = useOperationReportPreviewUrl({
  operationType: computed(() => props.operationType),
  selectedReport,
  targetDate: computed(() => props.targetDate),
  targetMonth: computed(() => props.targetMonth),
})
const htmlPreviewUrl = computed(() => {
  const outputType = selectedReport.value?.outputType
  return outputType && outputType !== 'NONE' && outputType !== 'EXCEL_BOOK'
    ? previewUrl.value
    : ''
})
const {
  previewHtml,
  isPreviewLoading,
  previewError,
  reloadPreview,
} = useOperationReportPreviewHtml(htmlPreviewUrl)
const previewIframe = ref<HTMLIFrameElement | null>(null)

const pdfPreviewDialog = ref(false)
const pdfPreviewUrl = ref<string | null>(null)
const pdfFileKey = ref<string | null>(null)
const pdfFileName = ref<string | null>(null)
const pdfStorageType = ref<'LOCAL' | 'S3'>('LOCAL')

const executeBatchMutation = useExecuteBatchMutation()
const downloadBatchLogFileMutation = useDownloadBatchLogFileMutation()
const monthlyFilesDialog = ref(false)
const monthlyFiles = ref<MonthlyClosingReportFileResponse[]>([])
const outputMessage = ref('')
const outputSnackbar = ref(false)

const selectReport = (report: OperationReportPreviewResponse) => {
  selectedReport.value = report
  dialog.value = true
}

const closeDialog = () => {
  dialog.value = false
}

const outputButtonLabel = computed(() => {
  switch (selectedReport.value?.outputType) {
    case 'HTML_PRINT':
      return 'ブラウザ印刷'
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
    case 'HTML_PRINT':
      return 'mdi-printer-outline'
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
  if (selectedReport.value?.outputType === 'HTML_PRINT') {
    return !!previewHtml.value && !isPreviewLoading.value
  }

  if (props.operationType === 'MONTHLY') {
    return !!props.targetMonth && (
      !!props.closingVersion || !!props.allowMixedClosingVersions
    )
  }

  return !!selectedReport.value?.jobCode &&
    selectedReport.value.outputType !== 'NONE' &&
    selectedReport.value.outputType !== 'HTML_PREVIEW'
})

const showOutputButton = computed(() => {
  const outputType = selectedReport.value?.outputType
  return outputType !== undefined &&
    outputType !== 'NONE' &&
    outputType !== 'HTML_PREVIEW'
})

const executeReport = async () => {
  if (selectedReport.value?.outputType === 'HTML_PRINT') {
    const previewWindow = previewIframe.value?.contentWindow
    if (!previewWindow || !previewHtml.value) {
      showOutputMessage('印刷対象のプレビューを読み込めませんでした。')
      return
    }
    previewWindow.focus()
    previewWindow.print()
    return
  }

  if (props.operationType === 'MONTHLY') {
    await openMonthlyStoredReport()
    return
  }

  if (!selectedReport.value?.jobCode) return
  if (selectedReport.value.outputType === 'NONE') return

  const targetParamName = selectedReport.value.targetParamName || 'targetDate'
  const targetValue = props.targetDate
  const params = {
    [targetParamName]: targetValue ?? null,
  }

  const result = (await executeBatchMutation.mutateAsync({
    jobCode: selectedReport.value.jobCode,
    params,
  })) as BatchExecuteResponse

  if (result.status === 'FAILED') {
    showOutputMessage(
      result.message || '帳票の生成に失敗しました。バッチ実行履歴を確認してください。',
    )
    return
  }

  if (!result.outputFileKey || !result.logId) {
    showOutputMessage('生成された帳票ファイルを取得できませんでした。')
    return
  }

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

const openMonthlyStoredReport = async () => {
  if (
    !selectedReport.value ||
    !props.targetMonth ||
    (!props.closingVersion && !props.allowMixedClosingVersions)
  ) {
    showOutputMessage('締め処理後に印刷・出力できます。')
    return
  }

  const files = await getMonthlyClosingReportFiles(
    props.targetMonth,
    props.closingVersion ?? null,
    selectedReport.value.reportCode,
  )

  if (files.length === 0) {
    showOutputMessage('この締めVersionには保存済み帳票がありません。再締め後に確認してください。')
    return
  }

  const onlyFile = files[0]
  if (files.length === 1 && onlyFile) {
    await openStoredFile(onlyFile)
    return
  }

  monthlyFiles.value = files
  monthlyFilesDialog.value = true
}

const openStoredFile = async (file: MonthlyClosingReportFileResponse) => {
  if (!file.batchExecutionLogId) {
    showOutputMessage('保存済み帳票のダウンロード情報がありません。')
    return
  }

  const blob = (await downloadBatchLogFileMutation.mutateAsync(
    file.batchExecutionLogId,
  )) as Blob
  const outputType = selectedReport.value?.outputType ?? 'CUSTOM'
  const fileName = file.outputFileName ||
    `${file.reportCode}.${resolveExtension(outputType)}`

  if (outputType === 'PDF') {
    if (pdfPreviewUrl.value) URL.revokeObjectURL(pdfPreviewUrl.value)
    pdfPreviewUrl.value = URL.createObjectURL(blob)
    pdfFileKey.value = file.outputFileKey
    pdfFileName.value = fileName
    pdfStorageType.value = file.storageType ?? 'LOCAL'
    pdfPreviewDialog.value = true
    monthlyFilesDialog.value = false
    return
  }

  downloadBlob(blob, fileName)
}

const showOutputMessage = (message: string) => {
  outputMessage.value = message
  outputSnackbar.value = true
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
          v-for="report in visibleReports"
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

        <tr v-if="visibleReports.length === 0">
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
            v-if="showOutputButton"
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
          <div v-if="isPreviewLoading" class="preview-state">
            <v-progress-circular indeterminate color="primary" />
            <span>プレビューを生成しています。</span>
          </div>

          <div v-else-if="previewError" class="preview-state preview-error">
            <span>{{ previewError }}</span>
            <v-btn size="small" variant="tonal" @click="reloadPreview">
              再試行
            </v-btn>
          </div>

          <iframe
            v-else-if="selectedReport && previewHtml"
            ref="previewIframe"
            class="preview-iframe"
            :srcdoc="previewHtml"
            sandbox="allow-same-origin allow-modals"
            title="帳票プレビュー"
          />

          <div v-else class="empty-preview">
            この出力形式は帳票プレビューの対象外です。
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

    <v-dialog v-model="monthlyFilesDialog" max-width="720">
      <v-card>
        <v-card-title>保存済み帳票を選択</v-card-title>
        <v-card-subtitle>
          {{ targetMonth }} / Version {{ closingVersion }}
        </v-card-subtitle>
        <v-list lines="two">
          <v-list-item
            v-for="file in monthlyFiles"
            :key="file.id"
            :title="file.targetName || file.outputFileName || file.reportCode"
            :subtitle="file.outputFileName || file.reportCode"
            prepend-icon="mdi-file-document-outline"
            @click="openStoredFile(file)"
          >
            <template #append>
              <v-btn size="small" color="primary" variant="tonal">
                開く
              </v-btn>
            </template>
          </v-list-item>
        </v-list>
        <v-card-actions>
          <v-spacer />
          <v-btn variant="text" @click="monthlyFilesDialog = false">閉じる</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <v-snackbar v-model="outputSnackbar" color="info" timeout="3500">
      {{ outputMessage }}
    </v-snackbar>
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

.preview-state {
  display: grid;
  place-items: center;
  align-content: center;
  gap: 12px;
  min-height: calc(90vh - 72px);
  color: #64748b;
}

.preview-error {
  color: #b91c1c;
}
</style>
