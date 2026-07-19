import { useQueryClient } from '@tanstack/vue-query'

import { put } from '@/shared/api/http'
import { useAppMutation } from '@/shared/api/useAppMutation'

import { queryKeys } from './queryKeys'

import type {
  NoticeCreateRequest,
  NoticeResponse,
} from '../types/dashboardTypes'

type UpdateNoticePayload = {
  id: number
  body: NoticeCreateRequest
}

export const useUpdateNoticeMutation = () => {
  const queryClient = useQueryClient()

  return useAppMutation({
    mutationFn: async ({ id, body }: UpdateNoticePayload) =>
      await put<NoticeResponse>(`/api/notices/${id}`, body),

    onSuccess: async () => {
      await queryClient.invalidateQueries({
        queryKey: queryKeys.notice.all,
      })
    },
  })
}