import type { Component } from 'vue'
import type { Role } from '@/shared/auth/types/types'

export interface MenuItem {
  title: string
  icon?: string
  to?: string
  children?: MenuItem[]
  component?: Component
  resource?: string
  action?: string
  roles?: Role[]
}
