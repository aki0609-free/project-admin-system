<script setup lang="ts">
import { ref } from 'vue'
import TabLayout from '@/shared/components/layout/tab_layout/TabLayout.vue'
import ReportAdminListTab from '@/features/system/report/components/ReportAdminListTab.vue'
import ReportTestPrintTab from '@/features/system/report/components/ReportTestPrintTab.vue'

const tabs = [
  { label: '一覧', value: 'list' },
  { label: '手動印刷', value: 'testPrint' },
]

const activeTab = ref('list')
</script>

<template>
  <div class="report-admin-page">
    <div class="page-header">
      <h2>帳票管理</h2>
      <p class="page-description">
        帳票PDF・CSV・EXCELの生成する定義を管理します。
      </p>
    </div>

    <TabLayout v-model="activeTab" :tabs="tabs">
      <template #default="{ active }">
        <ReportAdminListTab v-if="active === 'list'" />
        <ReportTestPrintTab v-else-if="active === 'testPrint'" />
      </template>
    </TabLayout>
  </div>
</template>

<style scoped>
.page-header {
  display: grid;
  gap: 6px;
}

.page-description {
  margin: 0;
  color: #64748b;
}

.report-admin-page {
  display: grid;
  gap: 12px;
}
</style>
