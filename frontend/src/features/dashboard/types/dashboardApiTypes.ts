import type {
  NoticeContentFormat,
  NoticeSourceType,
} from './dashboardTypes'

export type DashboardNoticeType =
  | 'important'
  | 'warning'
  | 'info'

export type DashboardNoticeResponse = {
  id: number
  title: string
  start: string
  end: string
  type: DashboardNoticeType
  color?: string | null
  content?: string | null
  contentFormat?: NoticeContentFormat | null
  sourceType?: NoticeSourceType | null
  sourceRuleCode?: string | null
  pinnedFlag?: boolean
  activeFlag?: boolean
}