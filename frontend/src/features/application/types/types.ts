// Base Type
export type CellBaseType = {
  isCreated: boolean
  isUpdated: boolean
  isDeleted: boolean
}
// ApplicationMedia API Type
export type ApplicationMediaListItem = {
  id: number
  mediaName: string
  mediaArea: string | null
  mediaSlots: number | null
  mediaYearMonth: string | null
  cost: number | null
  hires: number | null
  unitPrice: number | null
}

export type ApplicationMediaDetail = {
  id: number
  mediaName: string
  mediaArea: string | null
  mediaSlots: number | null
  mediaYearMonth: string | null
  cost: number | null
  hires: number | null
  unitPrice: number | null
}

export type ApplicationMediaEditForm = {
  id: number | null
  mediaName: string
  mediaArea: string
  mediaSlots: number | null
  mediaYearMonth: string
  cost: number | null
}

export type ApplicationMediaCreateRequest = Omit<ApplicationMediaEditForm, 'id'>

export type ApplicationMediaUpdateRequest = Omit<ApplicationMediaEditForm, 'id'> & {
  id: number
}

export type ApplicationMediaBulkCreateItem = {
  mediaName: string
  mediaArea: string | null
  mediaSlots: number | null
  mediaYearMonth: string | null
  cost: number | null
}

export type ApplicationMediaBulkUpdateItem = {
  id: number
  mediaName: string
  mediaArea: string | null
  mediaSlots: number | null
  mediaYearMonth: string | null
  cost: number | null
}

export type ApplicationMediaBulkSaveRequest = {
  created: ApplicationMediaBulkCreateItem[]
  updated: ApplicationMediaBulkUpdateItem[]
  deletedIds: number[]
}