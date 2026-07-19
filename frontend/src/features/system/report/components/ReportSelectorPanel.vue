<script setup lang="ts">
import type { ReportMasterListItemResponse } from '@/features/system/report/types/reportMasterApiTypes'

defineProps<{
  reportMasters: ReportMasterListItemResponse[]
  selectedReport: ReportMasterListItemResponse | null
}>()

const selectedReportId = defineModel<number | null>('selectedReportId', {
  required: true,
})

const templateName = defineModel<string>('templateName', {
  required: true,
})
</script>

<template>
  <div class="selector-grid">
    <v-select
      v-model="selectedReportId"
      label="帳票"
      :items="reportMasters"
      item-title="reportName"
      item-value="id"
      variant="outlined"
      density="comfortable"
      hide-details
    >
      <template #item="{ props, item }">
        <v-list-item
          v-bind="props"
          :title="`${item.raw.reportCode} / ${item.raw.reportName}`"
        />
      </template>

      <template #selection="{ item }">
        {{ item.raw.reportCode }} / {{ item.raw.reportName }}
      </template>
    </v-select>

    <v-text-field
      :model-value="selectedReport?.reportCode ?? ''"
      label="帳票コード"
      variant="outlined"
      density="comfortable"
      readonly
      hide-details
    />

    <v-text-field
      v-model="templateName"
      label="保存名"
      variant="outlined"
      density="comfortable"
      hide-details
    />
  </div>
</template>

<style scoped>
.selector-grid {
  display: grid;
  gap: 16px;
}
</style>