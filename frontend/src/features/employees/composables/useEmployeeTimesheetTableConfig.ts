import { computed, type Ref } from 'vue'
import type {
  SimpleTableColumnDef,
  SimpleTableEditableRow,
} from '@/shared/components/table/simple_table/types/item/types'
import { createSimpleTableFilterRules } from '@/shared/components/table/simple_table/utils/createSimpleTableFilterRules'
import { EmployeeTimesheetResponse } from '../types/employeeWorkApiTypes'

export type EmployeeTimesheetTableRow = SimpleTableEditableRow & {
  id: number
  employeeCode: string
  employeeName: string
  workDate: string
  clockIn: string
  clockOut: string
  workHours: number
  overtimeHours: number
  nightShiftHours: number
  weekendText: string
  approvalStatus: string
  approvalComment: string
  raw: EmployeeTimesheetResponse
}

export const useEmployeeTimesheetTableConfig = (
  timesheets: Ref<EmployeeTimesheetResponse[]>,
) => {
  const rows = computed<EmployeeTimesheetTableRow[]>(() =>
    timesheets.value.map((item) => ({
      id: item.id,
      employeeCode: item.employeeCode,
      employeeName: item.employeeName,
      workDate: item.workDate,
      clockIn: item.clockIn ?? '',
      clockOut: item.clockOut ?? '',
      workHours: item.workHours,
      overtimeHours: item.overtimeHours,
      nightShiftHours: item.nightShiftHours,
      weekendText: item.weekendFlag ? '土日' : '',
      approvalStatus: item.approvalStatus,
      approvalComment: item.approvalComment ?? '',
      raw: item,
    })),
  )

  const columns = computed(() => {
    const defs: SimpleTableColumnDef<EmployeeTimesheetTableRow>[] = [
      { title: 'ID', key: 'id', width: '80px', filter: { type: 'text' } },
      { title: '社員コード', key: 'employeeCode', width: '140px', filter: { type: 'text' } },
      { title: '氏名', key: 'employeeName', width: '160px', filter: { type: 'text' } },
      { title: '勤務日', key: 'workDate', width: '130px', filter: { type: 'text' } },
      { title: '出勤', key: 'clockIn', width: '100px', filter: { type: 'text' } },
      { title: '退勤', key: 'clockOut', width: '100px', filter: { type: 'text' } },
      { title: '通常', key: 'workHours', width: '100px', filter: { type: 'text' } },
      { title: '残業', key: 'overtimeHours', width: '100px', filter: { type: 'text' } },
      { title: '深夜', key: 'nightShiftHours', width: '100px', filter: { type: 'text' } },
      { title: '土日', key: 'weekendText', width: '90px', filter: { type: 'text' } },
      { title: '承認', key: 'approvalStatus', width: '120px', filter: { type: 'text' } },
      { title: 'コメント', key: 'approvalComment', width: '240px', filter: { type: 'text' } },
    ]

    return defs
  })

  const filterRules = computed(() =>
    createSimpleTableFilterRules<EmployeeTimesheetTableRow>(columns.value),
  )

  return {
    rows,
    columns,
    filterRules,
  }
}