export const queryKeys = {
  dailyPreparations: {
    all: ['operation', 'daily-preparations'] as const,
    byTargetDate: (targetDate: string) =>
      ['operation', 'daily-preparations', targetDate] as const,
  },
}