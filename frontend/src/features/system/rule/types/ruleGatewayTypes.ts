export type RuleGatewayResultShape = 'SCALAR' | 'OBJECT' | 'LIST' | 'TABLE'

export type RuleGatewayExecuteRequest<TParameters extends Record<string, unknown> = Record<string, unknown>> = {
  gatewayKey: string
  targetDate?: string | null
  parameters: TParameters
}

export type RuleGatewayExecuteResponse<TResult = unknown> = {
  gatewayKey: string
  ruleName: string
  ruleVersion: number | null
  resultShape: RuleGatewayResultShape
  result: TResult
}

export type RuleGatewayBatchRow<TParameters extends Record<string, unknown> = Record<string, unknown>> = {
  rowKey: string
  parameters: TParameters
}

export type RuleGatewayBatchCalculation<TParameters extends Record<string, unknown> = Record<string, unknown>> = {
  outputKey: string
  gatewayKey: string
  parameters?: TParameters
}

export type RuleGatewayBatchRequest = {
  targetDate?: string | null
  failureMode?: 'FAIL_FAST' | 'PARTIAL'
  rows: RuleGatewayBatchRow[]
  calculations: RuleGatewayBatchCalculation[]
}

export type RuleGatewayBatchError = {
  code: string
  message: string
}

export type RuleGatewayBatchRowResponse<TResult = unknown> = {
  rowKey: string
  values: Record<string, TResult>
  errors: Record<string, RuleGatewayBatchError>
}

export type RuleGatewayBatchBinding = {
  outputKey: string
  gatewayKey: string
  ruleName: string
  ruleVersion: number | null
  resultShape: RuleGatewayResultShape
}

export type RuleGatewayBatchResponse<TResult = unknown> = {
  bindings: RuleGatewayBatchBinding[]
  rows: RuleGatewayBatchRowResponse<TResult>[]
}
