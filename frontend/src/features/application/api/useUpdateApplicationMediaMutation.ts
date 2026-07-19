import { useQueryClient } from '@tanstack/vue-query'
import { useAppMutation } from '@/shared/api/useAppMutation'
import { put } from '@/shared/api/http'
import { queryKeys } from '@/features/application/api/queryKeys'
import type { ApplicationMediaUpdateRequest } from '@/features/application/types/types'

export const useUpdateApplicationMediaMutation = () => {
  const queryClient = useQueryClient()

  return useAppMutation({
    mutationFn: (payload: ApplicationMediaUpdateRequest) => {
      const { id, ...body } = payload

      return put<void, typeof body>('/api/application-media/{id}', body, {
        params: {
          path: { id },
        },
      })
    },

    onSuccess: async (_: any, payload: any) => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: queryKeys.applicationMedias.all }),
        queryClient.invalidateQueries({ queryKey: queryKeys.applicationMedias.detail(payload.id) }),
      ])
    },
  })
}