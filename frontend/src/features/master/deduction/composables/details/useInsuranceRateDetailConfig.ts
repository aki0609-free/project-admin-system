import { computed } from 'vue'
import type { SimpleTableColumnDef } from '@/shared/components/table/simple_table/types/item/types'
import { createSimpleTableFilterRules } from '@/shared/components/table/simple_table/utils/createSimpleTableFilterRules'
import type { DeductionDetailTableRow } from '@/features/master/deduction/types/deductionTypes'

export const useInsuranceRateDetailConfig = () => {
  const columns = computed<SimpleTableColumnDef<DeductionDetailTableRow>[]>(() => [
    { title: 'ID', key: 'id', width: '200px', filter: { type: 'text' } },
    { title: '保険種別', key: 'insuranceType', width: '200px', filter: { type: 'text' } },
    { title: '年度', key: 'year', width: '200px', filter: { type: 'text' } },
    { title: '従業員負担率', key: 'employeeRate', width: '200px', filter: { type: 'text' } },
    { title: '会社負担率', key: 'employerRate', width: '200px', filter: { type: 'text' } },
    { title: '表示名', key: 'label', width: '300px', filter: { type: 'text' } },
  ])

  const filterRules = computed(() =>
    createSimpleTableFilterRules<DeductionDetailTableRow>(columns.value),
  )

  return {
    title: '保険料率詳細',
    description: '健康保険・雇用保険などの保険料率マスターを参照します。',
    rows: computed<DeductionDetailTableRow[]>(() => []),
    columns,
    filterRules,
  }
}