export type UserListItem = {
  id: number
  username: string
  enabled: boolean
  roles: string[]
}

export type Permission = {
  id: number
  name: string
  label: string
}

export type RoleDetail = {
  id: number
  name: string
  permissions: Permission[]
}

export type UserDetail = {
  id: number
  username: string
  enabled: boolean
  roles: RoleDetail[]
}

export type UserCreateForm = {
  username: string
  password: string
  enabled: boolean
  roles: string[]
}

export type UserUpdateForm = {
  id: number
  username: string
  password?: string
  enabled: boolean
  roles: string[]
}

export type UserEditForm = {
  id: number | null
  username: string
  password: string
  enabled: boolean
  roles: string[]
}

export type RoleOption = {
  title: string
  value: string
}

export type RoleEditForm = {
  id: number | null
  name: string
  permissionIds: number[]
}

export type RoleCreateForm = {
  name: string
  permissionIds: number[]
}

export type RoleUpdateForm = {
  id: number
  name: string
  permissionIds: number[]
}
