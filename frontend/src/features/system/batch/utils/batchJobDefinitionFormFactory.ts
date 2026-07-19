import type { BatchJobDefinitionResponse } from '@/features/system/batch/types/batchApiTypes'
import type { BatchJobDefinitionForm } from '@/features/system/batch/types/batchFormTypes'

export const createEmptyBatchJobDefinitionForm =
  (): BatchJobDefinitionForm => ({
    id: 0,
    jobCode: '',
    jobName: '',
    jobType: 'BACKUP',
    targetCode: '',
    immediateExecutable: true,
    scheduleEnabled: false,
    scheduleType: 'NONE',
    cronExpression: '',
    activeFlag: true,
    description: '',
  })

export const toBatchJobDefinitionForm = (
  item: BatchJobDefinitionResponse,
): BatchJobDefinitionForm => ({
  id: item.id,
  jobCode: item.jobCode,
  jobName: item.jobName,
  jobType: item.jobType,
  targetCode: item.targetCode,
  immediateExecutable: item.immediateExecutable,
  scheduleEnabled: item.scheduleEnabled,
  scheduleType: item.scheduleType,
  cronExpression: item.cronExpression ?? '',
  activeFlag: item.activeFlag,
  description: item.description ?? '',
})