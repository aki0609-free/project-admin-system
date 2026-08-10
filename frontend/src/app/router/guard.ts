/* eslint-disable @typescript-eslint/no-explicit-any */
import { usePermission } from '@/shared/auth/composables/usePermission'
import { useAuthStore } from '@/shared/auth/store/useAuthStore'
import type { Router } from 'vue-router'
import type { Role } from '@/shared/auth/types/types'

export function setupAuthGuard(router: Router) {
  router.beforeEach(async (to) => {
    const authStore = useAuthStore()
    const { can } = usePermission()

    if (!authStore.authReady) {
      await authStore.initAuth()
    }

    if (to.meta.requiresAuth && !authStore.isAuthenticated) {
      return '/login'
    }

    if (to.meta.resource && to.meta.action) {
      if (!can(to.meta.resource as any, to.meta.action as string)) {
        return '/forbidden'
      }
    }

    if (
      Array.isArray(to.meta.roles)
      && !to.meta.roles.some(
        role => authStore.user?.roles.includes(role as Role),
      )
    ) {
      return '/forbidden'
    }
  })
}
