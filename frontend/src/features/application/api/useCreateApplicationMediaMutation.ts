import { useQueryClient } from '@tanstack/vue-query'
import { useAppMutation } from '@/shared/api/useAppMutation'
import { post } from '@/shared/api/http'
import { queryKeys } from '@/features/application/api/queryKeys'
import type { ApplicationMediaCreateRequest } from '@/features/application/types/types'

export const useCreateApplicationMediaMutation = () => {
  const queryClient = useQueryClient()

  return useAppMutation({
    mutationFn: (payload: ApplicationMediaCreateRequest) =>
      post<number, ApplicationMediaCreateRequest>('/api/application-media', payload),

    onSuccess: async () => {
      await queryClient.invalidateQueries({
        queryKey: queryKeys.applicationMedias.all,
      })
    },
  })
}