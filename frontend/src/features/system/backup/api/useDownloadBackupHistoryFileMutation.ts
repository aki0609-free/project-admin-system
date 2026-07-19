import { useAppMutation } from '@/shared/api/useAppMutation'
import { getBlob } from '@/shared/api/http'

export const useDownloadBackupHistoryFileMutation = () => {
  return useAppMutation({
    mutationFn: async (historyId: number) =>
      await getBlob(`/api/system/backup/histories/${historyId}/file`),
  })
}