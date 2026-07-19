import { computed, type ComputedRef, type Ref, unref } from 'vue'
import type { RoleOption, RoleDetail } from '@/features/users/types/types'

type RoleSource = Ref<RoleDetail[]> | ComputedRef<RoleDetail[]>

export const useRoleOptions = (roles: RoleSource) => {
  const roleOptions = computed<RoleOption[]>(() =>
    unref(roles).map(role => ({
      title: role.name,
      value: role.name,
    }))
  )

  const roleLabelMap = computed<Record<string, string>>(() =>
    Object.fromEntries(
      unref(roles).map(role => [role.name, role.name])
    )
  )

  return {
    roleOptions,
    roleLabelMap,
  }
}