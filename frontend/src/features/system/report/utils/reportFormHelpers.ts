import type { ReportParamFormItem } from '@/features/system/report/types/reportFormTypes'

export const resequenceReportParams = (
  params: ReportParamFormItem[],
): ReportParamFormItem[] =>
  params.map((param, index) => ({
    ...param,
    displayOrder: index + 1,
  }))