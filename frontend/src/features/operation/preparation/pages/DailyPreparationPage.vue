<script setup lang="ts">
import ListDetailPageLayout from '@/shared/templates/list-detail/ListDetailPageTemplate.vue'
import TabLayout from '@/shared/components/layout/tab_layout/TabLayout.vue'
import DateFormField from '@/shared/components/form/base/components/form/DateFormField.vue'

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

  handleAssignmentCellUpdate,
  handleDispatchCellUpdate,
} = useDailyPreparationPage()
</script>

<template>
  <ListDetailPageLayout
    title="翌日準備"
    description="翌日以降の従業員配置・現場配車・作業伝票を管理します。"
    :left-toolbar-items="leftToolbarItems"
  >
    <template #search>
      <v-card variant="outlined" class="preparation-search-card">
        <v-card-text class="preparation-search-card__content">
          <DateFormField
            v-model="targetDate"
            class="preparation-search-control"
            label="対象日"
            variant="outlined"
            density="compact"
            hide-details
            prepend-inner-icon="mdi-calendar"
          />
        </v-card-text>
      </v-card>
    </template>

    <template #before-table>
      <div class="preparation-summary">
        <p class="count-text">
          {{ preparation ? '翌日準備あり' : '翌日準備未作成' }}
        </p>
      </div>
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
.preparation-search-card {
  width: 100%;
  border-color: #e2e8f0;
  border-radius: 12px;
  background: #ffffff;
  box-shadow: none;
}

.preparation-search-card__content {
  padding: 16px;
}

.preparation-search-control {
  max-width: 240px;
}

.count-text {
  margin: 0;
  color: #64748b;
  font-size: 13px;
}

.preparation-summary {
  display: grid;
  gap: 12px;
}
</style>
