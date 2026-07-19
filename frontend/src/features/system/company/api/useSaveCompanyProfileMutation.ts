import { useQueryClient } from '@tanstack/vue-query'

import { useAppMutation } from '@/shared/api/useAppMutation'
import { put } from '@/shared/api/http'

import { queryKeys } from './queryKeys'

import type {
  CompanyProfileResponse,
  CompanyProfileSaveRequest,
} from '../types/companyProfileApiTypes'

export const useSaveCompanyProfileMutation = () => {
  const queryClient = useQueryClient()

  return useAppMutation({
    mutationFn: (
      request: CompanyProfileSaveRequest,
    ) =>
      put<
        CompanyProfileResponse,
        CompanyProfileSaveRequest
      >(
        '/api/system/company-profile',
        request,
      ),

    onSuccess: async () => {
      await queryClient.invalidateQueries({
        queryKey: queryKeys.companyProfile.all,
      })
    },
  })
}