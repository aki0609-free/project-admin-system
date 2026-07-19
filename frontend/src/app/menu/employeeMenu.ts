import DailyReportPage from '@/features/dailyreport/page/DailyReportPage.vue'
import EmployeeLoanSavingsPage from '@/features/employees/pages/EmployeeLoanSavingsPage.vue'
import EmployeePage from '@/features/employees/pages/EmployeePage.vue'
import type { MenuItem } from './types'

export const employeeMenu: MenuItem = {
  title: '従業員管理',
  icon: 'mdi-card-account-details-outline',
  children: [
    {
      title: '従業員情報',
      to: '/employee/information',
      component: EmployeePage,
      resource: 'employee',
      action: 'view',
    },
    {
      title: '従業員貸付・貯蓄',
      to: '/employee/loan-savings',
      component: EmployeeLoanSavingsPage,
      resource: 'employee',
      action: 'view',
    },
  ],
}