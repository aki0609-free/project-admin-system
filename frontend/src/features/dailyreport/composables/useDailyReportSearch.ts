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

  keyword: string
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

    keyword: '',
  })

  const fields = computed<SearchPanelFieldDef<DailyReportSearchCondition>[]>(() => [
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
    {
      key: 'keyword',
      label: 'キーワード',
      type: 'text',
      md: 2,
      prependIcon: 'mdi-magnify',
      placeholder: '現場名・顧客名',
    },
    {
      key: 'targetWorkDate',
      label: '未入力確認日',
      type: 'date',
      md: 2,
      prependIcon: 'mdi-calendar-alert',
    },
    {
      key: 'attendanceTargetMonth',
      label: '勤怠対象月',
      type: 'month',
      md: 2,
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

      if (condition.keyword) {
        const keyword = condition.keyword.toLowerCase()

        const hit =
          report.customerName?.toLowerCase().includes(keyword) ||
          report.siteName?.toLowerCase().includes(keyword)

        if (!hit) return false
      }

      return true
    }),
  )

  const clear = () => {
    condition.employeeKeyword = ''

    condition.targetWorkDate = ''

    condition.workDateFrom = ''
    condition.workDateTo = ''

    condition.paymentDateFrom = ''
    condition.paymentDateTo = ''

    condition.keyword = ''
  }

  return {
    condition,
    fields,
    filteredReports,
    clear,
  }
}
