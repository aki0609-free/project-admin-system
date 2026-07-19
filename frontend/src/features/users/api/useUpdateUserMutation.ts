import { useQueryClient } from '@tanstack/vue-query'
import { useAppMutation } from '@/shared/api/useAppMutation'
import { put } from '@/shared/api/http'
import { queryKeys } from '@/features/users/api/queryKeys'
import type { UserUpdateForm } from '@/features/users/types/types'

export const useUpdateUserMutation = () => {
  const queryClient = useQueryClient()

  return useAppMutation({
    mutationFn: (payload: UserUpdateForm) => {
      const { id, ...body } = payload

      return put<void, typeof body>('/api/users/{id}', body, {
        params: {
          path: { id },
        },
      })
    },

    onSuccess: async (_: any, payload: any) => {
      await Promise.all([
        queryClient.invalidateQueries({
          queryKey: queryKeys.users.all,
        }),
        queryClient.invalidateQueries({
          queryKey: queryKeys.users.detail(String(payload.id)),
        }),
      ])
    },
  })
}