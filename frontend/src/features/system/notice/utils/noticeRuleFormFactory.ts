import type { NoticeRuleResponse } from '@/features/system/notice/types/noticeRuleApiTypes'
import type { NoticeRuleForm } from '@/features/system/notice/types/noticeRuleFormTypes'

export const createEmptyNoticeRuleForm = (): NoticeRuleForm => ({
  id: 0,
  ruleCode: '',
  ruleName: '',
  targetTableName: '',
  targetKeyColumnName: 'id',

  targetDateSourceType: 'DATE_COLUMN',
  targetDateColumnName: '',
  targetDayTypeColumnName: '',
  targetDayValueColumnName: '',

  targetLabelColumnName: '',
  whereClause: '',

  noticeTitleTemplate: '',
  noticeBodyTemplate: '',
  noticeContentFormat: 'PLAIN_TEXT',

  noticeSeverity: 'INFO',
  dateType: 'BEFORE_DAYS',
  daysBefore: 30,
  dayOfMonth: null,
  cronExpression: '0 0 8 * * *',
  activeFlag: true,
})

export const toNoticeRuleForm = (
  rule: NoticeRuleResponse,
): NoticeRuleForm => ({
  id: rule.id,
  ruleCode: rule.ruleCode,
  ruleName: rule.ruleName,
  targetTableName: rule.targetTableName,
  targetKeyColumnName: rule.targetKeyColumnName,

  targetDateSourceType: rule.targetDateSourceType ?? 'DATE_COLUMN',
  targetDateColumnName: rule.targetDateColumnName ?? '',
  targetDayTypeColumnName: rule.targetDayTypeColumnName ?? '',
  targetDayValueColumnName: rule.targetDayValueColumnName ?? '',

  targetLabelColumnName: rule.targetLabelColumnName ?? '',
  whereClause: rule.whereClause ?? '',

  noticeTitleTemplate: rule.noticeTitleTemplate,
  noticeBodyTemplate: rule.noticeBodyTemplate,
  noticeContentFormat: rule.noticeContentFormat ?? 'PLAIN_TEXT',

  noticeSeverity: rule.noticeSeverity,
  dateType: rule.dateType,
  daysBefore: rule.daysBefore,
  dayOfMonth: rule.dayOfMonth,
  cronExpression: rule.cronExpression ?? '',
  activeFlag: rule.activeFlag,
})