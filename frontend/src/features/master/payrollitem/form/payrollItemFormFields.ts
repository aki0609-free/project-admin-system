import type { TabbedFormFieldDef } from '@/shared/components/form/tabbed_form/types/types'

export const createPayrollItemCalculationFields = <
  T extends {
    ruleName: string | null
    defaultAmount: number | null
    allowManualInput: boolean
    minAmount: number | null
    maxAmount: number | null
  },
>(): TabbedFormFieldDef<T>[] => [
  { key: 'ruleName', label: 'Rule名', type: 'text', tab: '計算設定' },
  { key: 'defaultAmount', label: '初期金額', type: 'number', tab: '計算設定' },
  { key: 'allowManualInput', label: '手入力許可', type: 'checkbox', tab: '計算設定' },
  { key: 'minAmount', label: '下限金額', type: 'number', tab: '計算設定' },
  { key: 'maxAmount', label: '上限金額', type: 'number', tab: '計算設定' },
]