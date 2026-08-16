<script setup lang="ts">
import { computed, toRef } from 'vue'
import FormLayout from '@/shared/components/form/base/FormLayout.vue'
import GridBasedForm from '@/shared/components/form/grid_based_form/GridBasedForm.vue'
import AppDialog from '@/shared/ui/dialog/AppDialog.vue'
import type { EmployeeListItemResponse } from '@/features/employees/types/employeeApiTypes'
import type { EmployeeLoanForm } from '@/features/employees/types/employeeLoanSavingFormTypes'
import type { EmployeeLoanResponse } from '@/features/employees/types/employeeWorkApiTypes'
import { useEmployeeLoanEditDialog } from '../composables/useEmployeeLoanEditDialog'

const props = defineProps<{
  modelValue: boolean
  loan: EmployeeLoanResponse | null
  employees: EmployeeListItemResponse[]
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'save' | 'delete', value: EmployeeLoanForm): void
}>()

const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value),
})

const { formModel, fields, schema, leftFooterItems, rightFooterItems, isEdit } =
  useEmployeeLoanEditDialog(
    visible,
    toRef(props, 'loan'),
    toRef(props, 'employees'),
    (form) => emit('save', form),
    (form) => emit('delete', form),
  )
</script>

<template>
  <AppDialog
    v-model="visible"
    :title="isEdit ? '従業員貸付編集' : '従業員貸付新規作成'"
    size="lg"
    body-layout="stack"
    :left-footer-items="leftFooterItems"
    :right-footer-items="rightFooterItems"
  >
    <FormLayout v-model="formModel" :schema="schema">
      <GridBasedForm v-model="formModel" :fields="fields" />
    </FormLayout>

    <v-textarea
      v-model="formModel.approvalComment"
      label="承認コメント"
      variant="outlined"
      rows="4"
      auto-grow
      hide-details
    />
  </AppDialog>
</template>
