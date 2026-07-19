<script setup lang="ts">
import { computed } from 'vue'
import { useEmployeeLoanSavingsPage } from '@/features/employees/composables/useEmployeeLoanSavingsPage'
import TabLayout from '@/shared/components/layout/tab_layout/TabLayout.vue'
import EmployeeLoanTable from '../components/EmployeeLoanTable.vue'
import EmployeeLoanEditDialog from '../components/EmployeeLoanEditDialog.vue'
import EmployeeSavingTable from '../components/EmployeeSavingTable.vue'
import EmployeeSavingEditDialog from '../components/EmployeeSavingEditDialog.vue'
import ListDetailPageLayout from '@/toolbox/pages/ListDetailPageLayout.vue'

const {
  activeTab,
  tabs,

  employeesQuery,

  loansQuery,
  loanDialog,
  loanToolbarItems,
  saveLoan,
  deleteLoan,

  savingsQuery,
  savingDialog,
  savingToolbarItems,
  saveSaving,
  deleteSaving,
} = useEmployeeLoanSavingsPage()

const leftToolbarItems = computed(() =>
  activeTab.value === 'loans'
    ? loanToolbarItems.value
    : savingToolbarItems.value,
)
</script>

<template>
  <ListDetailPageLayout
    title="従業員貸付・貯蓄"
    description="従業員ごとの借入残高、月返済額、貯蓄率、貯蓄残高を管理します。"
    :left-toolbar-items="leftToolbarItems"
  >
    <TabLayout v-model="activeTab" :tabs="tabs">
      <template #default="{ active }">
        <EmployeeLoanTable
          v-if="active === 'loans'"
          :items="loansQuery.loans.value"
          @row-click="loanDialog.openEdit"
        />

        <EmployeeSavingTable
          v-else-if="active === 'savings'"
          :items="savingsQuery.savings.value"
          @row-click="savingDialog.openEdit"
        />
      </template>
    </TabLayout>

    <template #dialogs>
      <EmployeeLoanEditDialog
        v-model="loanDialog.visible.value"
        :loan="loanDialog.selected.value"
        :employees="employeesQuery.employees.value"
        @save="saveLoan"
        @delete="deleteLoan"
      />

      <EmployeeSavingEditDialog
        v-model="savingDialog.visible.value"
        :saving="savingDialog.selected.value"
        :employees="employeesQuery.employees.value"
        @save="saveSaving"
        @delete="deleteSaving"
      />
    </template>
  </ListDetailPageLayout>
</template>