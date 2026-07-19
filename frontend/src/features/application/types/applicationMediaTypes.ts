import { ApplicationMediaListItem, CellBaseType } from "./types"

export type FilterState = {
  mediaYearMonth: string
  mediaName: string
}

export type ApplicationMediaPivotRowBase = {
  mediaName: string
} & Record<string, any>

export type ApplicationMediaPivotRow = ApplicationMediaPivotRowBase & {
  id: number
}

export type ApplicationMediaLocalItem = ApplicationMediaListItem & CellBaseType
