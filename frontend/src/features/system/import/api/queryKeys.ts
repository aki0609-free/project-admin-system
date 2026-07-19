export const queryKeys = {
  importTargets: {
    all: ['importTargets'] as const,
    list: ['importTargets', 'list'] as const,
    active: ['importTargets', 'active'] as const,
    detail: (id: number | null) => ['importTargets', 'detail', id] as const,
  },

  importHistories: {
    all: ['importHistories'] as const,
    list: ['importHistories', 'list'] as const,
    errors: (historyId: number | null) =>
      ['importHistories', 'errors', historyId] as const,
  },
}