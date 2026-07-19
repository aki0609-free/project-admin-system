import { Role } from '@/shared/auth/types/types'

export const PermissionResource = {
  DASHBOARD: 'dashboard',
  EMPLOYEE: 'employee',
  MASTER: 'master',
  OPERATION: 'operation',
  APPLICATION: 'application',
  SYSTEM: 'system',
  USER: 'user',
  AUDIT_LOG: 'auditLog',
} as const

export const PermissionAction = {
  VIEW: 'view',
  MANAGE: 'manage',
} as const

export type PermissionResource =
  typeof PermissionResource[keyof typeof PermissionResource]

export type PermissionAction =
  typeof PermissionAction[keyof typeof PermissionAction]

export type PermissionKey = `${PermissionResource}:${PermissionAction}`

type PermissionMap = Record<
  PermissionResource,
  Partial<Record<PermissionAction, Role[]>>
>

export const permissionMap: PermissionMap = {
  [PermissionResource.DASHBOARD]: {
    [PermissionAction.VIEW]: [
      Role.SYS_ADMIN,
      Role.ADMIN,
      Role.MANAGER,
      Role.OPERATOR,
    ],
  },

  [PermissionResource.EMPLOYEE]: {
    [PermissionAction.VIEW]: [
      Role.SYS_ADMIN,
      Role.ADMIN,
      Role.MANAGER,
    ],
  },

  [PermissionResource.MASTER]: {
    [PermissionAction.VIEW]: [
      Role.SYS_ADMIN,
      Role.ADMIN,
      Role.MANAGER,
    ],
  },

  [PermissionResource.OPERATION]: {
    [PermissionAction.VIEW]: [
      Role.SYS_ADMIN,
      Role.ADMIN,
      Role.MANAGER,
    ],
  },

  [PermissionResource.APPLICATION]: {
    [PermissionAction.VIEW]: [
      Role.SYS_ADMIN,
      Role.ADMIN,
      Role.MANAGER,
    ],
  },

  [PermissionResource.SYSTEM]: {
    [PermissionAction.MANAGE]: [
      Role.SYS_ADMIN,
      Role.ADMIN,
    ],
  },

  [PermissionResource.USER]: {
    [PermissionAction.VIEW]: [
      Role.SYS_ADMIN,
      Role.ADMIN,
    ],
    [PermissionAction.MANAGE]: [
      Role.SYS_ADMIN,
    ],
  },

  [PermissionResource.AUDIT_LOG]: {
    [PermissionAction.VIEW]: [
      Role.SYS_ADMIN,
      Role.ADMIN,
    ],
  },
}

export const actionLabelMap: Record<PermissionAction, string> = {
  [PermissionAction.VIEW]: '閲覧',
  [PermissionAction.MANAGE]: '管理',
}

export const resourceLabelMap: Record<PermissionResource, string> = {
  [PermissionResource.DASHBOARD]: 'ダッシュボード',
  [PermissionResource.EMPLOYEE]: '従業員管理',
  [PermissionResource.MASTER]: 'マスター管理',
  [PermissionResource.OPERATION]: '締め処理',
  [PermissionResource.APPLICATION]: 'HR分析',
  [PermissionResource.SYSTEM]: 'システム運用',
  [PermissionResource.USER]: 'ユーザー管理',
  [PermissionResource.AUDIT_LOG]: '監査ログ',
}

export const createPermissionKey = (
  resource: PermissionResource | string,
  action: PermissionAction | string,
) => `${resource}:${action}`

export const permissionGroups = Object.entries(permissionMap).map(
  ([resource, actions]) => ({
    label: resourceLabelMap[resource as PermissionResource] ?? resource,
    permissions: Object.keys(actions).map((action) => ({
      title: actionLabelMap[action as PermissionAction] ?? action,
      value: createPermissionKey(resource, action),
    })),
  }),
)