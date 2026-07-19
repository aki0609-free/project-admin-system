import { computed } from 'vue'
import { z } from 'zod'
import type { FormSection } from '@/shared/components/form/sectioned_form/types/types'
import type { ReportParamFormItem } from '@/features/system/report/types/reportFormTypes'

export const reportParamDetailSchema = z.object({
  id: z.number(),
  paramName: z.string().min(1, '必須です'),
  paramLabel: z.string().min(1, '必須です'),
  paramType: z.enum(['STRING', 'LONG', 'DATE', 'DATETIME', 'BOOLEAN', 'DECIMAL']),
  controlType: z.enum([
    'TEXT',
    'NUMBER',
    'DATE',
    'DATETIME',
    'CHECKBOX',
    'SELECT',
    'MULTI_SELECT',
    'TEXTAREA',
  ]),
  requiredFlag: z.boolean(),
  visibleFlag: z.boolean(),
  multipleFlag: z.boolean(),
  filterFlag: z.boolean(),
  defaultValue: z.string().nullable().optional(),
  placeholder: z.string().nullable().optional(),
  inputColumnName: z.string().nullable().optional(),
  displayOrder: z.number().min(1),
  activeFlag: z.boolean(),
})

const paramTypeOptions = [
  { title: 'STRING', value: 'STRING' },
  { title: 'LONG', value: 'LONG' },
  { title: 'DATE', value: 'DATE' },
  { title: 'DATETIME', value: 'DATETIME' },
  { title: 'BOOLEAN', value: 'BOOLEAN' },
  { title: 'DECIMAL', value: 'DECIMAL' },
]

const controlTypeOptions = [
  { title: 'TEXT', value: 'TEXT' },
  { title: 'NUMBER', value: 'NUMBER' },
  { title: 'DATE', value: 'DATE' },
  { title: 'DATETIME', value: 'DATETIME' },
  { title: 'CHECKBOX', value: 'CHECKBOX' },
  { title: 'SELECT', value: 'SELECT' },
  { title: 'MULTI_SELECT', value: 'MULTI_SELECT' },
  { title: 'TEXTAREA', value: 'TEXTAREA' },
]

export const useReportParamDetailFormConfig = () => {
  const sections = computed<FormSection<ReportParamFormItem>[]>(() => [
    {
      title: '基本情報',
      fields: [
        { key: 'paramName', label: 'paramName', type: 'text' },
        { key: 'paramLabel', label: '表示名', type: 'text' },
        {
          key: 'paramType',
          label: 'paramType',
          type: 'select',
          options: paramTypeOptions,
        },
        {
          key: 'controlType',
          label: 'controlType',
          type: 'select',
          options: controlTypeOptions,
        },
      ],
    },
    {
      title: '入力補助',
      fields: [
        { key: 'defaultValue', label: '初期値', type: 'text' },
        { key: 'placeholder', label: 'placeholder', type: 'text' },
        { key: 'inputColumnName', label: 'inputColumnName', type: 'text' },
        { key: 'displayOrder', label: 'displayOrder', type: 'number' },
      ],
    },
    {
      title: '動作設定',
      fields: [
        { key: 'requiredFlag', label: '必須', type: 'checkbox' },
        { key: 'visibleFlag', label: '表示', type: 'checkbox' },
        { key: 'multipleFlag', label: '複数選択', type: 'checkbox' },
        { key: 'filterFlag', label: '検索条件', type: 'checkbox' },
        { key: 'activeFlag', label: '有効', type: 'checkbox' },
      ],
    },
  ])

  return {
    schema: reportParamDetailSchema,
    sections,
  }
}