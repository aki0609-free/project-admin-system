<script setup lang="ts">
import { ref } from 'vue'

import TabLayout from '@/shared/components/layout/tab_layout/TabLayout.vue'
import ListDetailPageLayout from '@/toolbox/pages/ListDetailPageLayout.vue'

import ImportExecuteTab from '@/features/system/import/components/ImportExecuteTab.vue'
import ImportHistoryTab from '@/features/system/import/components/ImportHistoryTab.vue'
import ImportTargetDefinitionTab from '@/features/system/import/components/ImportTargetDefinitionTab.vue'

const activeTab = ref<'definitions' | 'execute' | 'histories'>('definitions')

const tabs = [
  { label: 'インポート定義', value: 'definitions' },
  { label: 'インポート実行', value: 'execute' },
  { label: '履歴', value: 'histories' },
]
</script>

<template>
  <ListDetailPageLayout
    title="外部データ取込"
    description="CSV取込の実行と、取込対象テーブル・カラム定義を管理します。"
  >
    <TabLayout
      v-model="activeTab"
      :tabs="tabs"
    >
      <template #default="{ active }">
        <ImportTargetDefinitionTab v-if="active === 'definitions'" />
        <ImportExecuteTab v-else-if="active === 'execute'" />
        <ImportHistoryTab v-else-if="active === 'histories'" />
      </template>
    </TabLayout>
  </ListDetailPageLayout>
</template>