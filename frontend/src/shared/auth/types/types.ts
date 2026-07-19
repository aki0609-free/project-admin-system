export enum Role {
  OPERATOR = "OPERATOR",
  MANAGER = "MANAGER",
  ADMIN = "ADMIN",
  SYS_ADMIN = "SYS_ADMIN",
}

export const RoleLevel = {
    OPERATOR: 1,
    MANAGER: 2,
    ADMIN: 3,
    SYS_ADMIN: 4,
} as const

export interface AuthUser {
    id: number,
    tenantId: string,
    username: string,
    roles: Role[],
    permissions: string[]
}

export interface LoginResponse {
    user: AuthUser,
    accessToken: string,
    refreshToken: string
}