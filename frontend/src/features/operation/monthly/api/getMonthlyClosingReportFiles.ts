import { get } from '@/shared/api/http'
import type { MonthlyClosingReportFileResponse } from '../types/monthlyReportFileTypes'

export const getMonthlyClosingReportFiles = (
  targetMonth: string,
  closingVersion: number,
  reportCode: string,
) =>
  get<MonthlyClosingReportFileResponse[]>(
    '/api/operation/monthly/report-files',
    {
      params: {
        query: {
          targetMonth,
          closingVersion,
          reportCode,
        },
      },
    },
  )
