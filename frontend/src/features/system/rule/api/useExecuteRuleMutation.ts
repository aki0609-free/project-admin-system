import { useMutation } from '@tanstack/vue-query'

import { post } from '@/shared/api/http'
import type {
  RuleExecutionRequest,
  RuleExecutionResponse,
} from '@/features/system/rule/types/ruleApiTypes'

export const useExecuteRuleMutation = () =>
  useMutation({
    mutationFn: (body: RuleExecutionRequest) =>
      post<RuleExecutionResponse>(
        '/api/system/rules/execution/fire',
        body,
      ),
  })