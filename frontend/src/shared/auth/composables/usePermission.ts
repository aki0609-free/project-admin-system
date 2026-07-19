import { useAuth } from '@/shared/auth/composables/useAuth'
import { createPermissionKey } from '@/shared/auth/constants/permissions'

export function usePermission() {
  const { user } = useAuth()

  const can = (resource: string, action: string): boolean => {
    if (!user.value) return false

    return user.value.permissions.includes(
      createPermissionKey(resource, action),
    )
  }

  const canPermission = (permission: string): boolean => {
    if (!user.value) return false

    return user.value.permissions.includes(permission)
  }

  return {
    can,
    canPermission,
  }
}