<script setup lang="ts">
import { computed, toRef } from 'vue'
import TabLayout from '@/shared/components/layout/tab_layout/TabLayout.vue'
import DetailDialogLayout from '@/toolbox/dialog/DetailDialogLayout.vue'
import FormGridTab from '@/toolbox/tab/FormGridTab.vue'
import type {
  EmployeeDetailResponse,
  EmployeeResignRequest,
} from '../types/employeeApiTypes'
import type { EmployeeForm } from '../types/employeeFormTypes'
import { useEmployeeEditDialog } from '../composables/useEmployeeEditDialog'
import EmployeeResignDialog from './EmployeeResignDialog.vue'
import { useEmployeeResignationChecklistQuery } from '../api/useEmployeeResignationChecklistQuery'
import { useResignEmployeeMutation } from '../api/useResignEmployeeMutation'

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
  footerItems,
  resignDialogVisible,
} = useEmployeeEditDialog(
  visible,
  toRef(props, 'employee'),
  (form) => emit('save', form),
  (form) => emit('delete', form),
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
</script>

<template>
  <DetailDialogLayout
    v-model="visible"
    :title="isEdit ? '従業員情報編集' : '従業員情報新規作成'"
    max-width="1280"
    :footer-items="footerItems"
  >
    <TabLayout v-model="activeTab" :tabs="tabs">
      <template #default="{ active }">
        <FormGridTab
          v-if="active === 'basic'"
          v-model="formModel"
          :schema="basicSchema"
          :fields="basicFields"
        />

        <FormGridTab
          v-else-if="active === 'payroll'"
          v-model="formModel.payrollProfile"
          :schema="payrollSchema"
          :fields="payrollFields"
        />

        <FormGridTab
          v-else-if="active === 'contract'"
          v-model="formModel.contract"
          :schema="contractSchema"
          :fields="contractFields"
        >
          <v-textarea
            v-model="formModel.contract.note"
            label="契約メモ"
            variant="outlined"
            rows="5"
            auto-grow
            hide-details
          />
        </FormGridTab>
      </template>
    </TabLayout>
  </DetailDialogLayout>

  <EmployeeResignDialog
    v-model="resignDialogVisible"
    :employee="formModel"
    :checklist="resignationChecklistQuery.checklist.value"
    @submit="handleResign"
  />
</template>