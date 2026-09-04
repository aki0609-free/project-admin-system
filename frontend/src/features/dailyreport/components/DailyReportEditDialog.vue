<script setup lang="ts">
import { computed, toRef } from 'vue'
import TabLayout from '@/shared/components/layout/tab_layout/TabLayout.vue'
import FormGridTab from '@/shared/components/form/grid_based_form/FormGridTab.vue'
import AppDialog from '@/shared/ui/dialog/AppDialog.vue'
import DailyReportPayrollItemPanel from './DailyReportPayrollItemPanel.vue'

import type { DailyReportDetailResponse } from '@/features/dailyreport/types/dailyReportApiTypes'
import type { DailyReportForm } from '@/features/dailyreport/types/dailyReportFormTypes'
import type { EmployeeListItemResponse } from '@/features/employees/types/employeeApiTypes'

import { useDailyReportEditDialog } from '@/features/dailyreport/composables/useDailyReportEditDialog'
import type { DailyReportCreateParams } from '@/features/dailyreport/composables/useDailyReportDialog'

const props = defineProps<{
  modelValue: boolean
  dailyReport: DailyReportDetailResponse | null
  createParams: DailyReportCreateParams | null
  employees: EmployeeListItemResponse[]
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'save' | 'delete', value: DailyReportForm): void
}>()

const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value),
})

const {
  activeTab,
  formModel,
  isEdit,
  tabs,
  fields,
  billingFields,
  financeFields,
  schema,
  leftFooterItems,
  rightFooterItems,
  billingRateLoading,
  payrollItemsLoading,
  payrollItemsError,
  preparationDefaultsMessage,
} = useDailyReportEditDialog(
  visible,
  toRef(props, 'dailyReport'),
  toRef(props, 'createParams'),
  toRef(props, 'employees'),
  (form) => emit('save', form),
  (form) => emit('delete', form),
)
</script>

<template>
  <AppDialog
    v-model="visible"
    :title="isEdit ? '日報編集' : '日報新規作成'"
    size="xl"
    body-layout="stack"
    :left-footer-items="leftFooterItems"
    :right-footer-items="rightFooterItems"
  >
    <TabLayout v-model="activeTab" :tabs="tabs">
      <template #default="{ active }">
        <div v-if="active === 'basic'" class="basic-panel">
          <v-alert
            v-if="preparationDefaultsMessage"
            type="info"
            variant="tonal"
            density="compact"
          >
            {{ preparationDefaultsMessage }}
          </v-alert>
          <FormGridTab
            v-model="formModel"
            :schema="schema"
            :fields="fields"
          />
        </div>

        <div v-else-if="active === 'billing'" class="billing-panel">
          <v-alert v-if="formModel.customerSiteId == null" type="info" variant="tonal">
            基本情報タブで顧客と現場を選択してください。
          </v-alert>

          <template v-else>
            <v-alert type="info" variant="tonal">
              現場・職種・現場役職・勤務日から適用単価を確認します。
              保存時にはサーバー側で再判定されます。
            </v-alert>

            <v-progress-linear v-if="billingRateLoading" indeterminate />

            <FormGridTab v-model="formModel" :schema="schema" :fields="billingFields" />
          </template>
        </div>

        <DailyReportPayrollItemPanel
          v-else-if="active === 'allowance'"
          v-model="formModel.allowances"
          item-type="ALLOWANCE"
          :loading="payrollItemsLoading"
          :error="payrollItemsError"
        />

        <DailyReportPayrollItemPanel
          v-else-if="active === 'deduction'"
          v-model="formModel.deductions"
          item-type="DEDUCTION"
          :loading="payrollItemsLoading"
          :error="payrollItemsError"
        />

        <FormGridTab
          v-else-if="active === 'finance'"
          v-model="formModel"
          :schema="schema"
          :fields="financeFields"
        />
      </template>
    </TabLayout>
  </AppDialog>
</template>

<style scoped>
.billing-panel {
  display: grid;
  gap: 16px;
  padding: 16px;
}

.basic-panel {
  display: grid;
  gap: 12px;
}
</style>
