import ApplicantView from '@/features/application/pages/ApplicantView.vue'
import ApplicationMediaView from '@/features/application/pages/ApplicationMediaView.vue'
import type { MenuItem } from './types'

export const analysisMenu: MenuItem = {
  title: 'HR分析',
  icon: 'mdi-data-matrix',
  children: [
    {
      title: '応募者データ分析',
      to: '/application/applicant',
      component: ApplicantView,
      resource: 'application',
      action: 'view',
    },
    {
      title: '応募媒体データ分析',
      to: '/application/media',
      component: ApplicationMediaView,
      resource: 'application',
      action: 'view',
    },
  ],
}