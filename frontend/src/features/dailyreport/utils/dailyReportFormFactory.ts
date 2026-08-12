import type {
  DailyReportDetailResponse,
} from '@/features/dailyreport/types/dailyReportApiTypes'

import type {
  DailyReportForm,
} from '@/features/dailyreport/types/dailyReportFormTypes'

export const createEmptyDailyReportForm =
  (): DailyReportForm => ({
    id: 0,

    employeeId: null,

    workDate: '',
    paymentDate: '',

    customerId: null,
    customerSiteId: null,

    customerName: '',
    siteName: '',

    billingRateId: null,

    jobCode: '',
    jobName: '',

    siteRoleCode: 'GENERAL',
    siteRoleName: '一般',

    billingUnit: null,

    billingBaseUnitPrice: 0,
    billingOvertimeUnitPrice: 0,
    billingNightUnitPrice: 0,
    billingHolidayUnitPrice: 0,
    billingCommuteUnitPrice: 0,

    workDescription: '',

    startTime: '',
    endTime: '',

    breakMinutes: 0,

    workHours: 0,
    overtimeHours: 0,
    nightWorkHours: 0,
    holidayWorkHours: 0,
    holidayPremiumEligible: false,

    allowanceAmount: 0,
    deductionAmount: 0,

    allowances: [],
    deductions: [],

    vehicleUsedFlag: false,
    mileage: 0,

    paidLeaveDays: 0,
    paidLeaveRemainingDays: 0,
    paidLeaveRemainingAfterUsedDays: 0,

    loanRepaymentAmount: 0,
    savingAmount: 0,
    dormitoryChargeDays: 0,

    estimatedGrossPayAmount: 0,
    estimatedNetPayAmount: 0,

    loanBalance: 0,
    savingBalance: 0,

    monthlyLoanRepayment: 0,
    monthlySavingAmount: 0,

    approvalStatus: 'APPROVED',
    approvalComment: '',
  })

export const toDailyReportForm = (
  item: DailyReportDetailResponse,
): DailyReportForm => ({
  id: item.id,

  employeeId:
    item.employeeId,

  workDate:
    item.workDate,

  paymentDate:
    item.paymentDate ?? '',

  customerId:
    item.customerId,

  customerSiteId:
    item.customerSiteId,

  customerName:
    item.customerName ?? '',

  siteName:
    item.siteName ?? '',

  billingRateId:
    item.billingRateId ?? null,

  jobCode:
    item.jobCode ?? '',

  jobName:
    item.jobName ?? '',

  siteRoleCode:
    item.siteRoleCode
    ?? 'GENERAL',

  siteRoleName:
    item.siteRoleName
    ?? '一般',

  billingUnit:
    item.billingUnit ?? null,

  billingBaseUnitPrice:
    item.billingBaseUnitPrice
    ?? 0,

  billingOvertimeUnitPrice:
    item.billingOvertimeUnitPrice
    ?? 0,

  billingNightUnitPrice:
    item.billingNightUnitPrice
    ?? 0,

  billingHolidayUnitPrice:
    item.billingHolidayUnitPrice
    ?? 0,

  billingCommuteUnitPrice:
    item.billingCommuteUnitPrice
    ?? 0,

  workDescription:
    item.workDescription ?? '',

  startTime:
    item.startTime ?? '',

  endTime:
    item.endTime ?? '',

  breakMinutes:
    item.breakMinutes ?? 0,

  workHours:
    item.workHours ?? 0,

  overtimeHours:
    item.overtimeHours ?? 0,

  nightWorkHours:
    item.nightWorkHours ?? 0,

  holidayWorkHours:
    item.holidayWorkHours ?? 0,

  holidayPremiumEligible:
    item.holidayPremiumEligible ?? false,

  allowanceAmount:
    item.allowanceAmount ?? 0,

  deductionAmount:
    item.deductionAmount ?? 0,

  allowances:
    item.allowances.map(
      allowance => ({
        masterId:
          allowance.allowanceMasterId,

        code:
          allowance.allowanceCode,

        name:
          allowance.allowanceName,

        itemType:
          'ALLOWANCE',

        inputMode:
          'MANUAL',

        calculatedAmount:
          allowance.amount ?? 0,

        amount:
          allowance.amount ?? 0,

        manualOverride: false,
        overrideReason: '',

        editable:
          true,

        displayOrder:
          0,

        balanceTracked: false,
        balanceUnit: null,
        openingQuantity: 0,
        accruedQuantity: 0,
        consumedQuantity: 0,
        remainingQuantity: 0,
        quantity: 0,
        remainingAfterQuantity: 0,
      }),
    ),

  deductions:
    item.deductions.map(
      deduction => ({
        masterId:
          deduction.deductionMasterId,

        code:
          deduction.deductionCode,

        name:
          deduction.deductionName,

        itemType:
          'DEDUCTION',

        inputMode:
          'MANUAL',

        calculatedAmount:
          deduction.calculatedAmount ?? deduction.amount ?? 0,

        amount:
          deduction.amount ?? 0,

        manualOverride:
          deduction.manualOverride ?? false,

        overrideReason:
          deduction.overrideReason ?? '',

        editable:
          true,

        displayOrder:
          0,

        balanceTracked:
          deduction.quantity != null,

        balanceUnit:
          (deduction.balanceUnit as 'DAYS' | 'HOURS' | 'COUNT' | 'AMOUNT' | null) ?? null,

        openingQuantity: 0,
        accruedQuantity: 0,
        consumedQuantity: 0,
        remainingQuantity: 0,
        quantity: deduction.quantity ?? 0,
        remainingAfterQuantity: 0,
      }),
    ),

  vehicleUsedFlag:
    item.vehicleUsedFlag,

  mileage:
    item.mileage ?? 0,

  paidLeaveDays:
    item.paidLeaveDays ?? 0,

  paidLeaveRemainingDays:
    item.paidLeaveRemainingDays
    ?? 0,

  paidLeaveRemainingAfterUsedDays:
    item.paidLeaveRemainingAfterUsedDays
    ?? 0,

  loanRepaymentAmount:
    item.loanRepaymentAmount
    ?? 0,

  savingAmount:
    item.savingAmount ?? 0,

  dormitoryChargeDays:
    item.dormitoryChargeDays ?? 0,

  estimatedGrossPayAmount:
    item.estimatedGrossPayAmount
    ?? 0,

  estimatedNetPayAmount:
    item.estimatedNetPayAmount
    ?? 0,

  loanBalance:
    item.loanBalance ?? 0,

  savingBalance:
    item.savingBalance ?? 0,

  monthlyLoanRepayment:
    item.monthlyLoanRepayment
    ?? 0,

  monthlySavingAmount:
    item.monthlySavingAmount
    ?? 0,

  approvalStatus:
    item.approvalStatus,

  approvalComment:
    item.approvalComment ?? '',
})
