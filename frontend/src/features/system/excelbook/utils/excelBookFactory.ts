import type {
  ExcelBookMasterForm,
  ExcelBookMasterRequest,
  ExcelBookMasterResponse,
} from '../types/excelBookTypes'

export function createEmptyExcelBookForm(): ExcelBookMasterForm {
  return {
    id: -1,
    bookCode: '',
    bookName: '',
    templateFilePath: '',
    outputFilePath: '',
    sourceType: 'SNAPSHOT',
    sourceName: '',
    templateSheetName: 'TEMPLATE',
    activeFlag: true,
    _isNew: true,
  }
}

export function toExcelBookForm(
  response: ExcelBookMasterResponse,
): ExcelBookMasterForm {
  return {
    ...response,
    _isNew: false,
  }
}

export function toExcelBookRequest(
  form: ExcelBookMasterForm,
): ExcelBookMasterRequest {
  return {
    bookCode: form.bookCode,
    bookName: form.bookName,
    templateFilePath: form.templateFilePath,
    outputFilePath: form.outputFilePath,
    sourceType: form.sourceType,
    sourceName: form.sourceName,
    templateSheetName: form.templateSheetName || 'TEMPLATE',
    activeFlag: form.activeFlag,
  }
}