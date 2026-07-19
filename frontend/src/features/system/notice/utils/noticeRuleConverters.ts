import type { NoticeRuleSaveRequest } from '@/features/system/notice/types/noticeRuleApiTypes'
import type { NoticeRuleForm } from '@/features/system/notice/types/noticeRuleFormTypes'

const blankToNull = (value: string): string | null =>
  value.trim() ? value : null

export const toNoticeRuleSaveRequest = (
  form: NoticeRuleForm,
): NoticeRuleSaveRequest => ({
  ruleCode: form.ruleCode,
  ruleName: form.ruleName,

  targetTableName: form.targetTableName,
  targetKeyColumnName: form.targetKeyColumnName,

  targetDateSourceType: form.targetDateSourceType,

  targetDateColumnName: blankToNull(form.targetDateColumnName),
  targetDayTypeColumnName: blankToNull(form.targetDayTypeColumnName),
  targetDayValueColumnName: blankToNull(form.targetDayValueColumnName),

  targetLabelColumnName: blankToNull(form.targetLabelColumnName),
  whereClause: blankToNull(form.whereClause),

  noticeTitleTemplate: form.noticeTitleTemplate,
  noticeBodyTemplate: form.noticeBodyTemplate,
  noticeContentFormat: form.noticeContentFormat,

  noticeSeverity: form.noticeSeverity,
  dateType: form.dateType,
  daysBefore: form.daysBefore,
  dayOfMonth: form.dayOfMonth,
  cronExpression: blankToNull(form.cronExpression),
  activeFlag: form.activeFlag,
})