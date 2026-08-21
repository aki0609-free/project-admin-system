<script setup lang="ts">
import { computed, toRef, watch } from 'vue'
import TabLayout from '@/shared/components/layout/tab_layout/TabLayout.vue'
import AppDialog from '@/shared/ui/dialog/AppDialog.vue'
import FormLayout from '@/shared/components/form/base/FormLayout.vue'
import GridBasedForm from '@/shared/components/form/grid_based_form/GridBasedForm.vue'
import type { EmployeeDetailResponse, EmployeeResignRequest } from '../types/employeeApiTypes'
import type { EmployeeForm } from '../types/employeeFormTypes'
import { useEmployeeEditDialog } from '../composables/useEmployeeEditDialog'
import EmployeeResignDialog from './EmployeeResignDialog.vue'
import { useEmployeeResignationChecklistQuery } from '../api/useEmployeeResignationChecklistQuery'
import { useResignEmployeeMutation } from '../api/useResignEmployeeMutation'
import { useCancelEmployeeResignationMutation } from '../api/useCancelEmployeeResignationMutation'
import { useEmployeePayrollItemSettingCatalogQuery } from '../api/useEmployeePayrollItemSettingCatalogQuery'
import EmployeePayrollItemSettingsPanel from './EmployeePayrollItemSettingsPanel.vue'

const props = defineProps<{
  modelValue: boolean
  employee: EmployeeDetailResponse | null
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'save' | 'delete', value: EmployeeForm): void
  (e: 'resigned'): void
}>()

const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value),
})

const resignationChecklistQuery = useEmployeeResignationChecklistQuery()
const resignMutation = useResignEmployeeMutation()
const cancelResignationMutation = useCancelEmployeeResignationMutation()
const payrollItemCatalogQuery = useEmployeePayrollItemSettingCatalogQuery()

const {
  activeTab,
  formModel,
  isEdit,
  tabs,
  basicFields,
  payrollFields,
  contractFields,
  basicSchema,
  payrollSchema,
  contractSchema,
  leftFooterItems,
  rightFooterItems,
  resignDialogVisible,
} = useEmployeeEditDialog(
  visible,
  toRef(props, 'employee'),
  (form) => emit('save', form),
  (form) => emit('delete', form),
  async (employeeId) => {
    const confirmed = window.confirm('退職を取り消して在籍状態へ戻しますか？')
    if (!confirmed) return
    await cancelResignationMutation.mutateAsync(employeeId)
    visible.value = false
    emit('resigned')
  },
)

const handleResign = async (request: EmployeeResignRequest) => {
  if (!formModel.id) {
    return
  }

  await resignMutation.mutateAsync({
    id: formModel.id,
    request,
  })

  resignDialogVisible.value = false
  visible.value = false
  emit('resigned')
}

watch(
  [visible, () => props.employee, payrollItemCatalogQuery.settings],
  ([opened, currentEmployee, catalog]) => {
    if (!opened || currentEmployee || !catalog.length) return
    formModel.payrollItemSettings = catalog.map((item) => ({
      ...item,
      effectiveFrom: item.effectiveFrom ?? '',
      effectiveTo: item.effectiveTo ?? '',
      parameters: { ...item.parameters },
      parameterDefinitions: [...item.parameterDefinitions],
    }))
  },
  { immediate: true },
)
</script>

<template>
  <AppDialog
    v-model="visible"
    :title="isEdit ? '従業員情報編集' : '従業員情報新規作成'"
    size="xl"
    :left-footer-items="leftFooterItems"
    :right-footer-items="rightFooterItems"
  >
    <TabLayout v-model="activeTab" :tabs="tabs">
      <template #default="{ active }">
        <FormLayout v-if="active === 'basic'" v-model="formModel" :schema="basicSchema">
          <GridBasedForm v-model="formModel" :fields="basicFields" />
        </FormLayout>

        <EmployeePayrollItemSettingsPanel
          v-else-if="active === 'payrollItems'"
          v-model="formModel.payrollItemSettings"
          :employee-id="formModel.id"
        />

        <FormLayout
          v-else-if="active === 'payroll'"
          v-model="formModel.payrollProfile"
          :schema="payrollSchema"
        >
          <GridBasedForm v-model="formModel.payrollProfile" :fields="payrollFields" />
        </FormLayout>

        <FormLayout
          v-else-if="active === 'contract'"
          v-model="formModel.contract"
          :schema="contractSchema"
        >
          <GridBasedForm v-model="formModel.contract" :fields="contractFields" />
        </FormLayout>
      </template>
    </TabLayout>
  </AppDialog>

  <EmployeeResignDialog
    v-model="resignDialogVisible"
    :employee="formModel"
    :checklist="resignationChecklistQuery.checklist.value"
    :message="resignationChecklistQuery.message.value"
    @submit="handleResign"
  />
</template>
