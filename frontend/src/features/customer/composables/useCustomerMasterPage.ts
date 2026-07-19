import { computed } from 'vue'
import { createSimpleTableFilterRules } from '@/shared/components/table/simple_table/utils/createSimpleTableFilterRules'
import type { ToolbarItem } from '@/shared/components/toolbar/types/types'
import { useCustomersQuery } from '../api/useCustomersQuery'
import { toCustomerListItem } from '../mapper/customerMapper'
import type { CustomerListItem } from '../types/customerTypes'
import { useCustomerColumns } from './useCustomerColumns'
import { useCustomerEditDialog } from './useCustomerEditDialog'
import { useEnvelopePrintDialog } from './useEnvelopePrintDialog'

export const useCustomerMasterPage = () => {
  const { columns } = useCustomerColumns()
  const customersQuery = useCustomersQuery()

  const items = computed<CustomerListItem[]>(() =>
    customersQuery.customers.value.map(toCustomerListItem),
  )

  const filterRules = computed(() =>
    createSimpleTableFilterRules<CustomerListItem>(columns.value),
  )

  const editDialog = useCustomerEditDialog()
  const envelopePrint = useEnvelopePrintDialog(items)

  const toolbarItems = computed<ToolbarItem[]>(() => [
    {
      type: 'button',
      label: '新規登録',
      color: 'primary',
      onClick: editDialog.openCreate,
    },
    {
      type: 'button',
      label: '封筒宛名印刷',
      color: 'secondary',
      onClick: envelopePrint.openEnvelopePrint,
    },
  ])

  return {
    columns,
    customersQuery,
    items,
    filterRules,
    toolbarItems,
    editDialog,
    envelopePrint,
  }
}