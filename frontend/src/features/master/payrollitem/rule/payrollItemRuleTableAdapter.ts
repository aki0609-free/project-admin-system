import { calculateRuleGatewayTable } from '@/features/system/rule/table/ruleGatewayTableCalculator'
import { createPayrollItemRuleGatewayKey } from '@/features/system/rule/utils/ruleGatewayKeys'

export type PayrollItemRuleTargetType = 'ALLOWANCE' | 'DEDUCTION'

export type PayrollItemRuleTableRow<TRowKey extends string = string> = {
  rowKey: TRowKey
  parameters: Record<string, unknown>
}

export type PayrollItemRuleTableColumn<
  TOutputKey extends string = string,
> = {
  outputKey: TOutputKey
  targetType: PayrollItemRuleTargetType
  masterId: number
  parameters?: Record<string, unknown>
}

type CalculatePayrollItemRuleTableRequest<
  TRowKey extends string,
  TOutputKey extends string,
> = {
  targetDate?: string | null
  failureMode?: 'FAIL_FAST' | 'PARTIAL'
  rows: PayrollItemRuleTableRow<TRowKey>[]
  columns: PayrollItemRuleTableColumn<TOutputKey>[]
}

export const calculatePayrollItemRuleTable = <
  TRowKey extends string = string,
  TOutputKey extends string = string,
>({
  targetDate,
  failureMode = 'PARTIAL',
  rows,
  columns,
}: CalculatePayrollItemRuleTableRequest<TRowKey, TOutputKey>) =>
  calculateRuleGatewayTable<number, TRowKey, TOutputKey>({
    targetDate,
    failureMode,
    rows,
    columns: columns.map(column => ({
      outputKey: column.outputKey,
      gatewayKey: createPayrollItemRuleGatewayKey(
        column.targetType,
        column.masterId,
      ),
      parameters: column.parameters,
    })),
  })
