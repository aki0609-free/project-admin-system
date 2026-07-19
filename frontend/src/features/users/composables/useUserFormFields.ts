import { computed, type Ref } from 'vue'
import type { GridFormFieldDef } from '@/shared/components/form/grid_based_form/types/types'
import type { RoleOption, UserEditForm } from '@/features/users/types/types'

export const useUserFormFields = (
    roleOptions: Ref<RoleOption[]>,
    isEdit: Ref<boolean>
) => {
  const fields = computed<GridFormFieldDef<UserEditForm>[]>(() => [
    {
      key: 'username',
      label: 'ユーザー名',
      type: 'text',
      gridColumn: '1 / span 2',
      required: true,
    },
    {
      key: 'password',
      label: isEdit.value ? 'パスワード（変更時のみ入力）' : 'パスワード',
      type: 'password',
      gridColumn: '3 / span 2',
      required: !isEdit.value,
    },
    {
      key: 'enabled',
      label: '有効',
      type: 'checkbox',
    },
    {
      key: 'roles',
      label: 'ロール',
      type: 'selectboxWithChips',
      options: roleOptions.value,
      gridColumn: '1 / span 4',
      required: true,
    },
  ])

  return {
    fields,
  }
}