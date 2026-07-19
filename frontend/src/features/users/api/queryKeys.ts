export const queryKeys = {
  users: {
    all: ['users'] as const,
    list: (params?: any) => ['users', 'list', params] as const,
    detail: (id: string | number) => ['users', 'detail', id] as const,
  },
  roles: {
    all: ['roles'] as const,
    list: () => ['roles', 'list'] as const,
    detail: (id: string | number) => ['roles', 'detail', id] as const,
  },
  permissions: {
    all: ['permissions'] as const,
    list: () => ['permissions', 'list'] as const,
    detail: (id: string | number) => ['permissions', 'detail', id] as const,
  },
}