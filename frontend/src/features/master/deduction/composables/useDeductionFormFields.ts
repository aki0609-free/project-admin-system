import { computed } from 'vue'
import type { TabbedFormFieldDef } from '@/shared/components/form/tabbed_form/types/types'
import type { DeductionMaster } from '@/features/master/deduction/types/deductionTypes'
import {
  deductionCalculationTypeOptions,
  deductionDetailViewTypeOptions,
  deductionTypeOptions,
  deductionUnitOptions,
} from '@/features/master/deduction/constants/deductionConstants'

export const deductionFormTabs = ['基本情報', '計算設定', '表示設定'] as const

export const useDeductionFormFields = () => {
  const fields = computed<TabbedFormFieldDef<DeductionMaster>[]>(() => [
    { key: 'code', label: '控除コード', type: 'text', tab: '基本情報' },
    { key: 'name', label: '控除名', type: 'text', tab: '基本情報' },

    {
      key: 'deductionType',
      label: '控除種別',
      type: 'select',
      tab: '基本情報',
      options: [...deductionTypeOptions],
    },
    {
      key: 'calculationType',
      label: '計算区分',
      type: 'select',
      tab: '基本情報',
      options: [...deductionCalculationTypeOptions],
    },
    {
      key: 'deductionUnit',
      label: '控除単位',
      type: 'select',
      tab: '基本情報',
      options: [...deductionUnitOptions],
    },
    {
      key: 'detailViewType',
      label: '詳細参照タイプ',
      type: 'select',
      tab: '基本情報',
      options: [...deductionDetailViewTypeOptions],
    },

    { key: 'ruleName', label: 'Rule名', type: 'text', tab: '計算設定' },
    { key: 'defaultAmount', label: '初期金額', type: 'number', tab: '計算設定' },
    { key: 'allowManualInput', label: '手入力許可', type: 'checkbox', tab: '計算設定' },
    { key: 'minAmount', label: '下限金額', type: 'number', tab: '計算設定' },
    { key: 'maxAmount', label: '上限金額', type: 'number', tab: '計算設定' },

    { key: 'showOnDailyStatement', label: '日払い明細に表示', type: 'checkbox', tab: '表示設定' },
    { key: 'showOnMonthlyStatement', label: '月次給与明細に表示', type: 'checkbox', tab: '表示設定' },
    { key: 'carryToMonthlySettlement', label: '月次精算対象', type: 'checkbox', tab: '表示設定' },
    { key: 'displayOrder', label: '表示順', type: 'number', tab: '表示設定' },
    { key: 'enabled', label: '有効', type: 'checkbox', tab: '表示設定' },
    { key: 'note', label: '備考', type: 'text', tab: '表示設定' },
  ])

  return {
    tabs: deductionFormTabs,
    fields,
  }
}