import { EmployeeTimesheetForm } from "../types/employeeTimesheetFormTypes"
import { EmployeeTimesheetSaveRequest } from "../types/employeeWorkApiTypes"

const blankToNull = (value: string): string | null =>
  value.trim() ? value : null

export const toEmployeeTimesheetSaveRequest = (
  form: EmployeeTimesheetForm,
): EmployeeTimesheetSaveRequest => {
  if (form.employeeId == null) {
    throw new Error('employeeId is required.')
  }

  return {
    employeeId: form.employeeId,
    workDate: form.workDate,
    clockIn: blankToNull(form.clockIn),
    clockOut: blankToNull(form.clockOut),
    workHours: form.workHours,
    overtimeHours: form.overtimeHours,
    nightShiftHours: form.nightShiftHours,
    weekendFlag: form.weekendFlag,
    approvalStatus: form.approvalStatus,
    approvalComment: blankToNull(form.approvalComment),
  }
}