<script setup lang="ts">
import { ref } from 'vue'

import TabLayout from '@/shared/components/layout/tab_layout/TabLayout.vue'

import BackupTargetTable from '@/features/system/backup/components/BackupTargetTable.vue'
import BackupTargetEditDialog from '@/features/system/backup/components/BackupTargetEditDialog.vue'
import BackupHistoryTable from '@/features/system/backup/components/BackupHistoryTable.vue'

import { useBackupPage } from '@/features/system/backup/composables/useBackupPage'
import { useBackupHistoriesQuery } from '@/features/system/backup/api/useBackupHistoriesQuery'
import ListDetailPageLayout from '@/toolbox/pages/ListDetailPageLayout.vue'
import GenericToolbar from '@/shared/components/toolbar/GenericToolbar.vue'

const activeTab = ref<'targets' | 'histories'>('targets')

const tabs = [
  { label: 'バックアップ対象', value: 'targets' },
  { label: '履歴', value: 'histories' },
]

const {
  backupTargetsQuery,
  selectedCodes,
  dialogVisible,
  dialogTarget,
  toolbarItems,
  openEditDialog,
  onSaveTarget,
  onDeleteTarget,
} = useBackupPage(activeTab)

const historiesQuery = useBackupHistoriesQuery()
</script>

<template>
  <ListDetailPageLayout
    title="バックアップ"
    description="対象テーブルのCSV/ZIP出力と実行履歴を管理します。"
  >
    <TabLayout
      v-model="activeTab"
      :tabs="tabs"
    >
      <template #default="{ active }">
        <div
          v-if="active === 'targets'"
          class="tab-page"
        >
          <GenericToolbar :items="toolbarItems" />

          <BackupTargetTable
            :items="backupTargetsQuery.targets.value"
            :selected-codes="selectedCodes"
            @update:selected-codes="selectedCodes = $event"
            @row-click="openEditDialog"
          />
        </div>

        <div
          v-else-if="active === 'histories'"
          class="tab-page"
        >
          <BackupHistoryTable
            :items="historiesQuery.histories.value"
          />
        </div>
      </template>
    </TabLayout>

    <template #dialogs>
      <BackupTargetEditDialog
        v-model="dialogVisible"
        :target="dialogTarget"
        @save="onSaveTarget"
        @delete="onDeleteTarget"
      />
    </template>
  </ListDetailPageLayout>
</template>

<style scoped>
.tab-page {
  display: grid;
  gap: 12px;
  padding-top: 12px;
}
</style>