<script setup lang="ts">
import ListDetailPageLayout from '@/toolbox/pages/ListDetailPageLayout.vue'
import TabLayout from '@/shared/components/layout/tab_layout/TabLayout.vue'

import DailyPreparationAssignmentTable from '../components/DailyPreparationAssignmentTable.vue'
import DailyPreparationDispatchTable from '../components/DailyPreparationDispatchTable.vue'
import OperationReportTab from '@/features/operation/reportpreview/components/OperationReportTab.vue'

import { useDailyPreparationPage } from '../composables/useDailyPreparationPage'

const {
  targetDate,
  activeTab,
  tabs,

  preparation,
  assignmentRows,
  dispatchRows,

  leftToolbarItems,
  rightToolbarItems,

  handleAssignmentCellUpdate,
  handleDispatchCellUpdate,
} = useDailyPreparationPage()
</script>

<template>
  <ListDetailPageLayout
    title="翌日準備"
    description="翌日以降の従業員配置・現場配車・作業伝票を管理します。"
    :left-toolbar-items="leftToolbarItems"
    :right-toolbar-items="rightToolbarItems"
  >
    <template #search>
      <div class="preparation-search">
        <v-text-field
          v-model="targetDate"
          type="date"
          label="対象日"
          variant="outlined"
          density="compact"
          hide-details
          prepend-inner-icon="mdi-calendar"
        />
      </div>
    </template>

    <template #before-table>
      <p class="count-text">
        {{ preparation ? '翌日準備あり' : '翌日準備未作成' }}
      </p>
    </template>

    <TabLayout
      v-model="activeTab"
      :tabs="tabs"
    >
      <template #default="{ active }">
        <DailyPreparationAssignmentTable
          v-if="active === 'assignments'"
          :items="assignmentRows"
          @update:items="handleAssignmentCellUpdate"
        />

        <DailyPreparationDispatchTable
          v-else-if="active === 'dispatches'"
          :items="dispatchRows"
          @update:items="handleDispatchCellUpdate"
        />

        <OperationReportTab
          v-else
          operation-type="PREPARATION"
          :target-date="targetDate"
        />
      </template>
    </TabLayout>
  </ListDetailPageLayout>
</template>

<style scoped>
.preparation-search {
  max-width: 240px;
}

.count-text {
  margin: 0;
  color: #64748b;
  font-size: 13px;
}
</style>