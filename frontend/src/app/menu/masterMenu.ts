import AllowancePage from '@/features/master/allowance/page/AllowancePage.vue'
import DeductionPage from '@/features/master/deduction/page/DeductionPage.vue'
import type { MenuItem } from './types'

export const masterMenu: MenuItem = {
  title: 'マスター管理',
  icon: 'mdi-database',
  children: [
    {
      title: '控除マスターデータ',
      to: '/master/deduction',
      component: DeductionPage,
      resource: 'master',
      action: 'view',
    },
    {
      title: '手当マスターデータ',
      to: '/master/allowance',
      component: AllowancePage,
      resource: 'master',
      action: 'view',
    },
  ],
}