import { useAppMutation } from '@/shared/api/useAppMutation'
import { getBlob } from '@/shared/api/http'

export const useDownloadBatchLogFileMutation = () => {
  return useAppMutation({
    mutationFn: async (logId: number): Promise<Blob> =>
      await getBlob(`/api/system/batch/logs/${logId}/file`),
  })
}