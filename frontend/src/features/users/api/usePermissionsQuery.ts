import { computed } from 'vue'
import { useAppQuery } from '@/shared/api/useAppQuery'
import { get } from '@/shared/api/http'
import { queryKeys } from '@/features/users/api/queryKeys'
import type { Permission } from '@/features/users/types/types'

export const usePermissionsQuery = () => {
  const query = useAppQuery<Permission[]>({
    queryKey: queryKeys.permissions.list(),
    queryFn: () => get<Permission[]>('/api/permissions'),
  })

  const permissions = computed(() => query.data.value ?? [])

  return {
    ...query,
    permissions,
  }
}