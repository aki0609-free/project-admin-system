<script setup lang="ts">
import { computed, toRef } from 'vue'
import GridBasedForm from '@/shared/components/form/grid_based_form/GridBasedForm.vue'
import type { ReportMasterForm } from '@/features/system/report/types/reportFormTypes'
import { useReportMasterBasicFields } from '@/features/system/report/composables/useReportMasterBasicFields'

const props = defineProps<{
  modelValue: ReportMasterForm
  templateOptions: { title: string; value: string }[]
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: ReportMasterForm): void
}>()

const model = computed<ReportMasterForm>({
  get: () => props.modelValue,
  set: value => emit('update:modelValue', value),
})

const { fields } = useReportMasterBasicFields(
  toRef(props, 'templateOptions'),
)
</script>

<template>
  <div class="tab-page">
    <GridBasedForm
      v-model="model"
      :fields="fields"
    />
  </div>
</template>

<style scoped>
.tab-page {
  display: grid;
  gap: 16px;
  padding-top: 16px;
}
</style>