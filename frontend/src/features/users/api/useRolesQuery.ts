import { useAppQuery } from '@/shared/api/useAppQuery'
import { get } from '@/shared/api/http'
import { queryKeys } from '@/features/users/api/queryKeys'
import type { RoleDetail } from '@/features/users/types/types'

export const useRolesQuery = () => {
  return useAppQuery({
    queryKey: queryKeys.roles.list(),
    queryFn: () => get<RoleDetail[]>('/api/roles'),
  })
}