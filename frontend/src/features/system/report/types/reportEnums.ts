export type ReportPreProcessType =
  | 'NONE'
  | 'SQL'
  | 'PROCEDURE'

export type ReportCleanupType =
  | 'NONE'
  | 'SQL'
  | 'PROCEDURE'

export type ReportLayoutType =
  | 'SINGLE'
  | 'MULTI'

export type ReportOutputFormat =
  | 'PDF'
  | 'CSV'
  | 'EXCEL'

export type ReportParamType =
  | 'STRING'
  | 'LONG'
  | 'DATE'
  | 'BOOLEAN'
  | 'DECIMAL'
  | 'DATETIME'

export type ReportParamControlType =
  | 'TEXT'
  | 'NUMBER'
  | 'DATE'
  | 'DATETIME'
  | 'CHECKBOX'
  | 'SELECT'
  | 'MULTI_SELECT'
  | 'TEXTAREA'

export type ReportSignatureType =
  | 'STAMP'
  | 'HANDWRITTEN'
  | 'NAME_SEAL'

export type ReportHistoryStatus =
  | 'SUCCESS'
  | 'FAILED'
  | 'PROCESSING'

  export type ReportStorageType =
  | 'LOCAL'
  | 'S3'