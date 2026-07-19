import { computed, type Ref } from 'vue'
import type {
  SimpleTableColumnDef,
  SimpleTableEditableRow,
} from '@/shared/components/table/simple_table/types/item/types'
import { createSimpleTableFilterRules } from '@/shared/components/table/simple_table/utils/createSimpleTableFilterRules'
import type {
  BatchExecutionLogResponse,
  StorageType,
} from '@/features/system/batch/types/batchApiTypes'

export type BatchExecutionLogTableRow = SimpleTableEditableRow & {
  id: number
  jobCode: string
  jobName: string
  jobType: string
  targetCode: string
  status: string
  statusColor: string
  startedAt: string
  finishedAt: string
  message: string
  errorMessage: string
  storageType: StorageType | ''
  outputFileKey: string
  outputFileName: string
  contentType: string
  fileSizeText: string
  raw: BatchExecutionLogResponse
}

const resolveStatusColor = (status: string): string => {
  if (status === 'COMPLETED') return 'success'
  if (status === 'FAILED') return 'error'
  if (status === 'STARTED') return 'primary'
  return 'default'
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

export const useBatchExecutionLogTableConfig = (
  logs: Ref<BatchExecutionLogResponse[]>,
) => {
  const rows = computed<BatchExecutionLogTableRow[]>(() =>
    logs.value.map((item) => ({
      id: item.id,
      jobCode: item.jobCode,
      jobName: item.jobName,
      jobType: item.jobType,
      targetCode: item.targetCode,
      status: item.status,
      statusColor: resolveStatusColor(item.status),
      startedAt: item.startedAt ?? '',
      finishedAt: item.finishedAt ?? '',
      message: item.message ?? '',
      errorMessage: item.errorMessage ?? '',
      storageType: item.storageType ?? '',
      outputFileKey: item.outputFileKey ?? '',
      outputFileName: item.outputFileName ?? '',
      contentType: item.contentType ?? '',
      fileSizeText: formatFileSize(item.fileSize),
      raw: item,
    })),
  )

  const columns = computed(() => {
    const defs: SimpleTableColumnDef<BatchExecutionLogTableRow>[] = [
      { title: 'ID', key: 'id', width: '200px', filter: { type: 'text' } },
      { title: 'jobCode', key: 'jobCode', width: '220px', filter: { type: 'text' } },
      { title: 'jobType', key: 'jobType', width: '200px', filter: { type: 'text' } },
      { title: 'targetCode', key: 'targetCode', width: '220px', filter: { type: 'text' } },
      { title: 'status', key: 'status', width: '200px', filter: { type: 'text' } },
      { title: 'startedAt', key: 'startedAt', width: '300px', filter: { type: 'text' } },
      { title: 'finishedAt', key: 'finishedAt', width: '300px', filter: { type: 'text' } },
      { title: 'message', key: 'message', width: '300px', filter: { type: 'text' } },
      { title: 'error', key: 'errorMessage', width: '800px', filter: { type: 'text' } },
      { title: 'storage', key: 'storageType', width: '200px', filter: { type: 'text' } },
      { title: 'file', key: 'outputFileName', width: '400px', filter: { type: 'text' } },
      { title: 'size', key: 'fileSizeText', width: '150px', filter: { type: 'text' } },
    ]

    return defs
  })

  const filterRules = computed(() =>
    createSimpleTableFilterRules<BatchExecutionLogTableRow>(columns.value),
  )

  return {
    rows,
    columns,
    filterRules,
  }
}