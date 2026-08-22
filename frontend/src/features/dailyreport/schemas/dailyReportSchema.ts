import { z } from 'zod'

export const dailyReportAmountItemSchema = z.object({
  masterId: z.number().int().positive(),
  code: z.string(),
  name: z.string(),
  itemType: z.enum(['ALLOWANCE', 'DEDUCTION']),
  inputMode: z.enum([
    'MANUAL',
    'FIXED',
    'FIXED_WITH_OVERRIDE',
    'AUTO_CALCULATED',
    'AUTO_WITH_OVERRIDE',
  ]),
  calculatedAmount: z.number().int().min(0),
  amount: z.number().int().min(0, '手当・控除金額は0以上で入力してください'),
  manualOverride: z.boolean(),
  overrideReason: z.string().max(500),
  editable: z.boolean(),
  displayOrder: z.number().int(),
  balanceTracked: z.boolean(),
  balanceUnit: z.enum(['DAYS', 'HOURS', 'COUNT', 'AMOUNT']).nullable(),
  openingQuantity: z.number().min(0),
  accruedQuantity: z.number().min(0),
  consumedQuantity: z.number().min(0),
  remainingQuantity: z.number().min(0),
  quantity: z.number().min(0),
  remainingAfterQuantity: z.number().min(0),
}).superRefine((item, context) => {
  if (item.manualOverride && item.overrideReason.trim().length === 0) {
    context.addIssue({
      code: 'custom',
      path: ['overrideReason'],
      message: '金額を変更した場合は変更理由が必須です',
    })
  }
})

export const dailyReportSchema = z.object({
  id: z.number(),
  employeeId: z.number().nullable(),

  workDate: z.string().min(1, '勤務日は必須です'),
  paymentDate: z.string(),

  customerId: z.number().nullable(),
  customerSiteId: z.number().nullable(),

  customerName: z.string(),
  siteName: z.string(),

  billingRateId: z.number().nullable(),

  jobCode: z.string(),
  jobName: z.string(),

  siteRoleCode: z.string(),
  siteRoleName: z.string(),

  billingUnit: z
    .enum([
      'HOURLY',
      'DAILY',
      'MONTHLY',
      'FIXED',
    ])
    .nullable(),

  billingBaseUnitPrice: z.number().min(0),
  billingOvertimeUnitPrice: z.number().min(0),
  billingNightUnitPrice: z.number().min(0),
  billingHolidayUnitPrice: z.number().min(0),
  billingCommuteUnitPrice: z.number().min(0),

  workDescription: z.string(),

  startTime: z.string(),
  endTime: z.string(),

  breakMinutes: z.number().min(0),

  workHours: z.number().min(0),
  overtimeHours: z.number().min(0),
  nightWorkHours: z.number().min(0),
  holidayWorkHours: z.number().min(0),
  holidayPremiumEligible: z.boolean(),

  allowanceAmount: z.number().min(0),
  deductionAmount: z.number().min(0),

  loanRepaymentAmount: z.number().min(0),
  savingAmount: z.number().min(0),
  dormitoryChargeDays: z.number().int().min(0).max(31),

  estimatedGrossPayAmount: z.number().min(0),
  estimatedNetPayAmount: z.number(),

  loanBalance: z.number().min(0),
  savingBalance: z.number().min(0),

  monthlyLoanRepayment: z.number().min(0),
  monthlySavingAmount: z.number().min(0),

  allowances: z.array(dailyReportAmountItemSchema),
  deductions: z.array(dailyReportAmountItemSchema),

  vehicleUsedFlag: z.boolean(),
  mileage: z.number().min(0),

  paidLeaveDays: z.number().min(0),
  paidLeaveRemainingDays: z.number().min(0),
  paidLeaveRemainingAfterUsedDays: z.number(),

  approvalStatus: z.enum([
    'PENDING',
    'APPROVED',
    'REJECTED',
  ]),

  approvalComment: z.string(),
})
