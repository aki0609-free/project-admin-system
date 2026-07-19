import { useQueryClient } from '@tanstack/vue-query'
import { useAppMutation } from '@/shared/api/useAppMutation'
import { post } from '@/shared/api/http'
import { queryKeys } from '@/features/users/api/queryKeys'
import type { UserCreateForm } from '@/features/users/types/types'

export const useCreateUserMutation = () => {
  const queryClient = useQueryClient()

  return useAppMutation({
    mutationFn: (payload: UserCreateForm) =>
      post<void, UserCreateForm>('/api/users', payload),

    onSuccess: async () => {
      await queryClient.invalidateQueries({
        queryKey: queryKeys.users.all,
      })
    },
  })
}