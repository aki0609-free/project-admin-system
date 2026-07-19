import { computed } from 'vue'
import type { TabbedFormFieldDef } from '@/shared/components/form/tabbed_form/types/types'
import type { AllowanceMaster } from '@/features/master/allowance/types/allowanceTypes'
import {
  allowanceCalculationTypeOptions,
  allowanceDetailViewTypeOptions,
  allowanceTypeOptions,
  allowanceUnitOptions,
} from '@/features/master/allowance/constants/allowanceConstants'

export const allowanceFormTabs = ['基本情報', '計算設定', '表示設定'] as const

export const useAllowanceFormFields = () => {
  const fields = computed<TabbedFormFieldDef<AllowanceMaster>[]>(() => [
    { key: 'code', label: '手当コード', type: 'text', tab: '基本情報' },
    { key: 'name', label: '手当名', type: 'text', tab: '基本情報' },

    {
      key: 'allowanceType',
      label: '手当区分',
      type: 'select',
      tab: '基本情報',
      enumOptions: allowanceTypeOptions,
    },
    {
      key: 'calculationType',
      label: '計算区分',
      type: 'select',
      tab: '基本情報',
      enumOptions: allowanceCalculationTypeOptions,
    },
    {
      key: 'allowanceUnit',
      label: '支給単位',
      type: 'select',
      tab: '基本情報',
      enumOptions: allowanceUnitOptions,
    },
    {
      key: 'detailViewType',
      label: '詳細種類',
      type: 'select',
      tab: '基本情報',
      enumOptions: allowanceDetailViewTypeOptions,
    },

    { key: 'ruleName', label: 'Rule名', type: 'text', tab: '計算設定' },
    { key: 'defaultAmount', label: '初期金額', type: 'number', tab: '計算設定' },
    { key: 'allowManualInput', label: '手入力許可', type: 'checkbox', tab: '計算設定' },
    { key: 'minAmount', label: '下限金額', type: 'number', tab: '計算設定' },
    { key: 'maxAmount', label: '上限金額', type: 'number', tab: '計算設定' },

    { key: 'taxable', label: '課税対象', type: 'checkbox', tab: '表示設定' },
    { key: 'showOnDailyStatement', label: '日払い明細に表示', type: 'checkbox', tab: '表示設定' },
    { key: 'showOnMonthlyStatement', label: '月次明細に表示', type: 'checkbox', tab: '表示設定' },
    { key: 'displayOrder', label: '表示順', type: 'number', tab: '表示設定' },
    { key: 'enabled', label: '有効', type: 'checkbox', tab: '表示設定' },
    { key: 'note', label: '備考', type: 'text', tab: '表示設定' },
  ])

  return {
    tabs: allowanceFormTabs,
    fields,
  }
}