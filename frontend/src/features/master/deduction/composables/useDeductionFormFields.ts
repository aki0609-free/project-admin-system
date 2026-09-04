import { computed, type Ref } from 'vue'
import type { TabbedFormFieldDef } from '@/shared/components/form/tabbed_form/types/types'
import type { DeductionMaster } from '@/features/master/deduction/types/deductionTypes'
import {
  deductionCalculationTypeOptions,
  deductionDetailViewTypeOptions,
  deductionTypeOptions,
  deductionUnitOptions,
} from '@/features/master/deduction/constants/deductionConstants'

export const deductionFormTabs = ['基本情報', '計算設定', '表示設定'] as const

type DeductionFormFieldOptions = {
  isCreateMode: Ref<boolean>
  canManage: Ref<boolean>
  ruleOptions: Ref<{ title: string; value: string }[]>
}

export const useDeductionFormFields = (formOptions: DeductionFormFieldOptions) => {
  const fields = computed<TabbedFormFieldDef<DeductionMaster>[]>(() => [
    {
      key: 'code',
      label: '控除コード',
      type: 'text',
      tab: '基本情報',
      editable: formOptions.canManage.value && formOptions.isCreateMode.value,
    },
    { key: 'name', label: '控除名', type: 'text', tab: '基本情報', editable: formOptions.canManage.value },

    {
      key: 'deductionType',
      label: '控除種別',
      type: 'select',
      tab: '基本情報',
      options: [...deductionTypeOptions],
      editable: formOptions.canManage.value,
    },
    {
      key: 'calculationType',
      label: '計算区分',
      type: 'select',
      tab: '基本情報',
      options: [...deductionCalculationTypeOptions],
      editable: formOptions.canManage.value,
    },
    {
      key: 'deductionUnit',
      label: '控除単位',
      type: 'select',
      tab: '基本情報',
      options: [...deductionUnitOptions],
      editable: formOptions.canManage.value,
    },
    {
      key: 'detailViewType',
      label: '詳細参照タイプ',
      type: 'select',
      tab: '基本情報',
      options: [...deductionDetailViewTypeOptions],
      editable: formOptions.canManage.value,
    },

    {
      key: 'ruleName',
      label: 'Rule',
      type: 'select',
      tab: '計算設定',
      options: formOptions.ruleOptions.value,
      editable: formOptions.canManage.value,
      visible: model => model.calculationType === 'AUTO',
    },
    {
      key: 'defaultAmount',
      label: '固定金額',
      type: 'number',
      tab: '計算設定',
      editable: formOptions.canManage.value,
      visible: model => model.calculationType === 'FIXED',
    },
    {
      key: 'allowManualInput',
      label: '計算結果の手動変更を許可',
      type: 'checkbox',
      tab: '計算設定',
      editable: formOptions.canManage.value,
      visible: model => model.calculationType === 'AUTO' || model.calculationType === 'FIXED',
    },
    { key: 'minAmount', label: '下限金額', type: 'number', tab: '計算設定', editable: formOptions.canManage.value },
    { key: 'maxAmount', label: '上限金額', type: 'number', tab: '計算設定', editable: formOptions.canManage.value },

    { key: 'showOnDailyStatement', label: '日報に表示', type: 'checkbox', tab: '表示設定', editable: formOptions.canManage.value },
    { key: 'displayOrder', label: '表示順', type: 'number', tab: '表示設定', editable: formOptions.canManage.value },
    { key: 'enabled', label: '有効', type: 'checkbox', tab: '表示設定', editable: formOptions.canManage.value },
    { key: 'note', label: '備考', type: 'text', tab: '表示設定', editable: formOptions.canManage.value },
  ])

  return {
    tabs: deductionFormTabs,
    fields,
  }
}
