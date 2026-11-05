import { get, postForm, put } from '@/shared/api/http'
import type {
  MonthlyClosingOutputSaveRequest,
  MonthlyClosingOutputSetting,
  PreviewReportSetting,
  PreviewReportSettingSaveRequest,
} from '../types/reportOperationSettingTypes'

const basePath = '/api/admin/reports/operation-settings'

export const getClosingOutputs = () =>
  get<MonthlyClosingOutputSetting[]>(`${basePath}/closing-outputs`)

export const saveClosingOutputs = (requests: MonthlyClosingOutputSaveRequest[]) =>
  put<MonthlyClosingOutputSetting[], MonthlyClosingOutputSaveRequest[]>(
    `${basePath}/closing-outputs`,
    requests,
  )

export const getPreviewReportSettings = () =>
  get<PreviewReportSetting[]>(`${basePath}/preview-reports`)

export const savePreviewReportSetting = (
  request: PreviewReportSettingSaveRequest,
  template: File | null,
) => {
  const formData = new FormData()
  formData.append('definition', new Blob([JSON.stringify(request)], { type: 'application/json' }))
  if (template) formData.append('template', template)
  return postForm<PreviewReportSetting>(`${basePath}/preview-reports`, formData)
}
