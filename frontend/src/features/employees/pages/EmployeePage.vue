<script setup lang="ts">
import EmployeeTable from '../components/EmployeeTable.vue'
import EmployeeEditDialog from '../components/EmployeeEditDialog.vue'
import { useEmployeePage } from '../composables/useEmployeePage'
import ListDetailPageLayout from '@/toolbox/pages/ListDetailPageLayout.vue';

const {
  employeesQuery,
  dialog,
  leftToolbarItems,
  rightToolbarItems,
  save,
  remove,
} = useEmployeePage()
</script>

<template>
  <ListDetailPageLayout
    title="従業員情報"
    description="従業員の基本情報、給与・税金設定、契約情報を管理します。"
    :left-toolbar-items="leftToolbarItems"
    :right-toolbar-items="rightToolbarItems"
  >
    <EmployeeTable
      :items="employeesQuery.employees.value"
      @row-click="dialog.openEdit"
    />

    <template #dialogs>
      <EmployeeEditDialog
        v-model="dialog.visible.value"
        :employee="dialog.dialogEmployee.value"
        @save="save"
        @delete="remove"
      />
    </template>
  </ListDetailPageLayout>
</template>