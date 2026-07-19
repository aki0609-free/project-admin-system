<script setup lang="ts" generic="T extends Record<string, any>">
import type { FormSection } from './types/types'
import EditableFormCell from '../base/EditableFormCell.vue'

const props = defineProps<{
  sections: FormSection<T>[]
}>()
</script>

<template>
  <div class="sectioned-form">
    <div v-for="section in sections" :key="section.title" class="form-section">
      <h3 class="section-title">
        {{ section.title }}
      </h3>

      <div class="section-body">
        <div v-for="field in section.fields" :key="String(field.key)" class="section-row">
          <EditableFormCell :field="field" />
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.sectioned-form {
  display: flex;
  flex-direction: column;
  gap: 32px;
}

.form-section {
  padding: 16px;
  border: 1px solid #eee;
  border-radius: 8px;
}

.section-title {
  margin-bottom: 16px;
  font-size: 18px;
  font-weight: 600;
}

.section-body {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
</style>
