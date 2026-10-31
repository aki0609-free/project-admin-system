import { get } from '@/shared/api/http'
import type { PaymentCycle } from '@/features/employees/types/employeeApiTypes'

export type DailyReportDateSuggestion = {
  workDate: string
  paymentDate: string
}

export const fetchDailyReportDateSuggestion = (
  businessDate: string,
  workDate: string | null,
  paymentCycle: PaymentCycle,
) => get<DailyReportDateSuggestion>('/api/daily-reports/date-suggestion', {
  params: { query: { businessDate, workDate, paymentCycle } },
})
