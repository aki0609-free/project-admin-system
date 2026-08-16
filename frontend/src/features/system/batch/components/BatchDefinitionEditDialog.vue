<script setup lang="ts">
import { computed, toRef } from 'vue'
import FormLayout from '@/shared/components/form/base/FormLayout.vue'
import GridBasedForm from '@/shared/components/form/grid_based_form/GridBasedForm.vue'
import type { BatchJobDefinitionResponse } from '@/features/system/batch/types/batchApiTypes'
import type { BatchJobDefinitionForm } from '@/features/system/batch/types/batchFormTypes'
import { useBatchDefinitionEditDialog } from '@/features/system/batch/composables/useBatchDefinitionEditDialog'
import AppDialog from '@/shared/ui/dialog/AppDialog.vue'

const props = defineProps<{
  modelValue: boolean
  definition: BatchJobDefinitionResponse | null
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'save' | 'delete' | 'execute-now' | 'execute-scheduled', value: BatchJobDefinitionForm): void
}>()

const visible = computed({
  get: () => props.modelValue,
  set: value => emit('update:modelValue', value),
})

const {
  formModel,
  title,
  fields,
  schema,
  leftFooterItems,
  rightFooterItems,
} = useBatchDefinitionEditDialog(
  visible,
  toRef(props, 'definition'),
  form => emit('save', form),
  form => emit('delete', form),
  form => emit('execute-now', form),
  form => emit('execute-scheduled', form),
)
</script>

<template>
  <AppDialog
    v-model="visible"
    :title="title"
    size="lg"
    :max-width="1120"
    body-layout="stack"
    :left-footer-items="leftFooterItems"
    :right-footer-items="rightFooterItems"
  >
    <FormLayout
      v-model="formModel"
      :schema="schema"
    >
      <GridBasedForm
        v-model="formModel"
        :fields="fields"
      />
    </FormLayout>

    <v-alert
      v-if="formModel.scheduleEnabled"
      type="info"
      variant="tonal"
      density="compact"
    >
      保存するとスケジュールは自動で再読込されます。
    </v-alert>
  </AppDialog>
</template>
