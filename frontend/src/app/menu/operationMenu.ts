import DailyPreparationPage from '@/features/operation/preparation/pages/DailyPreparationPage.vue'
import type { MenuItem } from './types'
import MonthlyOperationPage from '@/features/operation/monthly/pages/MonthlyOperationPage.vue'
import DailyOperationPage from '@/features/operation/daily/pages/DailyOperationPage.vue'
import DailyReportPage from '@/features/dailyreport/page/DailyReportPage.vue'
import BookOperationPage from '@/features/operation/book/pages/BookOperationPage.vue'

export const operationMenu: MenuItem = {
  title: '締め処理',
  icon: 'mdi-calendar-star',
  children: [
    {
      title: '日報入力',
      to: '/operation/daily-reports',
      component: DailyReportPage,
      resource: 'operation',
      action: 'view',
    },
    {
      title: '翌日準備',
      to: '/operation/daily/preparation',
      component: DailyPreparationPage,
      resource: 'operation',
      action: 'view',
    },
    {
      title: '日次管理',
      to: '/operation/daily',
      component: DailyOperationPage,
      resource: 'operation',
      action: 'view',
    },
    {
      title: '月次管理',
      to: '/operation/monthly',
      component: MonthlyOperationPage,
      resource: 'operation',
      action: 'view',
    },
    {
      title: '台帳',
      to: '/operation/book',
      component: BookOperationPage,
      resource: 'operation',
      action: 'view',
    },
  ],
}