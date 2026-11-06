import { beforeEach, describe, expect, it, vi } from 'vitest'

import { executeRuleGatewayBatch } from '@/features/system/rule/api/ruleGatewayApi'
import { calculatePayrollItemRuleTable } from './payrollItemRuleTableAdapter'

vi.mock('@/features/system/rule/api/ruleGatewayApi', () => ({
  executeRuleGatewayBatch: vi.fn(),
}))

const executeRuleGatewayBatchMock = vi.mocked(executeRuleGatewayBatch)

describe('calculatePayrollItemRuleTable', () => {
  beforeEach(() => {
    executeRuleGatewayBatchMock.mockReset()
  })

  it('複数行と複数のAUTO項目を1回のbatch通信へまとめる', async () => {
    executeRuleGatewayBatchMock.mockResolvedValue({
      bindings: [
        {
          outputKey: 'legalDeposit',
          gatewayKey: 'PAYROLL_ITEM.DEDUCTION.9',
          ruleName: 'DAILY_LEGAL_DEPOSIT_ESTIMATE',
          ruleVersion: null,
          resultShape: 'SCALAR',
        },
        {
          outputKey: 'driverAllowance',
          gatewayKey: 'PAYROLL_ITEM.ALLOWANCE.5',
          ruleName: 'DRIVER_ALLOWANCE',
          ruleVersion: null,
          resultShape: 'SCALAR',
        },
      ],
      rows: [
        {
          rowKey: 'employee-1',
          values: { legalDeposit: 1000, driverAllowance: 750 },
          errors: {},
        },
        {
          rowKey: 'employee-2',
          values: { legalDeposit: 1800, driverAllowance: 1200 },
          errors: {},
        },
      ],
    })

    const result = await calculatePayrollItemRuleTable({
      targetDate: '2026-11-06',
      rows: [
        {
          rowKey: 'employee-1',
          parameters: {
            predictedStatutoryDeductionTotal: 20000,
            distanceKm: 30,
          },
        },
        {
          rowKey: 'employee-2',
          parameters: {
            predictedStatutoryDeductionTotal: 36000,
            distanceKm: 50,
          },
        },
      ],
      columns: [
        {
          outputKey: 'legalDeposit',
          targetType: 'DEDUCTION',
          masterId: 9,
        },
        {
          outputKey: 'driverAllowance',
          targetType: 'ALLOWANCE',
          masterId: 5,
        },
      ],
    })

    expect(executeRuleGatewayBatchMock).toHaveBeenCalledTimes(1)
    expect(executeRuleGatewayBatchMock).toHaveBeenCalledWith({
      targetDate: '2026-11-06',
      failureMode: 'PARTIAL',
      rows: [
        {
          rowKey: 'employee-1',
          parameters: {
            predictedStatutoryDeductionTotal: 20000,
            distanceKm: 30,
          },
        },
        {
          rowKey: 'employee-2',
          parameters: {
            predictedStatutoryDeductionTotal: 36000,
            distanceKm: 50,
          },
        },
      ],
      calculations: [
        {
          outputKey: 'legalDeposit',
          gatewayKey: 'PAYROLL_ITEM.DEDUCTION.9',
          parameters: undefined,
        },
        {
          outputKey: 'driverAllowance',
          gatewayKey: 'PAYROLL_ITEM.ALLOWANCE.5',
          parameters: undefined,
        },
      ],
    })
    expect(result.values['employee-1']).toEqual({
      legalDeposit: 1000,
      driverAllowance: 750,
    })
    expect(result.values['employee-2']).toEqual({
      legalDeposit: 1800,
      driverAllowance: 1200,
    })
  })

  it('部分失敗を行と出力列の組で返す', async () => {
    executeRuleGatewayBatchMock.mockResolvedValue({
      bindings: [],
      rows: [
        {
          rowKey: 'employee-1',
          values: {},
          errors: {
            legalDeposit: {
              code: 'RULE_CALCULATION_FAILED',
              message: '計算に失敗しました。',
            },
          },
        },
      ],
    })

    const result = await calculatePayrollItemRuleTable({
      rows: [{ rowKey: 'employee-1', parameters: {} }],
      columns: [
        {
          outputKey: 'legalDeposit',
          targetType: 'DEDUCTION',
          masterId: 9,
        },
      ],
    })

    expect(result.errors['employee-1']?.legalDeposit?.code).toBe(
      'RULE_CALCULATION_FAILED',
    )
  })
})
