export const queryKeys = {
  backup: {
    all: ['backup'] as const,
    targets: ['backup', 'targets'] as const,
    detail: (id: number | null) => ['backup', 'targets', 'detail', id] as const,
    histories: ['backup', 'histories'] as const,
  },
} as const