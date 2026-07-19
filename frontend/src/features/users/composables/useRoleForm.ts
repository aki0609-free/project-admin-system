import { ref } from 'vue'
import type { RoleDetail, RoleEditForm } from '@/features/users/types/types'
import { toRoleEditForm } from '@/features/users/utils/roleFormMapper'

const createEmptyRoleForm = (): RoleEditForm => ({
  id: null,
  name: '',
  permissionIds: [],
})

export const useRoleForm = () => {
  const dialog = ref(false)
  const form = ref<RoleEditForm>(createEmptyRoleForm())

  const openCreate = () => {
    form.value = createEmptyRoleForm()
    dialog.value = true
  }

  const openEdit = (role: RoleDetail) => {
    form.value = toRoleEditForm(role)
    dialog.value = true
  }

  const closeDialog = () => {
    dialog.value = false
  }

  return {
    dialog,
    form,
    openCreate,
    openEdit,
    closeDialog,
  }
}