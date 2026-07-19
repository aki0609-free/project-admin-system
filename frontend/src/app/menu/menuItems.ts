import { dashboardMenu } from './dashboardMenu'
import { masterMenu } from './masterMenu'
import { employeeMenu } from './employeeMenu'
import { operationMenu } from './operationMenu'
import { analysisMenu } from './analysisMenu'
import { userMenu } from './userMenu'
import { systemMenu } from './systemMenu'
import type { MenuItem } from './types'
import { customerMenu } from './customerMenu'
import { othersMenu } from './othersMenu'
import { adminMenu } from './adminMenu'

export const menuItems: MenuItem[] = [
  dashboardMenu,
  masterMenu,
  customerMenu,
  employeeMenu,
  operationMenu,
  analysisMenu,
  othersMenu,
  adminMenu,
  userMenu,
  systemMenu,
]