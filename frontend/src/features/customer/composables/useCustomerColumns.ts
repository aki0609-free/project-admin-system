import { computed } from 'vue'
import type { SimpleTableColumnDef } from '@/shared/components/table/simple_table/types/item/types'
import type { CustomerListItem } from '../types/customerTypes'
import { formatDayRule } from '@/shared/utils/DayRuleUtils'

export const useCustomerColumns = () => {
  const columns = computed(() => {
    const defs: SimpleTableColumnDef<CustomerListItem>[] = [
      { title: 'ID', key: 'id', width: '100px', filter: { type: 'text' } },
      { title: '顧客名', key: 'name', width: '220px', filter: { type: 'text' } },
      { title: 'ふりがな', key: 'furiganaName', width: '180px', filter: { type: 'text' } },
      { title: '短縮社名', key: 'shortName', width: '140px', filter: { type: 'text' } },
      { title: '住所', key: 'address', width: '260px', filter: { type: 'text' } },
      { title: '代表者名', key: 'representativeName', width: '160px', filter: { type: 'text' } },
      { title: '電話番号', key: 'phone', width: '160px', filter: { type: 'text' } },
      { title: '職種', key: 'jobType', width: '140px', filter: { type: 'text' } },
      { title: '契約有無', key: 'contractFlag', width: '120px', filter: { type: 'text' } },
      {
        title: '締日',
        key: 'closingDayRule',
        width: '100px',
        filter: { type: 'text' },
        formatter: value => formatDayRule(value),
      },
      {
        title: '支払日',
        key: 'paymentDayRule',
        width: '100px',
        filter: { type: 'text' },
        formatter: value => formatDayRule(value),
      },
      { title: '現場数', key: 'siteCount', width: '100px', filter: { type: 'text' } },
      { title: '社員数', key: 'employeeCount', width: '100px', filter: { type: 'text' } },
      { title: '最新入金', key: 'latestPaymentStatus', width: '120px', filter: { type: 'text' } },
    ]

    return defs
  })

  return { columns }
}