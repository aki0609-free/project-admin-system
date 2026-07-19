import type { GridFormFieldDef } from '@/shared/components/form/grid_based_form/types/types'
import type { RoleEditForm } from '@/features/users/types/types'

export const roleFormFields: GridFormFieldDef<RoleEditForm>[] = [
  {
    key: 'name',
    label: 'ロール名',
    type: 'text',
    gridColumn: '1 / span 4',
    required: true,
  },
]