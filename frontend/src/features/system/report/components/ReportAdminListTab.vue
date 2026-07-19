<script setup lang="ts">
import { ref } from 'vue'
import TabLayout from '@/shared/components/layout/tab_layout/TabLayout.vue'
import ReportMasterTableTab from '@/features/system/report/components/ReportMasterTableTab.vue'
import ReportSignatureTableTab from '@/features/system/report/components/ReportSignatureTableTab.vue'
import ReportHistoryTableTab from '@/features/system/report/components/ReportHistoryTableTab.vue'

const tabs = [
  { label: 'ReportMaster', value: 'reportMaster' },
  { label: 'Signature', value: 'signature' },
  { label: 'History', value: 'history' },
]

const activeTab = ref('reportMaster')
</script>

<template>
  <TabLayout v-model="activeTab" :tabs="tabs">
    <template #default="{ active }">
      <ReportMasterTableTab v-if="active === 'reportMaster'" />
      <ReportSignatureTableTab v-else-if="active === 'signature'" />
      <ReportHistoryTableTab v-else-if="active === 'history'" />
    </template>
  </TabLayout>
</template>