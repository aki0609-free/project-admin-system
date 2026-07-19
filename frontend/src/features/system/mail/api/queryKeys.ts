export const queryKeys = {
  mail: {
    all: ['mail'] as const,

    recipientGroups: {
      all: ['mail', 'recipientGroups'] as const,
      list: () => ['mail', 'recipientGroups', 'list'] as const,
      detail: (id: number | null) =>
        ['mail', 'recipientGroups', 'detail', id] as const,
    },

    templates: {
      all: ['mail', 'templates'] as const,
      list: () => ['mail', 'templates', 'list'] as const,
      detail: (id: number | null) =>
        ['mail', 'templates', 'detail', id] as const,
    },

    queues: {
      all: ['mail', 'queues'] as const,
      list: () => ['mail', 'queues', 'list'] as const,
    },
  },
} as const
