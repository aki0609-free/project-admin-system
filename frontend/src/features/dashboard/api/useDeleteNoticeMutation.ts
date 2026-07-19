import { useQueryClient } from '@tanstack/vue-query'

import { del } from '@/shared/api/http'
import { useAppMutation } from '@/shared/api/useAppMutation'

import { queryKeys } from './queryKeys'

export const useDeleteNoticeMutation = () => {
  const queryClient = useQueryClient()

  return useAppMutation({
    mutationFn: async (id: number) =>
      await del<void>(`/api/notices/${id}`),

    onSuccess: async () => {
      await queryClient.invalidateQueries({
        queryKey: queryKeys.notice.all,
      })
    },
  })
}