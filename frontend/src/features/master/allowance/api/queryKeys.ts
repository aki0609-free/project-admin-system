export const queryKeys = {
  allowances: {
    all: ['allowances'] as const,
    list: () => [...queryKeys.allowances.all, 'list'] as const,
    detail: (id: number) => [...queryKeys.allowances.all, 'detail', id] as const,
  },
}