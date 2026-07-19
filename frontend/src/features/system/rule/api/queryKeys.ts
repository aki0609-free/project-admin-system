export const ruleQueryKeys = {
  rules: {
    all: ['rules'] as const,
    list: ['rules', 'list'] as const,
    active: ['rules', 'active'] as const,
    detail: (id: number | null) => ['rules', 'detail', id] as const,
  },
}