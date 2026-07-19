export const queryKeys = {
  batch: {
    all: ['batch'] as const,

    definitions: {
      all: ['batch', 'definitions'] as const,
      list: () => ['batch', 'definitions', 'list'] as const,
      detail: (id: number | null) =>
        ['batch', 'definitions', 'detail', id] as const,
    },

    logs: {
      all: ['batch', 'logs'] as const,
      list: () => ['batch', 'logs', 'list'] as const,
      byJobCode: (jobCode: string | null) =>
        ['batch', 'logs', 'byJobCode', jobCode] as const,
    },
  },
} as const