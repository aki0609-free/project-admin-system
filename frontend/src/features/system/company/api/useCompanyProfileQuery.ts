import { computed } from 'vue'

import { apiClient } from '@/app/plugins/apiClient'
import { useAppQuery } from '@/shared/api/useAppQuery'

import { queryKeys } from './queryKeys'

import type { CompanyProfileResponse } from '../types/companyProfileApiTypes'

export const useCompanyProfileQuery = () => {
  const query = useAppQuery<CompanyProfileResponse | null>({
    queryKey: queryKeys.companyProfile.current(),

    queryFn: async () => {
      const { data, error } = await apiClient.GET('/api/system/company-profile')

      if (error) {
        throw error
      }

      // 会社情報が未登録の場合、バックエンドは本文なしの正常応答を返す。
      // 共通GET関数は本文なしを取得失敗として扱うため、このAPIだけは
      // 未登録を正常な null として扱う。
      return data ?? null
    },
  })

  const companyProfile = computed(() => query.data.value ?? null)

  return {
    ...query,
    companyProfile,
  }
}
