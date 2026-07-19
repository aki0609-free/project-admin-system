// src/features/application/api/useSaveApplicationMediaBulkMutation.ts
import { useQueryClient } from '@tanstack/vue-query'
import { useAppMutation } from '@/shared/api/useAppMutation'
import { postApplicationMediaBulkSave } from '@/features/application/api/postApplicationMediaBulkSave' 
import { queryKeys } from '@/features/application/api/queryKeys'
import type { ApplicationMediaBulkSaveRequest } from '@/features/application/types/types'

export const useSaveApplicationMediaBulkMutation = () => {
  const queryClient = useQueryClient()

  return useAppMutation({
    mutationFn: (payload: ApplicationMediaBulkSaveRequest) =>
      postApplicationMediaBulkSave(payload),

    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({
          queryKey: queryKeys.applicationMedias.all,
        }),
      ])
    },
  })
}