import { z } from 'zod'

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

  allowanceAmount: z.number().min(0),
  deductionAmount: z.number().min(0),

  loanRepaymentAmount: z.number().min(0),
  savingAmount: z.number().min(0),

  estimatedGrossPayAmount: z.number().min(0),
  estimatedNetPayAmount: z.number(),

  loanBalance: z.number().min(0),
  savingBalance: z.number().min(0),

  monthlyLoanRepayment: z.number().min(0),
  monthlySavingAmount: z.number().min(0),

  allowances: z.array(z.any()),
  deductions: z.array(z.any()),

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