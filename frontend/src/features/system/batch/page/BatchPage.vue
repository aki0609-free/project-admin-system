<script setup lang="ts">
import { ref } from 'vue'

import TabLayout from '@/shared/components/layout/tab_layout/TabLayout.vue'

import BatchDefinitionTab from '@/features/system/batch/components/BatchDefinitionTab.vue'
import BatchExecutionLogTab from '@/features/system/batch/components/BatchExecutionLogTab.vue'
import ListDetailPageLayout from '@/toolbox/pages/ListDetailPageLayout.vue'

const activeTab = ref<'definitions' | 'logs'>('definitions')

const tabs = [
  {
    label: 'バッチ定義',
    value: 'definitions',
  },
  {
    label: '実行履歴',
    value: 'logs',
  },
]
</script>

<template>
  <ListDetailPageLayout
    title="バッチ管理"
    description="バッチ定義、即時実行、スケジュール実行、実行履歴を管理します。"
  >
    <TabLayout
      v-model="activeTab"
      :tabs="tabs"
    >
      <template #default="{ active }">
        <BatchDefinitionTab
          v-if="active === 'definitions'"
        />

        <BatchExecutionLogTab
          v-else-if="active === 'logs'"
        />
      </template>
    </TabLayout>
  </ListDetailPageLayout>
</template>