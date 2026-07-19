<script setup lang="ts" generic="T extends Record<string, any>">
import { ref, computed, inject } from 'vue'
import EditableFormCell from '../base/EditableFormCell.vue'
import { TabbedFormFieldDef } from '@/shared/components/form/tabbed_form/types/types'
import { FormContextKey } from '@/shared/components/form/base/types/types'

const props = defineProps<{
  modelValue: T
  fields: TabbedFormFieldDef<T>[]
  tabs: string[]
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: T): void
}>()

const model = computed({
  get: () => props.modelValue,
  set: val => emit('update:modelValue', val)
})

const activeTab = ref(0)

const activeTabName = computed(() =>
  props.tabs[activeTab.value] ?? props.tabs[0] as string
)

const tabFields = computed(() => {
  const map: Record<string, TabbedFormFieldDef<T>[]> = {}

  props.tabs.forEach(tab => (map[tab] = []))

  props.fields.forEach(field => {
    const tabName = field.tab ?? props.tabs[0] as string
    if (!map[tabName]) map[tabName] = []
    map[tabName].push(field)
  })

  return map
})
</script>

<template>
  <!-- タブ -->
  <v-tabs v-model="activeTab" color="primary">
    <v-tab v-for="tab in tabs" :key="tab">
      {{ tab }}
    </v-tab>
  </v-tabs>

  <v-divider class="mb-4" />

  <!-- フォーム -->
  <v-row dense>
    <v-col
      v-for="field in tabFields[activeTabName] ?? []"
      :key="String(field.key)"
      cols="12"
      md="6"
    >
      <EditableFormCell :field="field" />
    </v-col>
  </v-row>
</template>