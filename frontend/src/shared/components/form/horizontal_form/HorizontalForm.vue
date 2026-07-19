<script setup lang="ts" generic="T extends Record<string, any>">
import type { FormFieldDef } from '../base/types/types'
import EditableFormCell from '../base/EditableFormCell.vue'

const props = defineProps<{
  fields: FormFieldDef<T>[]
  labelWidth?: number
}>()
</script>

<template>
  <div class="horizontal-form">
    <div
      v-for="field in fields"
      :key="String(field.key)"
      class="form-row"
    >
      <label
        class="form-label"
        :style="{ width: (labelWidth ?? 120) + 'px' }"
      >
        {{ field.label }}
      </label>

      <div class="form-input">
        <EditableFormCell :field="field" />
      </div>
    </div>
  </div>
</template>

<style scoped>
.horizontal-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.form-row {
  display: flex;
  align-items: center;
}

.form-label {
  font-weight: 600;
}

.form-input {
  flex: 1;
}
</style>