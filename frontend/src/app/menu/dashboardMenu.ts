import Dashboard from '@/features/dashboard/page/Dashboard.vue'
import type { MenuItem } from './types'

export const dashboardMenu: MenuItem = {
  title: 'ダッシュボード',
  icon: 'mdi-view-dashboard',
  to: '/',
  component: Dashboard,
  resource: 'dashboard',
  action: 'view',
}