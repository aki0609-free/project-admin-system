import { ApprovalStatus } from "./employeeWorkApiTypes"

export type EmployeeTimesheetForm = {
  id: number
  employeeId: number | null
  workDate: string
  clockIn: string
  clockOut: string
  workHours: number
  overtimeHours: number
  nightShiftHours: number
  weekendFlag: boolean
  approvalStatus: ApprovalStatus
  approvalComment: string
}