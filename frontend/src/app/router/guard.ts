/* eslint-disable @typescript-eslint/no-explicit-any */
import { usePermission } from '@/shared/auth/composables/usePermission'
import { useAuthStore } from '@/shared/auth/store/useAuthStore'
import type { Router } from 'vue-router'

export function setupAuthGuard(router: Router) {
  router.beforeEach((to) => {
    const authStore = useAuthStore()
    const { can } = usePermission()

    if (!authStore.authReady) return

    if (to.meta.requiresAuth && !authStore.isAuthenticated) {
      return '/login'
    }

    if (to.meta.resource && to.meta.action) {
      if (!can(to.meta.resource as any, to.meta.action as string)) {
        return '/forbidden'
      }
    }
  })
}