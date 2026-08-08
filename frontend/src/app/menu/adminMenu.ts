import { defineAsyncComponent } from 'vue'
import DeductionPage from '@/features/master/deduction/page/DeductionPage.vue'
import { Role } from '@/shared/auth/types/types'
import type { MenuItem } from './types'

const DocumentManagementPage = defineAsyncComponent(
  () => import('@/features/admin/document/pages/DocumentManagementPage.vue'),
)
const BusinessSettingsPage = defineAsyncComponent(
  () => import('@/features/admin/business/pages/BusinessSettingsPage.vue'),
)

export const adminMenu: MenuItem = {
  title: '管理者メニュー',
  icon: 'mdi-security',
  children: [
    {
      title: '業務管理',
      to: '/admin/business-settings',
      component: BusinessSettingsPage,
      resource: 'admin',
      action: 'manage',
      roles: [Role.SYS_ADMIN],
    },
    {
      title: '承認管理',
      to: '/admin/approval',
      component: DeductionPage,
      resource: 'admin',
      action: 'view',
    },
    {
      title: '書類管理',
      to: '/admin/document',
      component: DocumentManagementPage,
      resource: 'admin',
      action: 'view',
      roles: [Role.SYS_ADMIN],
    },
  ],
}
