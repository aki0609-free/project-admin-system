import { del, get, post, put } from '@/shared/api/http'
import type {
  GenericMasterDefinition,
  GenericMasterDefinitionSaveRequest,
  GenericMasterRecord,
  GenericMasterRecordSaveRequest,
} from '../types/genericMasterTypes'

const definitionPath = '/api/system/generic-masters/definitions'
const businessPath = '/api/admin/business-settings/generic-masters'

export const getGenericMasterDefinitions = () =>
  get<GenericMasterDefinition[]>(definitionPath)

export const createGenericMasterDefinition = (
  request: GenericMasterDefinitionSaveRequest,
) => post<GenericMasterDefinition, GenericMasterDefinitionSaveRequest>(definitionPath, request)

export const updateGenericMasterDefinition = (
  id: number,
  request: GenericMasterDefinitionSaveRequest,
) => put<GenericMasterDefinition, GenericMasterDefinitionSaveRequest>(
  `${definitionPath}/${id}`,
  request,
)

export const publishGenericMasterDefinition = (id: number) =>
  post<GenericMasterDefinition>(`${definitionPath}/${id}/publish`)

export const deleteGenericMasterDefinition = (id: number) =>
  del<undefined>(`${definitionPath}/${id}`)

export const getPublishedBusinessGenericMasters = () =>
  get<GenericMasterDefinition[]>(businessPath)

export const getGenericMasterRecords = (definitionId: number) =>
  get<GenericMasterRecord[]>(`${businessPath}/${definitionId}/records`)

export const createGenericMasterRecord = (
  definitionId: number,
  request: GenericMasterRecordSaveRequest,
) => post<GenericMasterRecord, GenericMasterRecordSaveRequest>(
  `${businessPath}/${definitionId}/records`,
  request,
)

export const updateGenericMasterRecord = (
  definitionId: number,
  recordId: number,
  request: GenericMasterRecordSaveRequest,
) => put<GenericMasterRecord, GenericMasterRecordSaveRequest>(
  `${businessPath}/${definitionId}/records/${recordId}`,
  request,
)

export const deleteGenericMasterRecord = (definitionId: number, recordId: number) =>
  del<undefined>(`${businessPath}/${definitionId}/records/${recordId}`)
