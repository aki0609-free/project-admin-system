import { computed, type Ref } from 'vue'
import type {
  SimpleTableColumnDef,
  SimpleTableEditableRow,
} from '@/shared/components/table/simple_table/types/item/types'
import { createSimpleTableFilterRules } from '@/shared/components/table/simple_table/utils/createSimpleTableFilterRules'
import type { DailyReportResponse } from '@/features/dailyreport/types/dailyReportApiTypes'
import { formatHours, normalizeTimeHHmm } from '@/shared/utils/TimeUtils'
import { formatYearMonthDay } from '@/shared/utils/DateUtils'

export type DailyReportTableRow = SimpleTableEditableRow & {
  id: number
  employeeCode: string
  employeeName: string

  workDate: string
  paymentDate: string

  customerName: string
  siteName: string

  startTime: string
  endTime: string

  workHours: number
  overtimeHours: number
  nightWorkHours: number

  vehicleText: string
  paidLeaveText: string
  approvalStatus: string

  raw: DailyReportResponse
}

export const useDailyReportTableConfig = (
  reports: Ref<DailyReportResponse[]>,
) => {
  const rows = computed<DailyReportTableRow[]>(() =>
    reports.value.map((item) => ({
      id: item.id,

      employeeCode: item.employeeCode,
      employeeName: item.employeeName,

      workDate: item.workDate,
      paymentDate: item.paymentDate ?? '',

      customerName: item.customerName ?? '',
      siteName: item.siteName ?? '',

      startTime: item.startTime ?? '',
      endTime: item.endTime ?? '',

      workHours: item.workHours,
      overtimeHours: item.overtimeHours,
      nightWorkHours: item.nightWorkHours,

      vehicleText: item.vehicleUsedFlag ? '使用' : '',
      paidLeaveText: item.paidLeaveUsedFlag ? '使用' : '',
      approvalStatus: item.approvalStatus,

      raw: item,
    })),
  )

  const columns = computed(() => {
    const defs: SimpleTableColumnDef<DailyReportTableRow>[] = [
      { title: 'ID', key: 'id', width: '180px', filter: { type: 'text' } },
      {
        title: '勤務日',
        key: 'workDate',
        width: '180px',
        filter: { type: 'text' },
        formatter: (value) => formatYearMonthDay(value as string),
      },
      {
        title: '支払日',
        key: 'paymentDate',
        width: '180px',
        filter: { type: 'text' },
        formatter: (value) => formatYearMonthDay(value as string),
      },
      { title: '社員コード', key: 'employeeCode', width: '180px', filter: { type: 'text' } },
      { title: '氏名', key: 'employeeName', width: '180px', filter: { type: 'text' } },
      { title: '顧客', key: 'customerName', width: '180px', filter: { type: 'text' } },
      { title: '現場', key: 'siteName', width: '180px', filter: { type: 'text' } },
      {
        title: '開始時間',
        key: 'startTime',
        width: '180px',
        filter: { type: 'text' },
        formatter: (value) => normalizeTimeHHmm(value as string),
      },
      {
        title: '終了時間',
        key: 'endTime',
        width: '180px',
        filter: { type: 'text' },
        formatter: (value) => normalizeTimeHHmm(value as string),
      },
      {
        title: '通常時間',
        key: 'workHours',
        width: '180px',
        filter: { type: 'text' },
        formatter: (value) => formatHours(value as string),
      },
      {
        title: '早出・残業時間',
        key: 'overtimeHours',
        width: '180px',
        filter: { type: 'text' },
        formatter: (value) => formatHours(value as string),
      },
      {
        title: '深夜時間',
        key: 'nightWorkHours',
        width: '180px',
        filter: { type: 'text' },
        formatter: (value) => formatHours(value as string),
      },
      { title: '車両', key: 'vehicleText', width: '180px', filter: { type: 'text' } },
      { title: '有給', key: 'paidLeaveText', width: '180px', filter: { type: 'text' } },
      { title: '承認', key: 'approvalStatus', width: '180px', filter: { type: 'text' } },
    ]

    return defs
  })

  const filterRules = computed(() =>
    createSimpleTableFilterRules<DailyReportTableRow>(columns.value),
  )

  return {
    rows,
    columns,
    filterRules,
  }
}