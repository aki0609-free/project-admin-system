import { computed } from 'vue'
import type { SimpleTableColumnDef } from '@/shared/components/table/simple_table/types/item/types'
import { createSimpleTableFilterRules } from '@/shared/components/table/simple_table/utils/createSimpleTableFilterRules'
import type { AllowanceDetailTableRow } from '@/features/master/allowance/types/allowanceTypes'
import { formatCurrency } from '@/shared/utils/CurrencyUtils'

export const useGenericAllowanceDetailConfig = () => {
  const columns = computed<SimpleTableColumnDef<AllowanceDetailTableRow>[]>(() => [
    {
      title: '削除',
      key: 'deleteSelected',
      width: '80px',
      type: 'checkbox',
      editable: true,
      filter: { type: 'checkbox' },
    },
    {
      title: '初期金額',
      key: 'defaultAmount',
      width: '140px',
      type: 'text',
      editable: true,
      filter: { type: 'text' },
      formatter: value => formatCurrency(value as number),
    },
    {
      title: '金額区分',
      key: 'amountType',
      width: '120px',
      type: 'text',
      editable: true,
      filter: { type: 'text' },
    },
    {
      title: '手入力可',
      key: 'allowManualInput',
      width: '120px',
      type: 'checkbox',
      editable: true,
      filter: { type: 'checkbox' },
    },
    {
      title: '下限金額',
      key: 'minAmount',
      width: '140px',
      type: 'text',
      editable: true,
      filter: { type: 'text' },
      formatter: value => formatCurrency(value as number),
    },
    {
      title: '上限金額',
      key: 'maxAmount',
      width: '140px',
      type: 'text',
      editable: true,
      filter: { type: 'text' },
      formatter: value => formatCurrency(value as number),
    },
    {
      title: '対象範囲',
      key: 'targetScope',
      width: '140px',
      type: 'text',
      editable: true,
      filter: { type: 'text' },
    },
    {
      title: '表示順',
      key: 'displayOrder',
      width: '100px',
      type: 'text',
      editable: true,
      filter: { type: 'text' },
    },
    {
      title: '有効',
      key: 'enabled',
      width: '100px',
      type: 'checkbox',
      editable: true,
      filter: { type: 'checkbox' },
    },
    {
      title: '備考',
      key: 'note',
      width: '240px',
      type: 'text',
      editable: true,
      filter: { type: 'text' },
    },
  ])

  const filterRules = computed(() =>
    createSimpleTableFilterRules<AllowanceDetailTableRow>(columns.value),
  )

  return {
    rows: computed<AllowanceDetailTableRow[]>(() => []),
    columns,
    filterRules,
    title: '一般手当設定',
    description: '勤務態度手当・運転手当・管理手当・その他手当などの一般手当設定です。',
  }
}