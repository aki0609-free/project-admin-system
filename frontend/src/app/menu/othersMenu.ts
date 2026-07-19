import DeductionPage from '@/features/master/deduction/page/DeductionPage.vue'
import type { MenuItem } from './types'

export const othersMenu: MenuItem = {
  title: 'その他',
  icon: 'mdi-dots-horizontal-circle',
  children: [
    {
      title: 'チャット',
      to: '/others/chat',
      component: DeductionPage,
      resource: 'others',
      action: 'view',
    },
  ],
}
