import { useQueryClient } from '@tanstack/vue-query'
import { useAppMutation } from '@/shared/api/useAppMutation'
import { del } from '@/shared/api/http'
import { queryKeys } from '@/features/system/report/api/queryKeys'

type Payload = {
  id: number
  reportMasterId: number
}

export const useDeleteReportSignatureMutation = () => {
  const queryClient = useQueryClient()

  return useAppMutation({
    mutationFn: ({ id }: Payload) =>
      del<{ message: string }>(`/api/system/report-signatures/${id}`),

    onSuccess: async (_data: any, variables: any) => {
      await Promise.all([
        queryClient.invalidateQueries({
          queryKey: queryKeys.reportSignatures.all,
        }),
        queryClient.invalidateQueries({
          queryKey: queryKeys.reportSignatures.byReportMaster(
            variables.reportMasterId,
          ),
        }),
      ])
    },
  })
}