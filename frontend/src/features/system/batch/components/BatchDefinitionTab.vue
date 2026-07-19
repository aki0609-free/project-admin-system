<script setup lang="ts">
import GenericToolbar from '@/shared/components/toolbar/GenericToolbar.vue'
import BatchDefinitionTable from '@/features/system/batch/components/BatchDefinitionTable.vue'
import BatchDefinitionEditDialog from '@/features/system/batch/components/BatchDefinitionEditDialog.vue'
import BatchExecuteDialog from '@/features/system/batch/components/BatchExecuteDialog.vue'
import PdfPreviewDialog from '@/shared/components/pdf/PdfPreviewDialog.vue'
import { useBatchDefinitionTab } from '@/features/system/batch/composables/useBatchDefinitionTab'

const {
  definitionsQuery,
  dialog,
  toolbarItems,

  save,
  remove,

  openExecuteNow,
  executeNow,
  executeScheduled,

  executeDialogVisible,
  executeTarget,
  executeLoading,

  previewVisible,
  previewUrl,
  previewTitle,
} = useBatchDefinitionTab()
</script>

<template>
  <div class="batch-tab">
    <div class="tab-header">
      <h3>バッチ定義</h3>

      <p class="tab-description">
        バッチの対象・即時実行・スケジュール実行を管理します。
      </p>
    </div>

    <GenericToolbar :items="toolbarItems" />

    <BatchDefinitionTable
      :items="definitionsQuery.definitions.value"
      @row-click="dialog.openEdit"
    />

    <BatchDefinitionEditDialog
      v-model="dialog.visible.value"
      :definition="dialog.dialogDefinition.value"
      @save="save"
      @delete="remove"
      @execute-now="openExecuteNow"
      @execute-scheduled="executeScheduled"
    />

    <BatchExecuteDialog
      v-model="executeDialogVisible"
      :definition="executeTarget"
      :loading="executeLoading"
      @execute="executeNow"
    />

    <PdfPreviewDialog
      v-model="previewVisible"
      :pdf-url="previewUrl"
      :pdf-file-key="null"
      :title="previewTitle"
    />
  </div>
</template>

<style scoped>
.batch-tab {
  display: grid;
  gap: 12px;
}

.tab-header {
  display: grid;
  gap: 6px;
}

.tab-description {
  margin: 0;
  color: #64748b;
}
</style>