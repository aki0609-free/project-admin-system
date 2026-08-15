import { get } from '@/shared/api/http'

export type ExternalSupportLinkSetting = {
  incidentReportUrl: string
  manualUrl: string
}

export const getExternalSupportLinks = () =>
  get<ExternalSupportLinkSetting>('/api/support-links')
