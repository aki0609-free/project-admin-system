import { useQueryClient } from '@tanstack/vue-query'
import { useAppMutation } from '@/shared/api/useAppMutation'
import { put } from '@/shared/api/http'
import { queryKeys } from '@/features/system/backup/api/queryKeys'
import type {
  BackupTargetResponse,
  BackupTargetSaveRequest,
} from '@/features/system/backup/types/backupApiTypes'

type Payload = {
  id: number
  request: BackupTargetSaveRequest
}

export const useUpdateBackupTargetMutation = () => {
  const queryClient = useQueryClient()

  return useAppMutation({
    mutationFn: async ({ id, request }: Payload) =>
      await put<BackupTargetResponse, BackupTargetSaveRequest>(
        `/api/system/backup/targets/${id}`,
        request,
      ),

    onSuccess: async (_data: any, variables: any) => {
      await queryClient.invalidateQueries({
        queryKey: queryKeys.backup.all,
      })
      await queryClient.invalidateQueries({
        queryKey: queryKeys.backup.detail(variables.id),
      })
    },
  })
}