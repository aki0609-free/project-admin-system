import AuthView from '@/features/users/pages/AuthView.vue'
import UserView from '@/features/users/pages/UserView.vue'
import type { MenuItem } from './types'

export const userMenu: MenuItem = {
  title: 'ユーザ管理',
  icon: 'mdi-account-multiple',
  children: [
    {
      title: 'ユーザ一覧',
      to: '/user/users',
      component: UserView,
      resource: 'user',
      action: 'manage',
    },
    {
      title: '権限一覧',
      to: '/user/auth',
      component: AuthView,
      resource: 'user',
      action: 'manage',
    },
  ],
}