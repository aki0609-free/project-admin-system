import { computed, type Ref } from 'vue'
import type { TabbedFormFieldDef } from '@/shared/components/form/tabbed_form/types/types'
import type { AllowanceMaster } from '@/features/master/allowance/types/allowanceTypes'
import {
  allowanceCalculationTypeOptions,
  allowanceDetailViewTypeOptions,
  allowanceTypeOptions,
  allowanceUnitOptions,
} from '@/features/master/allowance/constants/allowanceConstants'

export const allowanceFormTabs = ['基本情報', '計算設定', '表示設定'] as const

type AllowanceFormFieldOptions = {
  isCreateMode: Ref<boolean>
  canManage: Ref<boolean>
  ruleOptions: Ref<{ title: string; value: string }[]>
}

export const useAllowanceFormFields = (formOptions: AllowanceFormFieldOptions) => {
  const fields = computed<TabbedFormFieldDef<AllowanceMaster>[]>(() => [
    {
      key: 'code',
      label: '手当コード',
      type: 'text',
      tab: '基本情報',
      editable: formOptions.canManage.value && formOptions.isCreateMode.value,
    },
    { key: 'name', label: '手当名', type: 'text', tab: '基本情報', editable: formOptions.canManage.value },

    {
      key: 'allowanceType',
      label: '手当区分',
      type: 'select',
      tab: '基本情報',
      options: allowanceTypeOptions,
      editable: formOptions.canManage.value,
    },
    {
      key: 'calculationType',
      label: '計算区分',
      type: 'select',
      tab: '基本情報',
      options: allowanceCalculationTypeOptions,
      editable: formOptions.canManage.value,
    },
    {
      key: 'allowanceUnit',
      label: '支給単位',
      type: 'select',
      tab: '基本情報',
      options: allowanceUnitOptions,
      editable: formOptions.canManage.value,
    },
    {
      key: 'detailViewType',
      label: '詳細種類',
      type: 'select',
      tab: '基本情報',
      options: allowanceDetailViewTypeOptions,
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
      label: '手入力許可',
      type: 'checkbox',
      tab: '計算設定',
      editable: formOptions.canManage.value,
      visible: model => model.calculationType === 'MANUAL',
    },
    { key: 'minAmount', label: '下限金額', type: 'number', tab: '計算設定', editable: formOptions.canManage.value },
    { key: 'maxAmount', label: '上限金額', type: 'number', tab: '計算設定', editable: formOptions.canManage.value },

    { key: 'taxable', label: '課税対象', type: 'checkbox', tab: '表示設定', editable: formOptions.canManage.value },
    { key: 'showOnDailyStatement', label: '日払い明細に表示', type: 'checkbox', tab: '表示設定', editable: formOptions.canManage.value },
    { key: 'showOnMonthlyStatement', label: '月次明細に表示', type: 'checkbox', tab: '表示設定', editable: formOptions.canManage.value },
    { key: 'displayOrder', label: '表示順', type: 'number', tab: '表示設定', editable: formOptions.canManage.value },
    { key: 'enabled', label: '有効', type: 'checkbox', tab: '表示設定', editable: formOptions.canManage.value },
    { key: 'note', label: '備考', type: 'text', tab: '表示設定', editable: formOptions.canManage.value },
  ])

  return {
    tabs: allowanceFormTabs,
    fields,
  }
}
