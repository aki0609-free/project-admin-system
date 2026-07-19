import DeductionPage from '@/features/master/deduction/page/DeductionPage.vue'
import type { MenuItem } from './types'

export const adminMenu: MenuItem = {
  title: '管理者メニュー',
  icon: 'mdi-security',
  children: [
    {
      title: '承認管理',
      to: '/admin/approval',
      component: DeductionPage,
      resource: 'admin',
      action: 'view',
    },
    {
      title: '書類閲覧',
      to: '/admin/document',
      component: DeductionPage,
      resource: 'admin',
      action: 'view',
    },
  ],
}
