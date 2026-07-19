<script setup lang="ts">
import { useEmployeeTimesheetPage } from '@/features/employees/composables/useEmployeeTimesheetPage';
import GenericToolbar from '@/shared/components/toolbar/GenericToolbar.vue'
import EmployeeTimesheetTable from '../components/EmployeeTimesheetTable.vue';
import EmployeeTimesheetEditDialog from '../components/EmployeeTimesheetEditDialog.vue';

const {
  employeesQuery,
  timesheetsQuery,
  dialog,
  toolbarItems,
  save,
  remove,
} = useEmployeeTimesheetPage()
</script>

<template>
  <div class="employee-timesheet-page">
    <div class="page-header">
      <h2>勤怠管理</h2>
      <p class="page-description">
        従業員ごとの出勤・退勤・残業・深夜時間・承認状態を管理します。
      </p>
    </div>

    <GenericToolbar :items="toolbarItems" />

    <EmployeeTimesheetTable
      :items="timesheetsQuery.timesheets.value"
      @row-click="dialog.openEdit"
    />

    <EmployeeTimesheetEditDialog
      v-model="dialog.visible.value"
      :timesheet="dialog.selected.value"
      :employees="employeesQuery.employees.value"
      @save="save"
      @delete="remove"
    />
  </div>
</template>

<style scoped>
.employee-timesheet-page {
  display: grid;
  gap: 16px;
}

.page-header {
  display: grid;
  gap: 6px;
}

.page-description {
  margin: 0;
  color: #64748b;
}
</style>