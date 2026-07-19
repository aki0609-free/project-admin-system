<script setup lang="ts">
import GenericToolbar from '@/shared/components/toolbar/GenericToolbar.vue'
import ImportTargetTable from '@/features/system/import/components/ImportTargetTable.vue'
import ImportTargetEditDialog from '@/features/system/import/components/ImportTargetEditDialog.vue'
import { useImportTargetDefinitionTab } from '@/features/system/import/composables/useImportTargetDefinitionTab'

const {
  targetsQuery,
  dialog,
  toolbarItems,
  onSave,
  onDelete,
} = useImportTargetDefinitionTab()
</script>

<template>
  <div class="import-target-page">
    <div class="page-header">
      <div>
        <h2>インポート定義</h2>
        <p class="page-description">
          CSV取込で使用する対象テーブル・取込元・カラム定義を管理します。
        </p>
      </div>
    </div>

    <GenericToolbar :items="toolbarItems" />

    <ImportTargetTable
      :items="targetsQuery.targets.value"
      @row-click="dialog.openEditDialog"
    />

    <ImportTargetEditDialog
      v-model="dialog.visible.value"
      :target="dialog.dialogTarget.value"
      @save="onSave"
      @delete="onDelete"
    />
  </div>
</template>

<style scoped>
.import-target-page {
  display: grid;
  gap: 16px;
}

.page-header {
  display: grid;
  gap: 6px;
}

.page-description {
  margin: 0;
  color: #64748b;
}
</style>