export const queryKeys = {
  employees: {
    all: ['employees'] as const,
    list: () => ['employees', 'list'] as const,
    detail: (id: number | null) => ['employees', 'detail', id] as const,
    resignationConfiguration: () => ['employees', 'resignation-configuration'] as const,
    payrollItemSettingCatalog: () => ['employees', 'payroll-item-setting-catalog'] as const,
  },
  employeeWork: {
    all: ['employee-work'] as const,
    loans: {
      all: ['employee-work', 'loans'] as const,
      list: () => ['employee-work', 'loans', 'list'] as const,
      detail: (id: number | null) => ['employee-work', 'loans', 'detail', id] as const,
    },
    savings: {
      all: ['employee-work', 'savings'] as const,
      list: () => ['employee-work', 'savings', 'list'] as const,
      detail: (id: number | null) => ['employee-work', 'savings', 'detail', id] as const,
    },
    financeTransactions: {
      all: ['employee-work', 'finance-transactions'] as const,
      list: () => ['employee-work', 'finance-transactions', 'list'] as const,
    },
  },
} as const
