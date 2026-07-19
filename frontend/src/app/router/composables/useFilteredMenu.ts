import { computed } from 'vue'
import { useAuth } from '@/shared/auth/composables/useAuth'
import type { MenuItem } from '@/app/menu/types'
import { usePermission } from '@/shared/auth/composables/usePermission'

export function useFilteredMenu(menu: MenuItem[]) {
  const { can } = usePermission()
  const { isAuthenticated } = useAuth()

  const filterItems = (items: MenuItem[]): MenuItem[] => {
    return items
      .map(item => {
        if (item.children) {
          const filteredChildren = filterItems(item.children)
          if (filteredChildren.length > 0) {
            return { ...item, children: filteredChildren }
          }
          return null
        }

        if (item.resource && item.action) {
          return can(item.resource, item.action) ? item : null
        }

        return item
      })
      .filter(Boolean) as MenuItem[]
  }

  return computed(() => {
    if (!isAuthenticated.value) return []
    return filterItems(menu)
  })
}