import { ref } from 'vue'
import type { UserEditForm, UserListItem } from '@/features/users/types/types'

const createEmptyUserForm = (): UserEditForm => ({
  id: 0,
  username: '',
  password: '',
  enabled: true,
  roles: [],
})

export const useUserForm = () => {
  const dialog = ref(false)
  const isEdit = ref(false)
  const form = ref<UserEditForm>(createEmptyUserForm())

  const openCreate = () => {
    isEdit.value = false
    form.value = createEmptyUserForm()
    dialog.value = true
  }

  const openEdit = (row: UserListItem) => {
    isEdit.value = true
    form.value = {
      id: row.id,
      username: row.username,
      password: '',
      enabled: row.enabled,
      roles: [...row.roles],
    }
    dialog.value = true
  }

  const closeDialog = () => {
    dialog.value = false
    form.value = createEmptyUserForm()
  }

  return {
    dialog,
    isEdit,
    form,
    openCreate,
    openEdit,
    closeDialog,
  }
}
