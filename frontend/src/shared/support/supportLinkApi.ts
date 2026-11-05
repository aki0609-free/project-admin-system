import { get } from '@/shared/api/http'

export type SupportLinkSetting = {
  incidentReportUrl: string
  manualUrl: string
}

export const getExternalSupportLinks = () =>
  get<SupportLinkSetting>('/api/support-links')
