import { computed } from 'vue'
import type { SimpleTableColumnDef } from '@/shared/components/table/simple_table/types/item/types'
import type { AllowanceListItem } from '@/features/master/allowance/types/allowanceTypes'
import {
  allowanceCalculationTypeLabelMap,
  allowanceCalculationTypeOptions,
  allowanceTypeLabelMap,
  allowanceTypeOptions,
  allowanceUnitLabelMap,
  allowanceUnitOptions,
} from '@/features/master/allowance/constants/allowanceConstants'

export const useAllowanceColumns = () => {
  const columns = computed<SimpleTableColumnDef<AllowanceListItem>[]>(() => [
    { title: 'ID', key: 'id', width: '80px', filter: { type: 'text' } },
    { title: '手当コード', key: 'code', width: '300px', filter: { type: 'text' } },
    { title: '手当名', key: 'name', width: '200px', filter: { type: 'text' } },

    {
      title: '手当区分',
      key: 'allowanceType',
      width: '200px',
      filter: { type: 'select' },
      enumOptions: [...allowanceTypeOptions],
      formatter: value =>
        allowanceTypeLabelMap[value as keyof typeof allowanceTypeLabelMap] ?? value,
    },
    {
      title: '計算区分',
      key: 'calculationType',
      width: '200px',
      filter: { type: 'select' },
      enumOptions: [...allowanceCalculationTypeOptions],
      formatter: value =>
        allowanceCalculationTypeLabelMap[
          value as keyof typeof allowanceCalculationTypeLabelMap
        ] ?? value,
    },
    {
      title: '支給単位',
      key: 'allowanceUnit',
      width: '200px',
      filter: { type: 'select' },
      enumOptions: [...allowanceUnitOptions],
      formatter: value =>
        allowanceUnitLabelMap[value as keyof typeof allowanceUnitLabelMap] ?? value,
    },

    { title: 'Rule名', key: 'ruleName', width: '200px', filter: { type: 'text' } },
    { title: '初期金額', key: 'defaultAmount', width: '200px', filter: { type: 'text' } },
    {
      title: '手入力',
      key: 'allowManualInput',
      width: '200px',
      type: 'checkbox',
      filter: { type: 'checkbox' },
    },
    { title: '下限金額', key: 'minAmount', width: '200px', filter: { type: 'text' } },
    { title: '上限金額', key: 'maxAmount', width: '200px', filter: { type: 'text' } },

    { title: '課税', key: 'taxable', width: '200px', type: 'checkbox', filter: { type: 'checkbox' } },
    {
      title: '日払い表示',
      key: 'showOnDailyStatement',
      width: '200px',
      type: 'checkbox',
      filter: { type: 'checkbox' },
    },
    {
      title: '月次表示',
      key: 'showOnMonthlyStatement',
      width: '200px',
      type: 'checkbox',
      filter: { type: 'checkbox' },
    },
    { title: '表示順', key: 'displayOrder', width: '100px', filter: { type: 'text' } },
    { title: '有効', key: 'enabled', width: '100px', type: 'checkbox', filter: { type: 'checkbox' } },
    { title: '備考', key: 'note', width: '300px', filter: { type: 'text' } },
  ])

  return { columns }
}