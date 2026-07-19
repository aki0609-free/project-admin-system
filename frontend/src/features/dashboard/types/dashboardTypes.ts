export type NoticeType = 'IMPORTANT' | 'WARNING' | 'INFO'
export type NoticeContentFormat = 'PLAIN_TEXT' | 'HTML' | 'MARKDOWN'
export type NoticeSourceType = 'MANUAL' | 'AUTO'

export type NoticeResponse = {
  id: number
  title: string
  start: string
  end: string
  type: NoticeType
  color: string
  contentFormat: NoticeContentFormat
  content: string | null
  sourceType: NoticeSourceType
  sourceRuleCode: string | null
  pinnedFlag: boolean
  activeFlag: boolean
}

export type NoticeCreateRequest = {
  title: string
  start: string
  end: string
  type: NoticeType
  color: string
  contentFormat: NoticeContentFormat
  content: string | null
  pinnedFlag: boolean
  activeFlag: boolean
}