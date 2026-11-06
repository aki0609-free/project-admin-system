import { executeRuleGatewayBatch } from '../api/ruleGatewayApi'
import type {
  RuleGatewayBatchError,
  RuleGatewayBatchResponse,
} from '../types/ruleGatewayTypes'

export type RuleGatewayTableRow<TRowKey extends string = string> = {
  rowKey: TRowKey
  parameters: Record<string, unknown>
}

export type RuleGatewayTableColumn<TOutputKey extends string = string> = {
  outputKey: TOutputKey
  gatewayKey: string
  parameters?: Record<string, unknown>
}

export type RuleGatewayTableResult<
  TRowKey extends string,
  TOutputKey extends string,
  TResult,
> = {
  values: Record<TRowKey, Partial<Record<TOutputKey, TResult>>>
  errors: Record<
    TRowKey,
    Partial<Record<TOutputKey, RuleGatewayBatchError>>
  >
  bindings: RuleGatewayBatchResponse<TResult>['bindings']
}

type CalculateRuleGatewayTableRequest<
  TRowKey extends string,
  TOutputKey extends string,
> = {
  targetDate?: string | null
  failureMode?: 'FAIL_FAST' | 'PARTIAL'
  rows: RuleGatewayTableRow<TRowKey>[]
  columns: RuleGatewayTableColumn<TOutputKey>[]
}

export const calculateRuleGatewayTable = async <
  TResult = number,
  TRowKey extends string = string,
  TOutputKey extends string = string,
>({
  targetDate,
  failureMode = 'PARTIAL',
  rows,
  columns,
}: CalculateRuleGatewayTableRequest<TRowKey, TOutputKey>): Promise<
  RuleGatewayTableResult<TRowKey, TOutputKey, TResult>
> => {
  const response = await executeRuleGatewayBatch<TResult>({
    targetDate,
    failureMode,
    rows,
    calculations: columns,
  })

  const values = {} as Record<
    TRowKey,
    Partial<Record<TOutputKey, TResult>>
  >
  const errors = {} as Record<
    TRowKey,
    Partial<Record<TOutputKey, RuleGatewayBatchError>>
  >

  for (const row of response.rows) {
    const rowKey = row.rowKey as TRowKey
    values[rowKey] = row.values as Partial<Record<TOutputKey, TResult>>
    errors[rowKey] = row.errors as Partial<
      Record<TOutputKey, RuleGatewayBatchError>
    >
  }

  return { values, errors, bindings: response.bindings }
}
