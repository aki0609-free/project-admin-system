export const queryKeys = {
  notice: {
    all: ['notice'] as const,
    list: ['notice', 'list'] as const,
    calendar: (from: string, to: string) =>
      ['notice', 'calendar', from, to] as const,
  },
} as const