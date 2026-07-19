import type {
  BatchJobType,
  BatchScheduleType,
} from '@/features/system/batch/types/batchApiTypes'

export type BatchJobDefinitionForm = {
  id: number
  jobCode: string
  jobName: string
  jobType: BatchJobType
  targetCode: string
  immediateExecutable: boolean
  scheduleEnabled: boolean
  scheduleType: BatchScheduleType
  cronExpression: string
  activeFlag: boolean
  description: string
}