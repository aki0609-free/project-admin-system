import { useQueryClient } from '@tanstack/vue-query'
import { useAppMutation } from '@/shared/api/useAppMutation'
import { put } from '@/shared/api/http'
import { queryKeys } from '@/features/users/api/queryKeys'
import type { RoleUpdateForm } from '@/features/users/types/types'

export const useUpdateRoleMutation = () => {
  const queryClient = useQueryClient()

  return useAppMutation({
    mutationFn: (payload: RoleUpdateForm) => {
      const { id, ...body } = payload

      return put<void, typeof body>('/api/roles/{id}', body, {
        params: {
          path: { id },
        },
      })
    },

    onSuccess: async (_: any, payload: any) => {
      await Promise.all([
        queryClient.invalidateQueries({
          queryKey: queryKeys.roles.all,
        }),
        queryClient.invalidateQueries({
          queryKey: queryKeys.roles.detail(String(payload.id)),
        }),
      ])
    },
  })
}