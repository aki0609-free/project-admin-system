import { useQueryClient } from '@tanstack/vue-query'
import { useAppMutation } from '@/shared/api/useAppMutation'
import { post } from '@/shared/api/http'
import { queryKeys } from '@/features/system/backup/api/queryKeys'
import type {
  BackupTargetResponse,
  BackupTargetSaveRequest,
} from '@/features/system/backup/types/backupApiTypes'

export const useCreateBackupTargetMutation = () => {
  const queryClient = useQueryClient()

  return useAppMutation({
    mutationFn: async (request: BackupTargetSaveRequest) =>
      await post<BackupTargetResponse, BackupTargetSaveRequest>(
        '/api/system/backup/targets',
        request,
      ),

    onSuccess: async () => {
      await queryClient.invalidateQueries({
        queryKey: queryKeys.backup.all,
      })
    },
  })
}