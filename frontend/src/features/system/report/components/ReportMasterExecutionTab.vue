<script setup lang="ts">
import { computed } from 'vue'
import GridBasedForm from '@/shared/components/form/grid_based_form/GridBasedForm.vue'
import type { GridFormFieldDef } from '@/shared/components/form/grid_based_form/types/types'
import type { ReportMasterForm } from '@/features/system/report/types/reportFormTypes'

const props = defineProps<{
  modelValue: ReportMasterForm
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: ReportMasterForm): void
}>()

const model = computed<ReportMasterForm>({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value),
})

const fields = computed<GridFormFieldDef<ReportMasterForm>[]>(() => {
  const result: GridFormFieldDef<ReportMasterForm>[] = [
    {
      key: 'preProcessType',
      label: '前処理方式',
      type: 'select',
      options: [
        { title: 'NONE', value: 'NONE' },
        { title: 'SQL', value: 'SQL' },
        { title: 'PROCEDURE', value: 'PROCEDURE' },
      ],
    },

    {
      key: 'cleanupType',
      label: 'Cleanup方式',
      type: 'select',
      options: [
        { title: 'NONE', value: 'NONE' },
        { title: 'SQL', value: 'SQL' },
        { title: 'PROCEDURE', value: 'PROCEDURE' },
      ],
    },
  ]

  if (model.value.preProcessType === 'SQL') {
    result.push({
      key: 'preProcessSql',
      label: '前処理SQL',
      type: 'sqlEditor',
      rows: 10,
      autoGrow: true,
      gridColumn: '1 / span 4',
    })
  }

  if (model.value.preProcessType === 'PROCEDURE') {
    result.push({
      key: 'procedureName',
      label: 'Procedure名',
      type: 'text',
      gridColumn: '1 / span 4',
    })
  }

  result.push({
    key: 'querySql',
    label: 'Query SQL',
    type: 'sqlEditor',
    rows: 12,
    autoGrow: true,
    gridColumn: '1 / span 4',
  })

  if (model.value.cleanupType === 'SQL') {
    result.push({
      key: 'cleanupSql',
      label: 'Cleanup SQL',
      type: 'sqlEditor',
      rows: 8,
      autoGrow: true,
      gridColumn: '1 / span 4',
    })
  }

  if (model.value.cleanupType === 'PROCEDURE') {
    result.push({
      key: 'cleanupProcedureName',
      label: 'Cleanup Procedure名',
      type: 'text',
      gridColumn: '1 / span 4',
    })
  }

  return result
})
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