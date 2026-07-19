import { useQueryClient } from '@tanstack/vue-query'
import { useAppMutation } from '@/shared/api/useAppMutation'
import { post } from '@/shared/api/http'
import { queryKeys } from '@/features/application/api/queryKeys'
import type { ApplicantCreateRequest } from '@/features/application/types/applicantApiTypes'

export const useCreateApplicantMutation = () => {
  const queryClient = useQueryClient()

  return useAppMutation({
    mutationFn: (payload: ApplicantCreateRequest) =>
      post<number, ApplicantCreateRequest>('/api/applicants', payload),

    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: queryKeys.applicants.all }),
        queryClient.invalidateQueries({ queryKey: queryKeys.applicationMedias.all }),
      ])
    },
  })
}