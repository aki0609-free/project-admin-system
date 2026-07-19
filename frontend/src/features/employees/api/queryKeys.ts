export const queryKeys = {
  employees: {
    all: ['employees'] as const,
    list: () => ['employees', 'list'] as const,
    detail: (id: number | null) => ['employees', 'detail', id] as const,
    resignationChecklist: () => ['employees', 'resignation-checklist'] as const,
  },
  employeeWork: {
    all: ['employee-work'] as const,
    timesheets: {
      all: ['employee-work', 'timesheets'] as const,
      list: () => ['employee-work', 'timesheets', 'list'] as const,
      detail: (id: number | null) => ['employee-work', 'timesheets', 'detail', id] as const,
    },
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
  },
} as const