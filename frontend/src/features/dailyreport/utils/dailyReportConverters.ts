import type {
  DailyReportSaveRequest,
} from '@/features/dailyreport/types/dailyReportApiTypes'

import type {
  DailyReportForm,
} from '@/features/dailyreport/types/dailyReportFormTypes'

const blankToNull = (
  value: string,
): string | null =>
  value.trim()
    ? value.trim()
    : null

export const toDailyReportSaveRequest = (
  form: DailyReportForm,
): DailyReportSaveRequest => {
  if (form.employeeId == null) {
    throw new Error(
      'employeeId is required.',
    )
  }

  return {
    employeeId: form.employeeId,

    workDate: form.workDate,
    paymentDate:
      blankToNull(form.paymentDate),

    customerId:
      form.customerId,

    customerSiteId:
      form.customerSiteId,

    customerName:
      blankToNull(form.customerName),

    siteName:
      blankToNull(form.siteName),

    jobCode:
      blankToNull(form.jobCode),

    jobName:
      blankToNull(form.jobName),

    siteRoleCode:
      form.customerSiteId == null
        ? null
        : (
          blankToNull(
            form.siteRoleCode,
          ) ?? 'GENERAL'
        ),

    siteRoleName:
      form.customerSiteId == null
        ? null
        : (
          blankToNull(
            form.siteRoleName,
          ) ?? '一般'
        ),

    workDescription:
      blankToNull(
        form.workDescription,
      ),

    startTime:
      blankToNull(form.startTime),

    endTime:
      blankToNull(form.endTime),

    breakMinutes:
      Number(
        form.breakMinutes ?? 0,
      ),

    workHours:
      Number(
        form.workHours ?? 0,
      ),

    overtimeHours:
      Number(
        form.overtimeHours ?? 0,
      ),

    nightWorkHours:
      Number(
        form.nightWorkHours ?? 0,
      ),

    holidayWorkHours:
      Number(
        form.holidayWorkHours ?? 0,
      ),

    allowanceAmount:
      Number(
        form.allowanceAmount ?? 0,
      ),

    deductionAmount:
      Number(
        form.deductionAmount ?? 0,
      ),

    vehicleUsedFlag:
      Boolean(
        form.vehicleUsedFlag,
      ),

    mileage:
      Number(
        form.mileage ?? 0,
      ),

    paidLeaveDays:
      Number(
        form.paidLeaveDays ?? 0,
      ),

    loanRepaymentAmount:
      Number(
        form.loanRepaymentAmount
        ?? 0,
      ),

    savingAmount:
      Number(
        form.savingAmount ?? 0,
      ),

    approvalStatus:
      form.approvalStatus,

    approvalComment:
      blankToNull(
        form.approvalComment,
      ),

    allowances:
      form.allowances.map(
        item => ({
          allowanceMasterId:
            item.masterId,

          allowanceCode:
            item.code,

          allowanceName:
            item.name,

          amount:
            Number(
              item.amount ?? 0,
            ),
        }),
      ),

    deductions:
      form.deductions.map(
        item => ({
          deductionMasterId:
            item.masterId,

          deductionCode:
            item.code,

          deductionName:
            item.name,

          amount:
            Number(
              item.amount ?? 0,
            ),
        }),
      ),
  }
}