<script setup lang="ts">
import { computed, ref } from 'vue'
import ListDetailPageLayout from '@/toolbox/pages/ListDetailPageLayout.vue'
import SearchPanel from '@/shared/components/search/SearchPanel.vue'
import TabLayout from '@/shared/components/layout/tab_layout/TabLayout.vue'

import DailyReportTable from '@/features/dailyreport/components/DailyReportTable.vue'
import DailyReportEditDialog from '@/features/dailyreport/components/DailyReportEditDialog.vue'
import DailyReportMissingEmployeeTable from '@/features/dailyreport/components/DailyReportMissingEmployeeTable.vue'
import DailyReportMonthlyAttendanceTable from '@/features/dailyreport/components/DailyReportMonthlyAttendanceTable.vue'

import { useDailyReportPage } from '@/features/dailyreport/composables/useDailyReportPage'
import { useDailyReportSearch } from '@/features/dailyreport/composables/useDailyReportSearch'
import { useDailyReportMissingEmployeesQuery } from '@/features/dailyreport/api/useDailyReportMissingEmployeesQuery'
import { useDailyReportMonthlyAttendanceQuery } from '@/features/dailyreport/api/useDailyReportMonthlyAttendanceQuery'
import { formatYearMonthDay } from '@/shared/utils/DateUtils'

const {
  employeesQuery,
  dailyReportsQuery,
  dialog,
  leftToolbarItems,
  rightToolbarItems,
  save,
  remove,
} = useDailyReportPage()

const activeTab = ref<'reports' | 'missing' | 'monthlyAttendance'>('reports')

const tabs = [
  { label: '日報一覧', value: 'reports' },
  { label: '未入力者', value: 'missing' },
  { label: '月次勤怠', value: 'monthlyAttendance' },
]

const search = useDailyReportSearch(
  () => dailyReportsQuery.reports.value,
)

const missingWorkDate = computed(() => search.condition.targetWorkDate)
const targetMonth = computed(() => search.condition.attendanceTargetMonth)

const missingEmployeesQuery = useDailyReportMissingEmployeesQuery(
  missingWorkDate,
)

const monthlyAttendanceQuery = useDailyReportMonthlyAttendanceQuery(
  targetMonth,
)

const attendancePeriodText = computed(() => {
  const first = monthlyAttendanceQuery.attendances.value[0]

  if (!first?.periodStart || !first?.periodEnd) {
    return ''
  }

  return `${formatYearMonthDay(first.periodStart)} ～ ${formatYearMonthDay(first.periodEnd)}`
})

const countText = computed(() => {
  if (activeTab.value === 'reports') {
    return `表示件数：${search.filteredReports.value.length} 件`
  }

  if (activeTab.value === 'missing') {
    if (!missingWorkDate.value) {
      return '未入力確認日を指定してください。'
    }

    return `未入力者：${missingEmployeesQuery.employees.value.length} 人`
  }

  if (!targetMonth.value) {
    return '月次勤怠を表示する月を指定してください。'
  }

  return `月次勤怠：${monthlyAttendanceQuery.attendances.value.length} 人`
})

const openCreateFromMissing = (row: { employeeId: number }) => {
  if (!missingWorkDate.value) return

  dialog.openCreate({
    employeeId: row.employeeId,
    workDate: missingWorkDate.value,
  })
}
</script>

<template>
  <ListDetailPageLayout
    title="日報入力処理"
    description="従業員・勤務日・現場・勤務時間・日当を管理します。"
    :left-toolbar-items="leftToolbarItems"
    :right-toolbar-items="rightToolbarItems"
  >
    <template #search>
      <SearchPanel
        v-model="search.condition"
        :fields="search.fields.value"
        @clear="search.clear"
      />
    </template>

    <template #before-table>
      <div class="before-table-info">
        <p class="count-text">
          {{ countText }}
        </p>

        <p
          v-if="activeTab === 'monthlyAttendance' && attendancePeriodText"
          class="period-text"
        >
          集計期間：{{ attendancePeriodText }}
        </p>
      </div>
    </template>

    <TabLayout v-model="activeTab" :tabs="tabs">
      <template #default="{ active }">
        <DailyReportTable
          v-if="active === 'reports'"
          :items="search.filteredReports.value"
          @row-click="dialog.openEdit"
        />

        <DailyReportMissingEmployeeTable
          v-else-if="active === 'missing'"
          :items="missingEmployeesQuery.employees.value"
          @row-click="openCreateFromMissing"
        />

        <DailyReportMonthlyAttendanceTable
          v-else-if="active === 'monthlyAttendance'"
          :items="monthlyAttendanceQuery.attendances.value"
        />
      </template>
    </TabLayout>

    <template #dialogs>
      <DailyReportEditDialog
        v-model="dialog.visible.value"
        :daily-report="dialog.selected.value"
        :create-params="dialog.createParams.value"
        :employees="employeesQuery.employees.value"
        @save="save"
        @delete="remove"
      />
    </template>
  </ListDetailPageLayout>
</template>

<style scoped>
.before-table-info {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 20px;
  align-items: center;
}

.count-text,
.period-text {
  margin: 0;
  color: #64748b;
  font-size: 13px;
}

.period-text {
  font-weight: 600;
  color: #334155;
}
</style>