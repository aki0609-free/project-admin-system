export const queryKeys = {
  reportMasters: {
    all: ['reportMasters'] as const,
    list: (params?: unknown) => ['reportMasters', 'list', params ?? null] as const,
    detail: (id: number | null) => ['reportMasters', 'detail', id] as const,
  },

  reportSignatures: {
    all: ['reportSignatures'] as const,
    list: (params?: unknown) => ['reportSignatures', 'list', params ?? null] as const,
    detail: (id: number | null) => ['reportSignatures', 'detail', id] as const,
    byReportMaster: (reportMasterId: number | null) =>
      ['reportSignatures', 'byReportMaster', reportMasterId] as const,
  },

  reportHistories: {
    all: ['reportHistories'] as const,
    list: (params?: unknown) => ['reportHistories', 'list', params ?? null] as const,
    detail: (id: number | null) => ['reportHistories', 'detail', id] as const,
    byReportMaster: (reportMasterId: number | null) =>
      ['reportHistories', 'byReportMaster', reportMasterId] as const,
  },

  reportTemplates: {
    all: ['reportTemplates'] as const,
    list: () => ['reportTemplates', 'list'] as const,
  },

  reportExecutions: {
    all: ['reportExecutions'] as const,
    prepare: (reportCode: string | null) =>
      ['reportExecutions', 'prepare', reportCode] as const,
    preview: (reportCode: string | null, executionId: string | null) =>
      ['reportExecutions', 'preview', reportCode, executionId] as const,
    download: (
      reportCode: string | null,
      executionId: string | null,
      format?: string | null,
    ) => ['reportExecutions', 'download', reportCode, executionId, format ?? null] as const,
  },
  
  reportTestParamTemplates: {
    all: ['reportTestParamTemplates'] as const,
    list: (reportCode: string | null) =>
      ['reportTestParamTemplates', 'list', reportCode] as const,
    detail: (id: number | null) =>
      ['reportTestParamTemplates', 'detail', id] as const,
  },
} as const