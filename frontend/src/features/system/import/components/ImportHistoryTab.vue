<script setup lang="ts">
import { ref } from 'vue'

import ImportErrorRowDialog from '@/features/system/import/components/ImportErrorRowDialog.vue'
import ImportHistoryTable from '@/features/system/import/components/ImportHistoryTable.vue'
import { useImportHistoriesQuery } from '@/features/system/import/api/useImportHistoriesQuery'

const historiesQuery = useImportHistoriesQuery()

const selectedHistoryId = ref<number | null>(null)
const errorDialogVisible = ref(false)

const showErrors = (historyId: number) => {
  selectedHistoryId.value = historyId
  errorDialogVisible.value = true
}
</script>

<template>
  <div class="import-history-tab">
    <ImportHistoryTable
      :items="historiesQuery.histories.value"
      @show-errors="showErrors"
    />

    <ImportErrorRowDialog
      v-model="errorDialogVisible"
      :history-id="selectedHistoryId"
    />
  </div>
</template>

<style scoped>
.import-history-tab {
  display: grid;
  gap: 12px;
  padding-top: 12px;
}
</style>