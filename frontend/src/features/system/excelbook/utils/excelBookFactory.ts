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
    sourceType: 'SNAPSHOT',
    layoutType: 'REPEATING_ROW',
    rendererKey: 'REPEATING_ROW',
    selection: {
      mode: 'NONE',
      dataSourceCode: null,
      valueColumn: null,
      displayColumns: [],
      allowSelectAll: false,
      generationUnit: 'ONE_FILE',
    },
    print: {
      paperSize: 'A4',
      orientation: 'PORTRAIT',
      fitToOnePage: false,
    },
    dataSourceCode: '',
    templateSheetName: 'TEMPLATE',
    templateRequired: true,
    activeFlag: true,
    variableMappings: [],
    _isNew: true,
  }
}

export function toExcelBookForm(
  response: ExcelBookMasterResponse,
): ExcelBookMasterForm {
  return {
    ...response,
    variableMappings: response.variableMappings.map(mapping => ({
      ...mapping,
    })),
    _isNew: false,
  }
}

export function toExcelBookRequest(
  form: ExcelBookMasterForm,
): ExcelBookMasterRequest {
  return {
    bookCode: form.bookCode,
    bookName: form.bookName,
    sourceType: form.sourceType,
    layoutType: form.layoutType,
    rendererKey: form.rendererKey,
    selection: {
      ...form.selection,
      displayColumns: [...form.selection.displayColumns],
    },
    print: { ...form.print },
    dataSourceCode: form.dataSourceCode,
    templateSheetName: form.templateSheetName || 'TEMPLATE',
    activeFlag: form.activeFlag,
    variableMappings: form.variableMappings.map(
      ({ variableKey, sourceColumn, scope, dataType, orderNo }) => ({
        variableKey,
        sourceColumn,
        scope,
        dataType,
        orderNo,
      }),
    ),
  }
}
