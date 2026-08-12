import { get } from '@/shared/api/http'
import type { MonthlyClosingReportFileResponse } from '../types/monthlyReportFileTypes'

export const getMonthlyClosingReportFiles = (
  targetMonth: string,
  closingVersion: number | null,
  reportCode: string,
) =>
  get<MonthlyClosingReportFileResponse[]>(
    '/api/operation/monthly/report-files',
    {
      params: {
        query: {
          targetMonth,
          ...(closingVersion ? { closingVersion } : {}),
          reportCode,
        },
      },
    },
  )
