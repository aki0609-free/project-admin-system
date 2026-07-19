import { computed } from 'vue'
import type { SimpleTableColumnDef } from '@/shared/components/table/simple_table/types/item/types'
import type { CustomerSite } from '../types/customerTypes'


export const useCustomerSiteColumns = () => {
  const columns = computed<SimpleTableColumnDef<CustomerSite>[]>(() => [
    {
      title: '削除',
      key: 'deleteSelected',
      width: '85px',
      type: 'checkbox',
      editable: true,
      filter: { type: 'checkbox' },
    },
    {
      title: 'ID',
      key: 'id',
      width: '100px',
      filter: { type: 'text' },
    },
    {
      title: '現場名',
      key: 'name',
      width: '220px',
      filter: { type: 'text' },
      editable: true,
      type: 'text',
    },
    {
      title: '担当者',
      key: 'contactPersonName',
      width: '180px',
      filter: { type: 'text' },
      editable: true,
      type: 'text',
    },
    {
      title: '担当者電話番号',
      key: 'contactPersonPhone',
      width: '180px',
      filter: { type: 'text' },
      editable: true,
      type: 'text',
    },
    {
      title: '担当者メール',
      key: 'contactPersonEmail',
      width: '240px',
      filter: { type: 'text' },
      editable: true,
      type: 'text',
    },
    {
      title: '会社からの距離',
      key: 'distanceFromCompanyKm',
      width: '160px',
      filter: { type: 'text' },
      editable: true,
      type: 'number',
      formatter: value =>
        value == null || value === ''
          ? ''
          : `${value}km`,
    },
  ])

  return { columns }
}