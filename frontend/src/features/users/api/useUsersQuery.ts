import { useAppQuery } from '@/shared/api/useAppQuery'
import { get } from '@/shared/api/http' 
import { queryKeys } from '@/features/users/api/queryKeys'
import type { UserListItem } from '@/features/users/types/types'

export const useUsersQuery = () => {
  return useAppQuery({
    queryKey: queryKeys.users.list(),
    queryFn: () => get<UserListItem[]>('/api/users'),
  })
}