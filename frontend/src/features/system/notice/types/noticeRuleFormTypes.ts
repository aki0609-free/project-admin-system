import type {
  NoticeContentFormat,
  NoticeDateType,
  NoticeSeverity,
  NoticeTargetDateSourceType,
} from '@/features/system/notice/types/noticeRuleApiTypes'

export type NoticeRuleForm = {
  id: number
  ruleCode: string
  ruleName: string

  targetTableName: string
  targetKeyColumnName: string

  targetDateSourceType: NoticeTargetDateSourceType
  targetDateColumnName: string
  targetDayTypeColumnName: string
  targetDayValueColumnName: string

  targetLabelColumnName: string
  whereClause: string

  noticeTitleTemplate: string
  noticeBodyTemplate: string
  noticeContentFormat: NoticeContentFormat

  noticeSeverity: NoticeSeverity
  dateType: NoticeDateType
  daysBefore: number | null
  dayOfMonth: number | null
  cronExpression: string
  activeFlag: boolean
}