import { computed, ComputedRef } from 'vue'
import type { SimpleTableColumnDef } from '@/shared/components/table/simple_table/types/item/types'
import type { RoleDetail } from '@/features/users/types/types'
import { SelectOption } from '@/shared/components/table/simple_table/types/filter/types'

export const useRoleColumns = (permissionOptions:  ComputedRef<SelectOption[]>) => {
  const columns = computed<SimpleTableColumnDef<RoleDetail>[]>(() => [
    {
      title: '役割',
      key: 'name',
      width: '170px',
      filter: { type: 'text' },
    },
    {
      title: '権限',
      key: 'permissions',
      filter: {
        type: 'select',
        multiple: true
      },
      enumOptions: permissionOptions.value
    },
  ])

  return {
    columns,
  }
}