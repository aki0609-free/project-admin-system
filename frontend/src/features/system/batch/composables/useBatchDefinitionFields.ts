import { computed } from 'vue'
import type { GridFormFieldDef } from '@/shared/components/form/grid_based_form/types/types'
import type { BatchJobDefinitionForm } from '@/features/system/batch/types/batchFormTypes'

export const useBatchDefinitionFields = (
  formModel: BatchJobDefinitionForm,
) => {
  const fields = computed<GridFormFieldDef<BatchJobDefinitionForm>[]>(() => [
    {
      key: 'jobCode',
      label: 'jobCode',
      type: 'text',
      gridColumn: '1 / span 4',
    },
    {
      key: 'jobName',
      label: 'jobName',
      type: 'text',
      gridColumn: '1 / span 3',
    },
    {
      key: 'jobType',
      label: 'jobType',
      type: 'select',
      gridColumn: '4 / span 1',
      options: [
        { title: 'BACKUP', value: 'BACKUP' },
        { title: 'IMPORT', value: 'IMPORT' },
        { title: 'MAIL', value: 'MAIL' },
        { title: 'REPORT', value: 'REPORT' },
        { title: 'REPORT_MAIL', value: 'REPORT_MAIL' },
      ],
    },
    {
      key: 'targetCode',
      label: 'targetCode',
      type: 'text',
      gridColumn: '1 / span 4',
    },
    {
      key: 'immediateExecutable',
      label: '即時実行可',
      type: 'checkbox',
      width: 120,
    },
    {
      key: 'scheduleEnabled',
      label: 'Schedule有効',
      type: 'checkbox',
      width: 140,
    },
    {
      key: 'activeFlag',
      label: '有効',
      type: 'checkbox',
      width: 100,
    },
    {
      key: 'scheduleType',
      label: 'scheduleType',
      type: 'select',
      options: [
        { title: 'NONE', value: 'NONE' },
        { title: 'CRON', value: 'CRON' },
      ],
      disabled: !formModel.scheduleEnabled,
    },
    {
      key: 'cronExpression',
      label: 'cronExpression',
      type: 'text',
      gridColumn: '1 / span 4',
      disabled: !formModel.scheduleEnabled || formModel.scheduleType !== 'CRON',
    },
  ])

  return {
    fields,
  }
}