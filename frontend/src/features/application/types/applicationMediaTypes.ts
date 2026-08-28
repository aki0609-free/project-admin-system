import type { ApplicationMediaListItem, CellBaseType } from './types'

export type FilterState = {
  mediaYearMonth: string
  mediaName: string
}

export type ApplicationMediaPivotRowBase = {
  mediaName: string
} & Record<string, string | number | null>

export type ApplicationMediaPivotRow = ApplicationMediaPivotRowBase & {
  id: number
}

export type ApplicationMediaLocalItem = ApplicationMediaListItem & CellBaseType
