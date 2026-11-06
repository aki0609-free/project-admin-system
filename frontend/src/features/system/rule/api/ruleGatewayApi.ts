import { post } from '@/shared/api/http'
import type {
  RuleGatewayBatchRequest,
  RuleGatewayBatchResponse,
  RuleGatewayExecuteRequest,
  RuleGatewayExecuteResponse,
} from '../types/ruleGatewayTypes'

const basePath = '/api/system/rule-gateway'

export const executeRuleGateway = <
  TResult = unknown,
  TParameters extends Record<string, unknown> = Record<string, unknown>,
>(request: RuleGatewayExecuteRequest<TParameters>) =>
  post<RuleGatewayExecuteResponse<TResult>, RuleGatewayExecuteRequest<TParameters>>(
    `${basePath}/execute`,
    request,
  )

export const executeRuleGatewayBatch = <TResult = unknown>(
  request: RuleGatewayBatchRequest,
) => post<RuleGatewayBatchResponse<TResult>, RuleGatewayBatchRequest>(
  `${basePath}/batch`,
  request,
)
