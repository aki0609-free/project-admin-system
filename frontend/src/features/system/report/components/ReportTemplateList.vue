<script setup lang="ts">
import type { ReportTestParamTemplateResponse } from '../types/reportTestParamTemplateApiTypes';


defineProps<{
  templates: ReportTestParamTemplateResponse[]
}>()

const emit = defineEmits<{
  (
    e: 'select',
    jsonText: string,
  ): void
}>()
</script>

<template>
  <v-expansion-panels
    v-if="templates.length"
    variant="accordion"
  >
    <v-expansion-panel>
      <v-expansion-panel-title>
        保存済みテストJSON
      </v-expansion-panel-title>

      <v-expansion-panel-text>
        <v-list density="compact">
          <v-list-item
            v-for="item in templates"
            :key="item.id"
            @click="
              emit(
                'select',
                item.jsonText,
              )
            "
          >
            <template #prepend>
              <v-icon
                v-if="item.defaultFlag"
                color="primary"
              >
                mdi-star
              </v-icon>
            </template>

            <v-list-item-title>
              {{ item.templateName }}
            </v-list-item-title>

            <v-list-item-subtitle
              class="history-json"
            >
              {{ item.jsonText }}
            </v-list-item-subtitle>
          </v-list-item>
        </v-list>
      </v-expansion-panel-text>
    </v-expansion-panel>
  </v-expansion-panels>
</template>

<style scoped>
.history-json {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  font-family: monospace;
}
</style>