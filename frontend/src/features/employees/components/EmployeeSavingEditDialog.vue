<script setup lang="ts">
import { computed, toRef } from 'vue'
import FormLayout from '@/shared/components/form/base/FormLayout.vue'
import GridBasedForm from '@/shared/components/form/grid_based_form/GridBasedForm.vue'
import AppDialog from '@/shared/ui/dialog/AppDialog.vue'
import { useEmployeeSavingEditDialog } from '@/features/employees/composables/useEmployeeSavingEditDialog'
import type { EmployeeListItemResponse } from '@/features/employees/types/employeeApiTypes'
import type { EmployeeSavingForm } from '@/features/employees/types/employeeLoanSavingFormTypes'
import type { EmployeeSavingResponse } from '@/features/employees/types/employeeWorkApiTypes'

const props = defineProps<{
  modelValue: boolean
  saving: EmployeeSavingResponse | null
  employees: EmployeeListItemResponse[]
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'save' | 'delete', value: EmployeeSavingForm): void
}>()

const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value),
})

const { formModel, fields, schema, leftFooterItems, rightFooterItems, isEdit } =
  useEmployeeSavingEditDialog(
    visible,
    toRef(props, 'saving'),
    toRef(props, 'employees'),
    (form) => emit('save', form),
    (form) => emit('delete', form),
  )
</script>

<template>
  <AppDialog
    v-model="visible"
    :title="isEdit ? '従業員貯蓄編集' : '従業員貯蓄新規作成'"
    size="lg"
    body-layout="stack"
    :left-footer-items="leftFooterItems"
    :right-footer-items="rightFooterItems"
  >
    <FormLayout v-model="formModel" :schema="schema">
      <GridBasedForm v-model="formModel" :fields="fields" />
    </FormLayout>
  </AppDialog>
</template>
