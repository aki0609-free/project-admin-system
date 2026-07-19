import type { BatchJobDefinitionSaveRequest } from '@/features/system/batch/types/batchApiTypes'
import type { BatchJobDefinitionForm } from '@/features/system/batch/types/batchFormTypes'

const blankToNull = (value: string): string | null =>
  value.trim() ? value : null

export const toBatchJobDefinitionSaveRequest = (
  form: BatchJobDefinitionForm,
): BatchJobDefinitionSaveRequest => ({
  jobCode: form.jobCode,
  jobName: form.jobName,
  jobType: form.jobType,
  targetCode: form.targetCode,
  immediateExecutable: form.immediateExecutable,
  scheduleEnabled: form.scheduleEnabled,
  scheduleType: form.scheduleEnabled ? form.scheduleType : 'NONE',
  cronExpression: form.scheduleEnabled ? blankToNull(form.cronExpression) : null,
  activeFlag: form.activeFlag,
  description: blankToNull(form.description),
})