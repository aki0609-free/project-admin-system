<script setup lang="ts">
import { computed, toRef } from 'vue'
import FormLayout from '@/shared/components/form/base/FormLayout.vue'
import GridBasedForm from '@/shared/components/form/grid_based_form/GridBasedForm.vue'
import AppDialog from '@/shared/ui/dialog/AppDialog.vue'
import { useEmployeeTimesheetEditDialog } from '@/features/employees/composables/useEmployeeTimesheetEditDialog'
import { EmployeeListItemResponse } from '@/features/employees/types/employeeApiTypes'
import { EmployeeTimesheetForm } from '@/features/employees/types/employeeTimesheetFormTypes'
import { EmployeeTimesheetResponse } from '@/features/employees/types/employeeWorkApiTypes'

const props = defineProps<{
  modelValue: boolean
  timesheet: EmployeeTimesheetResponse | null
  employees: EmployeeListItemResponse[]
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'save', value: EmployeeTimesheetForm): void
  (e: 'delete', value: EmployeeTimesheetForm): void
}>()

const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value),
})

const { formModel, isEdit, fields, schema, leftFooterItems, rightFooterItems } =
  useEmployeeTimesheetEditDialog(
    visible,
    toRef(props, 'timesheet'),
    toRef(props, 'employees'),
    (form) => emit('save', form),
    (form) => emit('delete', form),
  )
</script>

<template>
  <AppDialog
    v-model="visible"
    :title="isEdit ? '勤怠編集' : '勤怠新規作成'"
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
