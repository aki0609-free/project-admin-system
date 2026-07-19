export type StorageType =
  | 'LOCAL'
  | 'S3'

export type BatchJobType =
  | 'BACKUP'
  | 'IMPORT'
  | 'MAIL'
  | 'REPORT'
  | 'REPORT_MAIL'

export type BatchScheduleType =
  | 'NONE'
  | 'CRON'

export type BatchExecutionStatus =
  | 'STARTED'
  | 'COMPLETED'
  | 'FAILED'

export type BatchJobDefinitionResponse = {
  id: number
  jobCode: string
  jobName: string
  jobType: BatchJobType
  targetCode: string
  immediateExecutable: boolean
  scheduleEnabled: boolean
  scheduleType: BatchScheduleType
  cronExpression: string | null
  lastExecutedAt: string | null
  nextExecuteAt: string | null
  activeFlag: boolean
  description: string | null
}

export type BatchJobDefinitionSaveRequest = {
  jobCode: string
  jobName: string
  jobType: BatchJobType
  targetCode: string
  immediateExecutable: boolean
  scheduleEnabled: boolean
  scheduleType: BatchScheduleType
  cronExpression: string | null
  activeFlag: boolean
  description: string | null
}

export type BatchExecuteResponse = {
  jobCode: string
  status: BatchExecutionStatus
  message: string
  logId: number

  storageType?: StorageType | null
  outputFileKey?: string | null
  outputFileName?: string | null
  contentType?: string | null
  fileSize?: number | null
}

export type BatchExecutionLogResponse = {
  id: number
  jobCode: string
  jobName: string
  jobType: BatchJobType
  targetCode: string
  status: BatchExecutionStatus

  startedAt: string | null
  finishedAt: string | null

  message: string | null
  errorMessage: string | null

  storageType: StorageType | null
  outputFileKey: string | null
  outputFileName: string | null
  contentType: string | null
  fileSize: number | null
}

export type BatchExecuteRequest = {
  params?: Record<string, unknown>
}

export type BatchExecutePayload = {
  jobCode: string
  params?: Record<string, unknown>
}