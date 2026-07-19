import { computed, type Ref } from 'vue'
import type {
  SimpleTableColumnDef,
  SimpleTableEditableRow,
} from '@/shared/components/table/simple_table/types/item/types'
import { createSimpleTableFilterRules } from '@/shared/components/table/simple_table/utils/createSimpleTableFilterRules'
import type { NoticeRuleResponse } from '@/features/system/notice/types/noticeRuleApiTypes'

export type NoticeRuleTableRow = SimpleTableEditableRow & {
  id: number
  ruleCode: string
  ruleName: string
  targetTableName: string
  targetDateSourceType: string
  targetDateColumnName: string
  targetDayTypeColumnName: string
  targetDayValueColumnName: string
  noticeContentFormat: string
  noticeSeverity: string
  dateType: string
  cronExpression: string
  activeText: string
  raw: NoticeRuleResponse
}

const resolveTargetDateColumnText = (item: NoticeRuleResponse) => {
  if (item.targetDateSourceType === 'DAY_RULE') {
    return `${item.targetDayTypeColumnName ?? ''} / ${item.targetDayValueColumnName ?? ''}`
  }

  return item.targetDateColumnName ?? ''
}

export const useNoticeRuleTableConfig = (
  rules: Ref<NoticeRuleResponse[]>,
) => {
  const rows = computed<NoticeRuleTableRow[]>(() =>
    rules.value.map((item) => ({
      id: item.id,
      ruleCode: item.ruleCode,
      ruleName: item.ruleName,
      targetTableName: item.targetTableName,
      targetDateSourceType: item.targetDateSourceType ?? 'DATE_COLUMN',
      targetDateColumnName: resolveTargetDateColumnText(item),
      targetDayTypeColumnName: item.targetDayTypeColumnName ?? '',
      targetDayValueColumnName: item.targetDayValueColumnName ?? '',
      noticeContentFormat: item.noticeContentFormat ?? 'PLAIN_TEXT',
      noticeSeverity: item.noticeSeverity,
      dateType: item.dateType,
      cronExpression: item.cronExpression ?? '',
      activeText: item.activeFlag ? '有効' : '無効',
      raw: item,
    })),
  )

  const columns = computed(() => {
    const defs: SimpleTableColumnDef<NoticeRuleTableRow>[] = [
      { title: 'ID', key: 'id', width: '80px', filter: { type: 'text' } },
      { title: 'ruleCode', key: 'ruleCode', width: '220px', filter: { type: 'text' } },
      { title: 'ルール名', key: 'ruleName', width: '220px', filter: { type: 'text' } },
      { title: '対象テーブル', key: 'targetTableName', width: '180px', filter: { type: 'text' } },
      { title: '日付取得方式', key: 'targetDateSourceType', width: '140px', filter: { type: 'text' } },
      { title: '日付/DayRuleカラム', key: 'targetDateColumnName', width: '220px', filter: { type: 'text' } },
      { title: '本文形式', key: 'noticeContentFormat', width: '120px', filter: { type: 'text' } },
      { title: '重要度', key: 'noticeSeverity', width: '120px', filter: { type: 'text' } },
      { title: '日付条件', key: 'dateType', width: '160px', filter: { type: 'text' } },
      { title: 'cron', key: 'cronExpression', width: '180px', filter: { type: 'text' } },
      { title: '状態', key: 'activeText', width: '100px', filter: { type: 'text' } },
    ]

    return defs
  })

  const filterRules = computed(() =>
    createSimpleTableFilterRules<NoticeRuleTableRow>(columns.value),
  )

  return {
    rows,
    columns,
    filterRules,
  }
}