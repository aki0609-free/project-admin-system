export type ExcelBookSourceType = 'SNAPSHOT'

export type ExcelBookMasterResponse = {
  id: number
  bookCode: string
  bookName: string
  templateFilePath: string
  outputFilePath: string
  sourceType: ExcelBookSourceType
  sourceName: string
  templateSheetName: string
  activeFlag: boolean
}

export type ExcelBookMasterRequest = {
  bookCode: string
  bookName: string
  templateFilePath: string
  outputFilePath: string
  sourceType: ExcelBookSourceType
  sourceName: string
  templateSheetName: string
  activeFlag: boolean
}

export type ExcelBookMasterForm = ExcelBookMasterResponse & {
  _isNew?: boolean
}

export type ExcelBookUpdateRequest = {
  targetMonth: string
}