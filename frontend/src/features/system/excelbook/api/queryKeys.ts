export const queryKeys = {
  excelBookMasters: {
    all: ['excelBookMasters'] as const,
    list: (params?: unknown) => ['excelBookMasters', 'list', params ?? null] as const,
    detail: (id: number | null) => ['excelBookMasters', 'detail', id] as const,
  },

  excelBooks: {
    all: ['excelBooks'] as const,
    update: (bookCode: string | null, targetMonth: string | null) =>
      ['excelBooks', 'update', bookCode, targetMonth] as const,
  },

  spreadsheetTemplates: {
    all: ['spreadsheetTemplates'] as const,
    detail: (masterId: number | null) =>
      ['spreadsheetTemplates', 'detail', masterId] as const,
  },

  dataSourceCatalogs: {
    active: ['excelBookDataSourceCatalogs', 'active'] as const,
  },
} as const
