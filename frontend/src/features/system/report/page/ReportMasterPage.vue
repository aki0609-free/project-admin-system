<script setup lang="ts">
import { ref } from 'vue'
import TabLayout from '@/shared/components/layout/tab_layout/TabLayout.vue'
import ListDetailPageLayout from '@/shared/templates/list-detail/ListDetailPageTemplate.vue'
import ReportAdminListTab from '@/features/system/report/components/ReportAdminListTab.vue'
import ReportTestPrintTab from '@/features/system/report/components/ReportTestPrintTab.vue'

const tabs = [
  { label: '一覧', value: 'list' },
  { label: '手動印刷', value: 'testPrint' },
]

const activeTab = ref('list')
</script>

<template>
  <ListDetailPageLayout
    title="帳票管理"
    description="PDF・CSV・EXCELを生成する帳票定義、署名、出力履歴を管理します。"
  >
    <TabLayout v-model="activeTab" :tabs="tabs">
      <template #default="{ active }">
        <ReportAdminListTab v-if="active === 'list'" />
        <ReportTestPrintTab v-else-if="active === 'testPrint'" />
      </template>
    </TabLayout>
  </ListDetailPageLayout>
</template>
