import type {
  UserCreateForm,
  UserEditForm,
  UserUpdateForm,
} from '@/features/users/types/types'

export const toUserCreatePayload = (
  form: UserEditForm,
): UserCreateForm => ({
  username: form.username,
  password: form.password,
  enabled: form.enabled,
  roles: [...form.roles],
})

export const toUserUpdatePayload = (
  form: UserEditForm,
): UserUpdateForm => {
  if (form.id == null) {
    throw new Error('更新対象の id がありません。')
  }

  return {
    id: form.id,
    username: form.username,
    enabled: form.enabled,
    roles: [...form.roles],
    ...(form.password.trim() ? { password: form.password } : {}),
  }
}