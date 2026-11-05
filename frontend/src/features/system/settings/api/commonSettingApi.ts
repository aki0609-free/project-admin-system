import { del, get, post, put } from '@/shared/api/http'

import type {
  SystemSettingItem,
  SystemSettingDefinition,
  SystemSettingDefinitionCreateRequest,
  SystemSettingDefinitionUpdateRequest,
  SystemSettingValueSaveRequest,
} from '../types/commonSettingTypes'

const basePath = '/api/system/common-settings'

export const getSystemSettings = () => get<SystemSettingItem[]>(basePath)

export const saveSystemSettings = (request: SystemSettingValueSaveRequest) =>
  put<SystemSettingItem[], SystemSettingValueSaveRequest>(`${basePath}/values`, request)

export const getSystemSettingDefinitions = () =>
  get<SystemSettingDefinition[]>(`${basePath}/definitions`)

export const createSystemSettingDefinition = (request: SystemSettingDefinitionCreateRequest) =>
  post<SystemSettingDefinition, SystemSettingDefinitionCreateRequest>(
    `${basePath}/definitions`,
    request,
  )

export const updateSystemSettingDefinition = (
  id: number,
  request: SystemSettingDefinitionUpdateRequest,
) => put<SystemSettingDefinition, SystemSettingDefinitionUpdateRequest>(
  `${basePath}/definitions/${id}`,
  request,
)

export const deleteSystemSettingDefinition = (id: number) =>
  del<unknown>(`${basePath}/definitions/${id}`)
