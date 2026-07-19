export const queryKeys = {
  applicants: {
    all: ['applicants'] as const,
    list: (params?: unknown) => ['applicants', 'list', params ?? null] as const,
    detail: (id: number) => ['applicants', 'detail', id] as const,
  },
  applicationMedias: {
    all: ['applicationMedias'] as const,
    list: (params?: unknown) => ['applicationMedias', 'list', params ?? null] as const,
    detail: (id: number) => ['applicationMedias', 'detail', id] as const,
    bulk: ['applicationMedias', 'bulk'] as const,
  },
  applicationMediaAi: {
    all: ['applicationMediaAi'] as const,
    analysis: (params?: unknown) => ['applicationMediaAi', 'analysis', params ?? null] as const,
  },
}