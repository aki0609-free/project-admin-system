/* eslint-disable @typescript-eslint/no-invalid-void-type */
import { useMutation, useQueryClient } from '@tanstack/vue-query'
import { del, post, put } from '@/shared/api/http'
import { ruleQueryKeys } from './queryKeys'
import type {
  RuleMasterResponse,
  RuleMasterSaveRequest,
} from '../types/ruleApiTypes'

export const useCreateRuleMutation = () => {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (body: RuleMasterSaveRequest) =>
      post<RuleMasterResponse>('/api/system/rules', body),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ruleQueryKeys.rules.all,
      })
    },
  })
}

export const useUpdateRuleMutation = () => {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({
      id,
      body,
    }: {
      id: number
      body: RuleMasterSaveRequest
    }) =>
      put<RuleMasterResponse>(`/api/system/rules/${id}`, body),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ruleQueryKeys.rules.all,
      })
    },
  })
}

export const useDeleteRuleMutation = () => {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (id: number) =>
      del<void>(`/api/system/rules/${id}`),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ruleQueryKeys.rules.all,
      })
    },
  })
}