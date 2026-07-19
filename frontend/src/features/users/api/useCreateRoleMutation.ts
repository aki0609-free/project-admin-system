import { useQueryClient } from '@tanstack/vue-query'
import { useAppMutation } from '@/shared/api/useAppMutation'
import { post } from '@/shared/api/http'
import { queryKeys } from '@/features/users/api/queryKeys'
import type { RoleCreateForm } from '../types/types' 

export const useCreateRoleMutation = () => {
  const queryClient = useQueryClient()

  return useAppMutation({
    mutationFn: (payload: RoleCreateForm) =>
      post<void, RoleCreateForm>('/api/roles', payload),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: queryKeys.roles.all })
    },
  })
}