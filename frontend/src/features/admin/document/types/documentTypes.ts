export type DocumentArea =
  | 'GENERAL'
  | 'GENERATED_REPORTS'
  | 'BACKUPS'
  | 'TEMPLATES'

export type DocumentOperation =
  | 'READ'
  | 'SEARCH'
  | 'DETAILS'
  | 'DOWNLOAD'
  | 'CREATE_DIRECTORY'
  | 'UPLOAD'
  | 'COPY'
  | 'MOVE'
  | 'RENAME'
  | 'DELETE'

export interface DocumentAreaResponse {
  area: DocumentArea
  displayName: string
  allowedOperations: DocumentOperation[]
}
