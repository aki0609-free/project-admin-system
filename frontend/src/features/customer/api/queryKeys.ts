export const queryKeys = {
  customers: {
    all: ['customers'] as const,

    list: () =>
      [...queryKeys.customers.all, 'list'] as const,

    detail: (id: number) =>
      [...queryKeys.customers.all, 'detail', id] as const,

    options: () =>
      [...queryKeys.customers.all, 'options'] as const,

    transactions: (customerId: number) =>
      [
        ...queryKeys.customers.all,
        'transactions',
        customerId,
      ] as const,

    billingRates: (customerId: number) =>
      [
        ...queryKeys.customers.all,
        'billingRates',
        customerId,
      ] as const,
  },
} as const