import { computed } from 'vue'
import { useAppQuery } from '@/shared/api/useAppQuery'
import { get } from '@/shared/api/http'
import { queryKeys } from '@/features/application/api/queryKeys'
import type { ApplicantListItemResponse } from '@/features/application/types/applicantApiTypes' 

export const useApplicantsQuery = () => {
  const query = useAppQuery<ApplicantListItemResponse[]>({
    queryKey: queryKeys.applicants.list(),
    queryFn: () => get<ApplicantListItemResponse[]>('/api/applicants'),
  })

  const applicants = computed(() => query.data.value ?? [])

  return {
    ...query,
    applicants,
  }
}