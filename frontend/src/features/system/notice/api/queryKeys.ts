export const queryKeys = {
  noticeRules: {
    all: ['noticeRules'] as const,
    list: () => ['noticeRules', 'list'] as const,
    detail: (id: number | null) => ['noticeRules', 'detail', id] as const,
  },
} as const