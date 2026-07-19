import { computed, type Ref } from 'vue'
import type {
  SimpleTableColumnDef,
  SimpleTableEditableRow,
} from '@/shared/components/table/simple_table/types/item/types'
import { createSimpleTableFilterRules } from '@/shared/components/table/simple_table/utils/createSimpleTableFilterRules'
import type { BatchJobDefinitionResponse } from '@/features/system/batch/types/batchApiTypes'

export type BatchDefinitionTableRow = SimpleTableEditableRow & {
  id: number
  jobCode: string
  jobName: string
  jobType: string
  targetCode: string
  immediateText: string
  scheduleText: string
  cronExpression: string
  lastExecutedAt: string
  activeText: string
  raw: BatchJobDefinitionResponse
}

export const useBatchDefinitionTableConfig = (
  definitions: Ref<BatchJobDefinitionResponse[]>,
) => {
  const rows = computed<BatchDefinitionTableRow[]>(() =>
    definitions.value.map((item) => ({
      id: item.id,
      jobCode: item.jobCode,
      jobName: item.jobName,
      jobType: item.jobType,
      targetCode: item.targetCode,
      immediateText: item.immediateExecutable ? '可' : '不可',
      scheduleText: item.scheduleEnabled ? item.scheduleType : 'なし',
      cronExpression: item.cronExpression ?? '',
      lastExecutedAt: item.lastExecutedAt ?? '',
      activeText: item.activeFlag ? '有効' : '無効',
      raw: item,
    })),
  )

  const columns = computed(() => {
    const defs: SimpleTableColumnDef<BatchDefinitionTableRow>[] = [
      { title: 'ID', key: 'id', width: '80px', filter: { type: 'text' } },
      { title: 'jobCode', key: 'jobCode', width: '260px', filter: { type: 'text' } },
      { title: 'jobName', key: 'jobName', width: '260px', filter: { type: 'text' } },
      { title: 'type', key: 'jobType', width: '140px', filter: { type: 'text' } },
      { title: 'targetCode', key: 'targetCode', width: '220px', filter: { type: 'text' } },
      { title: '即時', key: 'immediateText', width: '90px', filter: { type: 'text' } },
      { title: 'Schedule', key: 'scheduleText', width: '120px', filter: { type: 'text' } },
      { title: 'cron', key: 'cronExpression', width: '180px', filter: { type: 'text' } },
      { title: '最終実行', key: 'lastExecutedAt', width: '300px', filter: { type: 'text' } },
      { title: '状態', key: 'activeText', width: '100px', filter: { type: 'text' } },
    ]

    return defs
  })

  const filterRules = computed(() =>
    createSimpleTableFilterRules<BatchDefinitionTableRow>(columns.value),
  )

  return {
    rows,
    columns,
    filterRules,
  }
}