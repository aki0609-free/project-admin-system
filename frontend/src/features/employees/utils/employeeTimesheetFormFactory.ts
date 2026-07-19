import { EmployeeTimesheetForm } from "../types/employeeTimesheetFormTypes"
import { EmployeeTimesheetResponse } from "../types/employeeWorkApiTypes"

export const createEmptyEmployeeTimesheetForm = (): EmployeeTimesheetForm => ({
  id: 0,
  employeeId: null,
  workDate: '',
  clockIn: '',
  clockOut: '',
  workHours: 0,
  overtimeHours: 0,
  nightShiftHours: 0,
  weekendFlag: false,
  approvalStatus: 'PENDING',
  approvalComment: '',
})

export const toEmployeeTimesheetForm = (
  item: EmployeeTimesheetResponse,
): EmployeeTimesheetForm => ({
  id: item.id,
  employeeId: item.employeeId,
  workDate: item.workDate,
  clockIn: item.clockIn ?? '',
  clockOut: item.clockOut ?? '',
  workHours: item.workHours ?? 0,
  overtimeHours: item.overtimeHours ?? 0,
  nightShiftHours: item.nightShiftHours ?? 0,
  weekendFlag: item.weekendFlag,
  approvalStatus: item.approvalStatus,
  approvalComment: item.approvalComment ?? '',
})