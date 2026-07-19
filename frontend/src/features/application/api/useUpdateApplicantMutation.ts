import { useQueryClient } from '@tanstack/vue-query'
import { useAppMutation } from '@/shared/api/useAppMutation'
import { put } from '@/shared/api/http'
import { queryKeys } from '@/features/application/api/queryKeys'
import type { ApplicantUpdateRequest } from '@/features/application/types/applicantApiTypes'

export const useUpdateApplicantMutation = () => {
  const queryClient = useQueryClient()

  return useAppMutation({
    mutationFn: (payload: ApplicantUpdateRequest) => {
      const { id, ...body } = payload

      return put<void, typeof body>('/api/applicants/{id}', body, {
        params: {
          path: { id },
        },
      })
    },

    onSuccess: async (_result: any, payload: any) => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: queryKeys.applicants.all }),
        queryClient.invalidateQueries({ queryKey: queryKeys.applicationMedias.all }),
        queryClient.invalidateQueries({
          queryKey: queryKeys.applicants.detail(payload.id),
        }),
      ])
    },
  })
}