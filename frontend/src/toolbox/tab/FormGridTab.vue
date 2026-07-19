<!-- eslint-disable @typescript-eslint/no-explicit-any -->
<script setup lang="ts" generic="T extends Record<string, any>">
import FormLayout from '@/shared/components/form/base/FormLayout.vue'
import GridBasedForm from '@/shared/components/form/grid_based_form/GridBasedForm.vue'
import type { GridFormFieldDef } from '@/shared/components/form/grid_based_form/types/types'
import type { z } from 'zod'

defineProps<{
  modelValue: T
  schema: z.ZodObject<any>
  fields: GridFormFieldDef<T>[]
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: T): void
}>()
</script>

<template>
  <div class="form-grid-tab">
    <FormLayout
      :model-value="modelValue"
      :schema="schema"
      @update:model-value="emit('update:modelValue', $event as T)"
    >
      <GridBasedForm
        :model-value="modelValue"
        :fields="fields"
        @update:model-value="emit('update:modelValue', $event)"
      />

      <slot />
    </FormLayout>
  </div>
</template>

<style scoped>
.form-grid-tab {
  display: grid;
  gap: 16px;
  padding-top: 16px;
}
</style>