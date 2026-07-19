export const queryKeys = {
  dailyReports: {
    all: ['daily-reports'] as const,
    list: () => ['daily-reports', 'list'] as const,
    detail: (id: number) => ['daily-reports', 'detail', id] as const,
    inputItems: () => ['daily-reports', 'input-items'] as const,
    estimatedPayPreview: () => ['daily-reports', 'estimated-pay-preview'] as const,
    monthlyAttendance: (targetMonth: string) =>
      ['daily-report-monthly-attendance', targetMonth] as const,
  },

  dailyReportMissing: {
    all: ['daily-report-missing'] as const,
    list: (workDate: string) => ['daily-report-missing', workDate] as const,
  },
} as const