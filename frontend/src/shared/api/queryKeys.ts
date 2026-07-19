export const queryKeys = {
    users: {
        all: ['users'] as const,
        list: (params?: any) => ['users', 'list', params] as const,
        detail: (id: string) => ['users', 'details', id] as const,
    },
}