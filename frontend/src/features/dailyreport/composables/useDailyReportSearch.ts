import { computed, reactive } from 'vue'
import type { SearchPanelFieldDef } from '@/shared/components/search/types/searchPanelTypes'
import type { DailyReportResponse } from '@/features/dailyreport/types/dailyReportApiTypes'

export type DailyReportSearchCondition = {
  employeeKeyword: string

  workDateFrom: string
  workDateTo: string

  paymentDateFrom: string
  paymentDateTo: string

  targetWorkDate: string
  attendanceTargetMonth: string
}

export const useDailyReportSearch = (reportsGetter: () => DailyReportResponse[]) => {
  const condition = reactive<DailyReportSearchCondition>({
    employeeKeyword: '',

    workDateFrom: '',
    workDateTo: '',

    paymentDateFrom: '',
    paymentDateTo: '',

    targetWorkDate: '',
    attendanceTargetMonth: '',
  })

  const reportFields = computed<SearchPanelFieldDef<DailyReportSearchCondition>[]>(() => [
    {
      key: 'employeeKeyword',
      label: '従業員',
      type: 'text',
      md: 2,
      prependIcon: 'mdi-account-search-outline',
      placeholder: '社員コード・氏名',
    },
    {
      key: 'workDateFrom',
      label: '勤務日From',
      type: 'date',
      md: 2,
      prependIcon: 'mdi-calendar-start',
    },
    {
      key: 'workDateTo',
      label: '勤務日To',
      type: 'date',
      md: 2,
      prependIcon: 'mdi-calendar-end',
    },
    {
      key: 'paymentDateFrom',
      label: '支払日From',
      type: 'date',
      md: 2,
      prependIcon: 'mdi-cash-clock',
    },
    {
      key: 'paymentDateTo',
      label: '支払日To',
      type: 'date',
      md: 2,
      prependIcon: 'mdi-cash-check',
    },
  ])

  const attendanceFields = computed<SearchPanelFieldDef<DailyReportSearchCondition>[]>(() => [
    {
      key: 'targetWorkDate',
      label: '未入力確認日',
      type: 'date',
      md: 3,
      prependIcon: 'mdi-calendar-alert',
    },
    {
      key: 'attendanceTargetMonth',
      label: '勤怠対象月',
      type: 'month',
      md: 3,
      prependIcon: 'mdi-calendar-month',
    },
  ])

  const filteredReports = computed<DailyReportResponse[]>(() =>
    reportsGetter().filter((report) => {
      if (condition.employeeKeyword) {
        const keyword = condition.employeeKeyword.toLowerCase()

        const employeeHit =
          report.employeeCode?.toLowerCase().includes(keyword) ||
          report.employeeName?.toLowerCase().includes(keyword)

        if (!employeeHit) return false
      }

      if (condition.workDateFrom && report.workDate < condition.workDateFrom) {
        return false
      }

      if (condition.workDateTo && report.workDate > condition.workDateTo) {
        return false
      }

      if (
        condition.paymentDateFrom &&
        (!report.paymentDate || report.paymentDate < condition.paymentDateFrom)
      ) {
        return false
      }

      if (
        condition.paymentDateTo &&
        (!report.paymentDate || report.paymentDate > condition.paymentDateTo)
      ) {
        return false
      }

      return true
    }).sort((left, right) => {
      const workDateOrder = right.workDate.localeCompare(left.workDate)
      if (workDateOrder !== 0) return workDateOrder

      const paymentDateOrder = (left.paymentDate ?? '')
        .localeCompare(right.paymentDate ?? '')
      if (paymentDateOrder !== 0) return paymentDateOrder

      return right.id - left.id
    }),
  )

  const clearReportFilters = () => {
    condition.employeeKeyword = ''
    condition.workDateFrom = ''
    condition.workDateTo = ''
    condition.paymentDateFrom = ''
    condition.paymentDateTo = ''
  }

  const clearAttendanceFilters = () => {
    condition.targetWorkDate = ''
    condition.attendanceTargetMonth = ''
  }

  return {
    condition,
    reportFields,
    attendanceFields,
    filteredReports,
    clearReportFilters,
    clearAttendanceFilters,
  }
}
