import { computed } from 'vue'
import type { SimpleTableColumnDef } from '@/shared/components/table/simple_table/types/item/types'
import type { CustomerEmployee } from '../types/customerTypes'

export const useCustomerEmployeeColumns = () => {
  const columns = computed<SimpleTableColumnDef<CustomerEmployee>[]>(() => [
    {
      title: '削除',
      key: 'deleteSelected',
      width: '83px',
      type: 'checkbox',
      editable: true,
      filter: { type: 'checkbox' },
    },
    { title: 'ID', key: 'id', width: '80px', filter: { type: 'text' } },
    { title: '名前', key: 'name', width: '180px', filter: { type: 'text' }, editable: true, type: 'text' },
    { title: 'ふりがな', key: 'furiganaName', width: '180px', filter: { type: 'text' }, editable: true, type: 'text' },
    { title: '役職', key: 'position', width: '180px', filter: { type: 'text' }, editable: true, type: 'text' },
    { title: '電話番号', key: 'phone', width: '180px', filter: { type: 'text' }, editable: true, type: 'text' },
    { title: 'メールアドレス', key: 'email', width: '200px', filter: { type: 'text' }, editable: true, type: 'text' },
    { title: '請求書送付先', key: 'invoiceToFlag', width: '180px', type: 'checkbox', editable: true, filter: { type: 'checkbox' } },
    { title: '請求書CC', key: 'invoiceCcFlag', width: '180px', type: 'checkbox', editable: true, filter: { type: 'checkbox' } },
  ])

  return { columns }
}