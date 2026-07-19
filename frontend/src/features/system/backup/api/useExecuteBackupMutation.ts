import { useQueryClient } from '@tanstack/vue-query'
import { useAppMutation } from '@/shared/api/useAppMutation'
import { postBlob } from '@/shared/api/http'
import { queryKeys } from '@/features/system/backup/api/queryKeys'
import type { BackupExecuteRequest } from '@/features/system/backup/types/backupApiTypes'

export const useExecuteBackupMutation = () => {
  const queryClient = useQueryClient()

  return useAppMutation({
    mutationFn: async (
      request: BackupExecuteRequest,
    ) => {
      return await postBlob<BackupExecuteRequest>(
        '/api/system/backup/execute',
        request,
      )
    },

    onSuccess: async () => {
      await queryClient.invalidateQueries({
        queryKey: queryKeys.backup.histories,
      })
    },
  })
}