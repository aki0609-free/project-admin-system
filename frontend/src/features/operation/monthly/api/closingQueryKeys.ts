export const queryKeys = {
  closings: {
    all: ['closings'] as const,

    summary: (targetMonth: string) =>
      [...queryKeys.closings.all, 'summary', targetMonth] as const,
  },
}