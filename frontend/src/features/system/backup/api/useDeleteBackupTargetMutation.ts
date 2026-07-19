import { useQueryClient } from '@tanstack/vue-query'
import { useAppMutation } from '@/shared/api/useAppMutation'
import { del } from '@/shared/api/http'
import { queryKeys } from '@/features/system/backup/api/queryKeys'

type Payload = {
  id: number
}

export const useDeleteBackupTargetMutation = () => {
  const queryClient = useQueryClient()

  return useAppMutation({
    mutationFn: async ({ id }: Payload) =>
      await del<void>(`/api/system/backup/targets/${id}`),

    onSuccess: async () => {
      await queryClient.invalidateQueries({
        queryKey: queryKeys.backup.all,
      })
    },
  })
}