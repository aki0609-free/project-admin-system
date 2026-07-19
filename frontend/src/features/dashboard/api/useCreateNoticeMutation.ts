import { useQueryClient } from '@tanstack/vue-query'
import { useAppMutation } from '@/shared/api/useAppMutation'
import { post } from '@/shared/api/http'
import { queryKeys } from './queryKeys'
import { type NoticeCreateRequest, type NoticeResponse } from '../types/dashboardTypes'

export const useCreateNoticeMutation = () => {
  const queryClient = useQueryClient()

  return useAppMutation({
    mutationFn: async (body: NoticeCreateRequest) =>
      await post<NoticeResponse>('/api/notices', body),

    onSuccess: async () => {
      await queryClient.invalidateQueries({
        queryKey: queryKeys.notice.all,
      })
    },
  })
}