import { computed, type Ref } from 'vue'
import type { SimpleTableColumnDef } from '@/shared/components/table/simple_table/types/item/types'
import type { RoleOption, UserListItem } from '@/features/users/types/types'

export const useUserColumns = (roleOptions: Ref<RoleOption[]>) => {
  const columns = computed<SimpleTableColumnDef<UserListItem>[]>(() => [
    {
      key: 'id',
      title: 'ID',
      width: '155px',
      filter: { type: 'text' }
    },
    {
      key: 'username',
      title: 'ユーザー名',
      width: '155px',
      filter: { type: 'text' }
    },
    {
      key: 'enabled',
      title: '有効',
      type: 'checkbox',
      width: '152px',
      filter: { type: 'checkbox' },
      formatter: v => (v ? '有効' : '無効')
    },
    {
      key: 'roles',
      title: '役割',
      filter: {
        type: 'select',
        predicate: (row, value) => row.roles.includes(value as string)
      },
      enumOptions: roleOptions.value,
    },
  ])

  return {
    columns,
  }
}