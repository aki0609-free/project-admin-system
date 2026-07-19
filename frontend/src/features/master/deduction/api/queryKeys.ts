export const queryKeys = {
  deductions: {
    all: ['deductions'] as const,
    list: () => [...queryKeys.deductions.all, 'list'] as const,
    detail: (id: number) => [...queryKeys.deductions.all, 'detail', id] as const,
  },
}