import { computed } from 'vue'
import type {
  SimpleTableColumnDef,
  SimpleTableEditableRow,
} from '@/shared/components/table/simple_table/types/item/types'
import { createSimpleTableFilterRules } from '@/shared/components/table/simple_table/utils/createSimpleTableFilterRules'
import type { ReportHistoryResponse } from '@/features/system/report/types/reportHistoryApiTypes'

export type ReportHistoryTableRow = SimpleTableEditableRow & {
  id: number
  reportCode: string
  reportName: string
  fileName: string
  outputFormat: string
  status: string
  statusColor: string
  storageType: string
  storedFileKey: string
  downloadable: boolean
  fileSizeText: string
  executedBy: string
  createdAt: string
  raw: ReportHistoryResponse
}

const formatFileSize = (
  size: number | null | undefined,
): string => {
  if (size == null) return ''

  if (size < 1024) {
    return `${size} B`
  }

  if (size < 1024 * 1024) {
    return `${Math.round(size / 1024)} KB`
  }

  return `${Math.round(size / 1024 / 1024)} MB`
}

const resolveStatusColor = (
  status: string,
): string => {
  if (status === 'SUCCESS') return 'success'
  if (status === 'FAILED') return 'error'
  if (status === 'PROCESSING') return 'primary'
  return 'default'
}

export const useReportHistoryTableConfig = (
  reportHistories: Readonly<{
    value: ReportHistoryResponse[]
  }>,
) => {
  const rows = computed<ReportHistoryTableRow[]>(() =>
    reportHistories.value.map((item) => {
      const storageType = item.storageType ?? ''
      const storedFileKey = item.storedFileKey ?? ''

      return {
        id: item.id,
        reportCode: item.reportCode ?? '',
        reportName: item.reportName ?? '',
        fileName: item.storedFileName ?? item.fileName ?? '',
        outputFormat: item.outputFormat,
        status: item.status,
        statusColor: resolveStatusColor(item.status),
        storageType,
        storedFileKey,
        downloadable: Boolean(storageType && storedFileKey),
        fileSizeText: formatFileSize(item.fileSize),
        executedBy: item.executedBy ?? '',
        createdAt: item.createdAt ?? '',
        raw: item,
      }
    }),
  )

  const columns = computed(() => {
    const defs: SimpleTableColumnDef<ReportHistoryTableRow>[] = [
      { title: 'ID', key: 'id', width: '120px', filter: { type: 'text' } },
      { title: '帳票コード', key: 'reportCode', width: '180px', filter: { type: 'text' } },
      { title: '帳票名', key: 'reportName', width: '220px', filter: { type: 'text' } },
      { title: 'ファイル名', key: 'fileName', width: '320px', filter: { type: 'text' } },
      { title: '形式', key: 'outputFormat', width: '120px', filter: { type: 'text' } },
      { title: '状態', key: 'status', width: '120px', filter: { type: 'text' } },
      { title: '保存先', key: 'storageType', width: '120px', filter: { type: 'text' } },
      { title: 'サイズ', key: 'fileSizeText', width: '120px', filter: { type: 'text' } },
      { title: '実行者', key: 'executedBy', width: '160px', filter: { type: 'text' } },
      { title: '作成日時', key: 'createdAt', width: '220px', filter: { type: 'text' } },
    ]

    return defs
  })

  const filterRules = computed(() =>
    createSimpleTableFilterRules<ReportHistoryTableRow>(columns.value),
  )

  return {
    rows,
    columns,
    filterRules,
  }
}