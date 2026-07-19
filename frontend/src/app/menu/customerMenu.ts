
import CustomerMaster from '@/features/customer/pages/CustomerMaster.vue'
import type { MenuItem } from './types'
import CustomerTransactionPage from '@/features/customer/pages/CustomerTransactionPage.vue'

export const customerMenu: MenuItem = {
  title: '顧客管理',
  icon: 'mdi-account-check',
  children: [
    {
      title: '顧客情報',
      to: '/customer/information',
      component: CustomerMaster,
      resource: 'customer',
      action: 'view',
    },
    {
      title: '取引情報',
      to: '/customer/transaction',
      component: CustomerTransactionPage,
      resource: 'customer',
      action: 'view',
    },
  ],
}