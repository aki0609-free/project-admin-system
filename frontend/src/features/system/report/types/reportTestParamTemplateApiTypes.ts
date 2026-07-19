export type ReportTestParamTemplateResponse = {
  id: number
  reportCode: string
  templateName: string
  jsonText: string
  defaultFlag: boolean
  activeFlag: boolean
}

export type ReportTestParamTemplateSaveRequest = {
  reportCode: string
  templateName: string
  jsonText: string
  defaultFlag: boolean
  activeFlag: boolean
}