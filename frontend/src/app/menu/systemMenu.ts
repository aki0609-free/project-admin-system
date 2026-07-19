import BackupPage from '@/features/system/backup/page/BackupPage.vue'
import BatchPage from '@/features/system/batch/page/BatchPage.vue'
import ImportPage from '@/features/system/import/page/ImportPage.vue'
import MailPage from '@/features/system/mail/page/MailPage.vue'
import NoticeRulePage from '@/features/system/notice/pages/NoticeRulePage.vue'
import ReportMasterPage from '@/features/system/report/page/ReportMasterPage.vue'
import type { MenuItem } from './types'
import RuleManagementPage from '@/features/system/rule/pages/RuleManagementPage.vue'
import ExcelBookMasterPage from '@/features/system/excelbook/pages/ExcelBookMasterPage.vue'

export const systemMenu: MenuItem = {
  title: 'システム運用',
  icon: 'mdi-cog-outline',
  children: [
    {
      title: 'お知らせ管理',
      to: '/system/notice',
      component: NoticeRulePage,
      resource: 'system',
      action: 'manage',
    },
    {
      title: 'ルール管理',
      to: '/system/rule',
      component: RuleManagementPage,
      resource: 'system',
      action: 'manage',
    },
    {
      title: '台帳管理',
      to: '/system/excelbook',
      component: ExcelBookMasterPage,
      resource: 'system',
      action: 'manage',
    },
    {
      title: 'バッチ処理',
      to: '/system/batch',
      component: BatchPage,
      resource: 'system',
      action: 'manage',
    },
    {
      title: '外部データ取込',
      to: '/system/import',
      component: ImportPage,
      resource: 'system',
      action: 'manage',
    },
    {
      title: 'バックアップ',
      to: '/system/backup',
      component: BackupPage,
      resource: 'system',
      action: 'manage',
    },
    {
      title: 'メール管理',
      to: '/system/mail',
      component: MailPage,
      resource: 'system',
      action: 'manage',
    },
    {
      title: '帳票管理',
      to: '/system/report',
      component: ReportMasterPage,
      resource: 'system',
      action: 'manage',
    },
  ],
}