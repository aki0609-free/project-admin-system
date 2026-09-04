import { get } from '@/shared/api/http'
import type { DailyReportPreparationDefaultResponse } from '../types/dailyReportApiTypes'

export const fetchDailyReportPreparationDefaults = async (
  workDate: string,
  employeeId: number,
): Promise<DailyReportPreparationDefaultResponse> =>
  await get<DailyReportPreparationDefaultResponse>(
    '/api/daily-reports/preparation-defaults',
    {
      params: {
        query: {
          workDate,
          employeeId,
        },
      },
    },
  )
