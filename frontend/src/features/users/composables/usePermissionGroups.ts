/* eslint-disable @typescript-eslint/no-non-null-assertion */
import { computed, type Ref } from 'vue'
import type { Permission } from '@/features/users/types/types'

export const usePermissionGroups = (permissions: Ref<Permission[]>) => {
  const groups = computed(() => {
    const map: Record<string, Permission[]> = {}

    permissions.value.forEach(p => {
      const resource = p.name.split(':')[0] ?? 'unknown'

      if (!map[resource!]) map[resource!] = []
      map[resource]!.push(p)
    })

    return Object.entries(map).map(([resource, perms]) => ({
      label: resource, // ← シンプルにそのまま or マッピングするならここ
      permissions: perms,
    }))
  })

  return { groups }
}