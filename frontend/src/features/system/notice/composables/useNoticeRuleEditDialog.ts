import { computed, reactive, watch, type Ref } from 'vue'
import { z } from 'zod'
import type { GridFormFieldDef } from '@/shared/components/form/grid_based_form/types/types'
import type { ToolbarItem } from '@/shared/components/toolbar/types/types'
import type { NoticeRuleResponse } from '@/features/system/notice/types/noticeRuleApiTypes'
import type { NoticeRuleForm } from '@/features/system/notice/types/noticeRuleFormTypes'
import {
  createEmptyNoticeRuleForm,
  toNoticeRuleForm,
} from '@/features/system/notice/utils/noticeRuleFormFactory'

export const noticeRuleSchema = z.object({
  id: z.number(),

  ruleCode: z.string().min(1, '必須です'),
  ruleName: z.string().min(1, '必須です'),

  targetTableName: z.string().min(1, '必須です'),
  targetKeyColumnName: z.string().min(1, '必須です'),

  targetDateSourceType: z.enum(['DATE_COLUMN', 'DAY_RULE']),

  targetDateColumnName: z.string(),
  targetDayTypeColumnName: z.string(),
  targetDayValueColumnName: z.string(),

  targetLabelColumnName: z.string(),
  whereClause: z.string(),

  noticeTitleTemplate: z.string().min(1, '必須です'),
  noticeBodyTemplate: z.string().min(1, '必須です'),
  noticeContentFormat: z.enum(['PLAIN_TEXT', 'HTML', 'MARKDOWN']),

  noticeSeverity: z.enum(['INFO', 'WARNING', 'IMPORTANT']),
  dateType: z.enum(['BEFORE_DAYS', 'EXACT_DAY', 'DAY_OF_MONTH', 'MONTH_END', 'AFTER_DAYS']),
  daysBefore: z.number().nullable(),
  dayOfMonth: z.number().nullable(),
  cronExpression: z.string(),
  activeFlag: z.boolean(),
})

export const useNoticeRuleEditDialog = (
  visible: Ref<boolean>,
  rule: Ref<NoticeRuleResponse | null>,
  emitSave: (form: NoticeRuleForm) => void,
  emitDelete: (form: NoticeRuleForm) => void,
) => {
  const formModel = reactive<NoticeRuleForm>(createEmptyNoticeRuleForm())

  const resetForm = () => {
    Object.assign(formModel, createEmptyNoticeRuleForm())
  }

  watch(
    () => visible.value,
    (opened) => {
      if (!opened) return

      if (!rule.value) {
        resetForm()
      }
    },
    { immediate: true },
  )

  watch(
    () => rule.value,
    (value) => {
      if (!visible.value) return

      if (!value) {
        resetForm()
        return
      }

      Object.assign(formModel, toNoticeRuleForm(value))
    },
    { immediate: true },
  )

  const isEdit = computed(() => formModel.id > 0)

  const fields: GridFormFieldDef<NoticeRuleForm>[] = [
    { key: 'id', label: 'ID', type: 'number', width: 100 },

    { key: 'ruleCode', label: 'ruleCode', type: 'text', gridColumn: '2 / span 2' },
    { key: 'ruleName', label: 'ルール名', type: 'text', gridColumn: '1 / span 3' },
    { key: 'activeFlag', label: '有効', type: 'checkbox', width: 100 },

    { key: 'targetTableName', label: '対象テーブル', type: 'text', gridColumn: '1 / span 2' },
    { key: 'targetKeyColumnName', label: 'KEYカラム', type: 'text', gridColumn: '3 / span 2' },

    {
      key: 'targetDateSourceType',
      label: '日付取得方式',
      type: 'select',
      gridColumn: '1 / span 2',
      options: [
        { title: '日付カラム', value: 'DATE_COLUMN' },
        { title: 'DayRule', value: 'DAY_RULE' },
      ],
    },

    { key: 'targetDateColumnName', label: '日付カラム', type: 'text', gridColumn: '3 / span 2' },
    { key: 'targetDayTypeColumnName', label: 'DayRule種別カラム', type: 'text', gridColumn: '1 / span 2' },
    { key: 'targetDayValueColumnName', label: 'DayRule値カラム', type: 'text', gridColumn: '3 / span 2' },

    { key: 'targetLabelColumnName', label: '表示名カラム', type: 'text', gridColumn: '1 / span 2' },
    { key: 'whereClause', label: 'WHERE条件', type: 'text', gridColumn: '3 / span 2' },

    {
      key: 'noticeSeverity',
      label: '重要度',
      type: 'select',
      options: [
        { title: 'INFO', value: 'INFO' },
        { title: 'WARNING', value: 'WARNING' },
        { title: 'IMPORTANT', value: 'IMPORTANT' },
      ],
    },
    {
      key: 'dateType',
      label: '日付条件',
      type: 'select',
      options: [
        { title: 'BEFORE_DAYS', value: 'BEFORE_DAYS' },
        { title: 'EXACT_DAY', value: 'EXACT_DAY' },
        { title: 'DAY_OF_MONTH', value: 'DAY_OF_MONTH' },
        { title: 'MONTH_END', value: 'MONTH_END' },
        { title: 'AFTER_DAYS', value: 'AFTER_DAYS' },
      ],
    },
    { key: 'daysBefore', label: '日数', type: 'number' },
    { key: 'dayOfMonth', label: '毎月日付', type: 'number' },

    { key: 'cronExpression', label: 'cron', type: 'text', gridColumn: '1 / span 4' },
    { key: 'noticeTitleTemplate', label: 'タイトルテンプレート', type: 'text', gridColumn: '1 / span 4' },
  ]

  const close = () => {
    visible.value = false
  }

  const save = () => {
    emitSave({ ...formModel })
  }

  const remove = () => {
    emitDelete({ ...formModel })
  }

  const footerItems = computed<ToolbarItem[]>(() => {
    const items: ToolbarItem[] = []

    if (isEdit.value) {
      items.push({
        type: 'button',
        label: '削除',
        color: 'error',
        onClick: remove,
      })
    }

    items.push(
      {
        type: 'button',
        label: '閉じる',
        color: 'secondary',
        onClick: close,
      },
      {
        type: 'button',
        label: '保存',
        color: 'primary',
        onClick: save,
      },
    )

    return items
  })

  return {
    formModel,
    isEdit,
    fields,
    schema: noticeRuleSchema,
    footerItems,
  }
}