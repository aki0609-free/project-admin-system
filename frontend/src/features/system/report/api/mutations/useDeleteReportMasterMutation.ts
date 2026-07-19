import { useQueryClient } from '@tanstack/vue-query'
import { useAppMutation } from '@/shared/api/useAppMutation'
import { del } from '@/shared/api/http'
import { queryKeys } from '@/features/system/report/api/queryKeys'

type Payload = {
  id: number
}

type DeleteResponse = {
  message: string
}

export const useDeleteReportMasterMutation = () => {
  const queryClient = useQueryClient()

  return useAppMutation({
    mutationFn: ({ id }: Payload) =>
      del<DeleteResponse>(`/api/system/report-masters/${id}`),

    onSuccess: async () => {
      await queryClient.invalidateQueries({
        queryKey: queryKeys.reportMasters.all,
      })
    },
  })
}