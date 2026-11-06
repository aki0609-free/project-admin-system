export type PayrollItemRuleTargetType = 'ALLOWANCE' | 'DEDUCTION'

export const createPayrollItemRuleGatewayKey = (
  targetType: PayrollItemRuleTargetType,
  masterId: number,
) => {
  if (!Number.isSafeInteger(masterId) || masterId <= 0) {
    throw new Error('masterIdには正の整数を指定してください')
  }
  return `PAYROLL_ITEM.${targetType}.${masterId}` as const
}
