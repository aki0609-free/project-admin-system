<script setup lang="ts">
import { computed, toRef } from 'vue'
import FormLayout from '@/shared/components/form/base/FormLayout.vue'
import GridBasedForm from '@/shared/components/form/grid_based_form/GridBasedForm.vue'
import GenericToolbar from '@/shared/components/toolbar/GenericToolbar.vue'
import { useEmployeeSavingEditDialog } from '@/features/employees/composables/useEmployeeSavingEditDialog';
import { EmployeeListItemResponse } from '@/features/employees/types/employeeApiTypes';
import { EmployeeSavingForm } from '@/features/employees/types/employeeLoanSavingFormTypes';
import { EmployeeSavingResponse } from '@/features/employees/types/employeeWorkApiTypes';

const props = defineProps<{
  modelValue: boolean
  saving: EmployeeSavingResponse | null
  employees: EmployeeListItemResponse[]
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'save', value: EmployeeSavingForm): void
  (e: 'delete', value: EmployeeSavingForm): void
}>()

const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value),
})

const {
  formModel,
  fields,
  schema,
  footerItems,
  isEdit,
} = useEmployeeSavingEditDialog(
  visible,
  toRef(props, 'saving'),
  toRef(props, 'employees'),
  (form) => emit('save', form),
  (form) => emit('delete', form),
)
</script>

<template>
  <v-dialog v-model="visible" max-width="1080" scrollable persistent>
    <v-card class="dialog-card">
      <v-card-title class="dialog-title">
        {{ isEdit ? '従業員貯蓄編集' : '従業員貯蓄新規作成' }}
      </v-card-title>

      <v-divider />

      <v-card-text class="dialog-body">
        <FormLayout v-model="formModel" :schema="schema">
          <GridBasedForm
            v-model="formModel"
            :fields="fields"
          />
        </FormLayout>

        <v-textarea
          v-model="formModel.approvalComment"
          label="承認コメント"
          variant="outlined"
          rows="4"
          auto-grow
          hide-details
        />
      </v-card-text>

      <v-divider />

      <div class="dialog-footer">
        <GenericToolbar :items="footerItems" />
      </div>
    </v-card>
  </v-dialog>
</template>

<style scoped>
.dialog-card {
  width: 100%;
  max-height: calc(100vh - 48px);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.dialog-title {
  padding: 16px 20px;
  font-weight: 700;
  flex-shrink: 0;
}

.dialog-body {
  display: grid;
  gap: 16px;
  padding: 16px 20px;
  overflow-y: auto;
  overflow-x: hidden;
  flex: 1 1 auto;
  min-height: 0;
  max-height: calc(100vh - 170px);
}

.dialog-footer {
  border-top: 1px solid #e5e7eb;
  flex-shrink: 0;
  background: #fff;
}
</style>