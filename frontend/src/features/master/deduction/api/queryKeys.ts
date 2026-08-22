export const queryKeys = {
  deductions: {
    all: ['deductions'] as const,
    list: () => [...queryKeys.deductions.all, 'list'] as const,
    detail: (id: number) => [...queryKeys.deductions.all, 'detail', id] as const,
    detailAt: (id: number, targetDate: string) =>
      [...queryKeys.deductions.detail(id), targetDate] as const,
  },
}
