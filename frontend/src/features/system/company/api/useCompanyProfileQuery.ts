import { computed } from 'vue'

import { useAppQuery } from '@/shared/api/useAppQuery'
import { get } from '@/shared/api/http'

import { queryKeys } from './queryKeys'

import type {
  CompanyProfileResponse,
} from '../types/companyProfileApiTypes'

export const useCompanyProfileQuery = () => {
  const query = useAppQuery<
    CompanyProfileResponse | null
  >({
    queryKey: queryKeys.companyProfile.current(),

    queryFn: () =>
      get<CompanyProfileResponse | null>(
        '/api/system/company-profile',
      ),
  })

  const companyProfile = computed(
    () => query.data.value ?? null,
  )

  return {
    ...query,
    companyProfile,
  }
}