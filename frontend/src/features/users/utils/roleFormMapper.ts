import type {
  RoleCreateForm,
  RoleDetail,
  RoleEditForm,
  RoleUpdateForm,
} from '@/features/users/types/types'

export const toRoleEditForm = (role: RoleDetail): RoleEditForm => ({
  id: role.id,
  name: role.name,
  permissionIds: role.permissions.map(p => p.id),
})

export const toRoleCreatePayload = (form: RoleEditForm): RoleCreateForm => ({
  name: form.name,
  permissionIds: [...form.permissionIds],
})

export const toRoleUpdatePayload = (form: RoleEditForm): RoleUpdateForm => {
  if (form.id == null) {
    throw new Error('更新対象の id がありません。')
  }

  return {
    id: form.id,
    name: form.name,
    permissionIds: [...form.permissionIds],
  }
}