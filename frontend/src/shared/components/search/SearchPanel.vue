<script setup lang="ts" generic="TModel extends Record<string, unknown>">
import DateFormField from '../form/base/components/form/DateFormField.vue'
import MonthFormField from '../form/base/components/form/MonthFormField.vue';
import type { SearchPanelFieldDef } from './types/searchPanelTypes'

const model = defineModel<TModel>({
  required: true,
})

defineProps<{
  fields: SearchPanelFieldDef<TModel>[]
  title?: string
  description?: string
  clearLabel?: string
}>()

const emit = defineEmits<{
  clear: []
}>()
</script>

<template>
  <v-card variant="flat" class="search-panel">
    <v-card-title v-if="title" class="search-panel__title">
      {{ title }}
    </v-card-title>
    <v-card-subtitle v-if="description" class="search-panel__description">
      {{ description }}
    </v-card-subtitle>
    <v-card-text>
      <v-row dense>
        <v-col
          v-for="field in fields"
          :key="field.key"
          :cols="field.cols ?? 12"
          :md="field.md ?? 3"
        >
          <v-text-field
            v-if="field.type === 'text'"
            v-model="model[field.key]"
            :label="field.label"
            :placeholder="field.placeholder"
            :prepend-inner-icon="field.prependIcon"
            density="compact"
            variant="outlined"
            hide-details
            :clearable="field.clearable ?? true"
          />

          <DateFormField
            v-else-if="field.type === 'date'"
            v-model="model[field.key] as string"
            :label="field.label"
            density="compact"
            variant="outlined"
            hide-details
            :clearable="field.clearable ?? true"
            :prepend-inner-icon="field.prependIcon ?? 'mdi-calendar'"
          />

          <MonthFormField
            v-else-if="field.type === 'month'"
            v-model="model[field.key] as string"
            :label="field.label"
            density="compact"
            :clearable="field.clearable ?? true"
          />

          <v-text-field
            v-else-if="field.type === 'number'"
            v-model.number="model[field.key]"
            :label="field.label"
            type="number"
            :prepend-inner-icon="field.prependIcon"
            density="compact"
            variant="outlined"
            hide-details
            :clearable="field.clearable ?? true"
          />

          <v-select
            v-else-if="field.type === 'select'"
            v-model="model[field.key]"
            :label="field.label"
            :items="field.options ?? []"
            :prepend-inner-icon="field.prependIcon"
            density="compact"
            variant="outlined"
            hide-details
            :clearable="field.clearable ?? true"
          />
        </v-col>

        <v-col cols="12" md="auto" class="actions-col">
          <v-btn
            variant="outlined"
            color="primary"
            prepend-icon="mdi-filter-remove-outline"
            height="40"
            @click="emit('clear')"
          >
            {{ clearLabel ?? 'クリア' }}
          </v-btn>
        </v-col>
      </v-row>
    </v-card-text>
  </v-card>
</template>

<style scoped>
.search-panel {
  margin-bottom: 16px;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  background: #ffffff;
}

.search-panel__title {
  padding: 14px 16px 2px;
  color: #334155;
  font-size: 15px;
  font-weight: 700;
}

.search-panel__description {
  padding: 2px 16px 0;
  color: #64748b;
  font-size: 12px;
}

.actions-col {
  display: flex;
  align-items: flex-start;
  padding-top: 4px;
}
</style>
