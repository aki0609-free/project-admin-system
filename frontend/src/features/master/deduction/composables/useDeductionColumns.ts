import { computed } from 'vue'
import type { SimpleTableColumnDef } from '@/shared/components/table/simple_table/types/item/types'
import type { DeductionListItem } from '@/features/master/deduction/types/deductionTypes'
import {
  deductionCalculationTypeLabelMap,
  deductionCalculationTypeOptions,
  deductionDetailViewTypeLabelMap,
  deductionDetailViewTypeOptions,
  deductionTypeLabelMap,
  deductionTypeOptions,
  deductionUnitLabelMap,
  deductionUnitOptions,
} from '@/features/master/deduction/constants/deductionConstants'

export const useDeductionColumns = () => {
  const columns = computed<SimpleTableColumnDef<DeductionListItem>[]>(() => [
    { title: 'ID', key: 'id', width: '80px', filter: { type: 'text' } },
    { title: '控除コード', key: 'code', width: '300px', filter: { type: 'text' } },
    { title: '控除名', key: 'name', width: '200px', filter: { type: 'text' } },

    {
      title: '控除種別',
      key: 'deductionType',
      width: '200px',
      filter: { type: 'select' },
      enumOptions: [...deductionTypeOptions],
      formatter: value =>
        deductionTypeLabelMap[value as keyof typeof deductionTypeLabelMap] ?? value,
    },
    {
      title: '計算区分',
      key: 'calculationType',
      width: '200px',
      filter: { type: 'select' },
      enumOptions: [...deductionCalculationTypeOptions],
      formatter: value =>
        deductionCalculationTypeLabelMap[
          value as keyof typeof deductionCalculationTypeLabelMap
        ] ?? value,
    },
    {
      title: '控除単位',
      key: 'deductionUnit',
      width: '200px',
      filter: { type: 'select' },
      enumOptions: [...deductionUnitOptions],
      formatter: value =>
        deductionUnitLabelMap[value as keyof typeof deductionUnitLabelMap] ?? value,
    },
    {
      title: '詳細種別',
      key: 'detailViewType',
      width: '200px',
      filter: { type: 'select' },
      enumOptions: [...deductionDetailViewTypeOptions],
      formatter: value =>
        deductionDetailViewTypeLabelMap[
          value as keyof typeof deductionDetailViewTypeLabelMap
        ] ?? value,
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

    {
      title: '日払い明細',
      key: 'showOnDailyStatement',
      width: '200px',
      type: 'checkbox',
      filter: { type: 'checkbox' },
    },
    {
      title: '月次明細',
      key: 'showOnMonthlyStatement',
      width: '200px',
      type: 'checkbox',
      filter: { type: 'checkbox' },
    },
    {
      title: '月次精算',
      key: 'carryToMonthlySettlement',
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