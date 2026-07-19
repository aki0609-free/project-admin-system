export type NoticeDateType =
  | 'BEFORE_DAYS'
  | 'EXACT_DAY'
  | 'DAY_OF_MONTH'
  | 'MONTH_END'
  | 'AFTER_DAYS'

export type NoticeSeverity =
  | 'INFO'
  | 'WARNING'
  | 'IMPORTANT'

export type NoticeContentFormat =
  | 'PLAIN_TEXT'
  | 'HTML'
  | 'MARKDOWN'

export type NoticeTargetDateSourceType =
  | 'DATE_COLUMN'
  | 'DAY_RULE'

export type NoticeRuleResponse = {
  id: number
  ruleCode: string
  ruleName: string
  targetTableName: string
  targetKeyColumnName: string
  targetDateSourceType: NoticeTargetDateSourceType
  targetDateColumnName: string | null
  targetDayTypeColumnName: string | null
  targetDayValueColumnName: string | null
  targetLabelColumnName: string | null
  whereClause: string | null
  noticeTitleTemplate: string
  noticeBodyTemplate: string
  noticeContentFormat: NoticeContentFormat
  noticeSeverity: NoticeSeverity
  dateType: NoticeDateType
  daysBefore: number | null
  dayOfMonth: number | null
  cronExpression: string | null
  activeFlag: boolean
}

export type NoticeRuleSaveRequest = Omit<NoticeRuleResponse, 'id'>

export type NoticeGenerateResult = {
  ruleCount: number
  targetCount: number
  generatedCount: number
  skippedCount: number
  message: string
}