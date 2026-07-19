export const dailyPaymentQueryKeys = {
  all: ['daily-payments'] as const,

  list: (paymentDate: string) =>
    [...dailyPaymentQueryKeys.all, 'list', paymentDate] as const,

  detail: (id: number) =>
    [...dailyPaymentQueryKeys.all, 'detail', id] as const,

  summary: (paymentDate: string) =>
    [...dailyPaymentQueryKeys.all, 'summary', paymentDate] as const,

  printSummary: (paymentDate: string) =>
    [...dailyPaymentQueryKeys.all, 'print-summary', paymentDate] as const,
}