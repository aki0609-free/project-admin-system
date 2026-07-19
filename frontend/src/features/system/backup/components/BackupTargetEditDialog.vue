<script setup lang="ts">
import { computed, toRef } from 'vue'
import TabLayout from '@/shared/components/layout/tab_layout/TabLayout.vue'
import FormLayout from '@/shared/components/form/base/FormLayout.vue'
import GridBasedForm from '@/shared/components/form/grid_based_form/GridBasedForm.vue'
import SimpleTable from '@/shared/components/table/simple_table/SimpleTable.vue'
import GenericToolbar from '@/shared/components/toolbar/GenericToolbar.vue'

import type { BackupTargetResponse } from '@/features/system/backup/types/backupApiTypes'
import type { BackupTargetDialogForm } from '@/features/system/backup/types/backupFormTypes'
import { useBackupTargetEditDialog } from '@/features/system/backup/composables/useBackupTargetEditDialog'
import { useBackupColumnEditor } from '@/features/system/backup/composables/useBackupColumnEditor'
import DetailDialogLayout from '@/toolbox/dialog/DetailDialogLayout.vue'

const props = defineProps<{
  modelValue: boolean
  target: BackupTargetResponse | null
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'save' | 'delete', value: BackupTargetDialogForm): void
}>()

const visible = computed({
  get: () => props.modelValue,
  set: value => emit('update:modelValue', value),
})

const {
  activeTab,
  formModel,
  title,
  tabs,
  basicFields,
  schema,
  leftFooterItems,
  rightFooterItems,
} = useBackupTargetEditDialog(
  visible,
  toRef(props, 'target'),
  form => emit('save', form),
  form => emit('delete', form),
)

const {
  rows: columnRows,
  columns: columnTableColumns,
  filterRules: columnFilterRules,
  selectedColumn,
  fields: columnFields,
  schema: columnSchema,
  toolbarItems: columnToolbarItems,
  selectRow: selectColumnRow,
} = useBackupColumnEditor(formModel)
</script>

<template>
  <DetailDialogLayout
    v-model="visible"
    :title="title"
    max-width="1280"
    :left-footer-items="leftFooterItems"
    :right-footer-items="rightFooterItems"
  >
    <TabLayout
      v-model="activeTab"
      :tabs="tabs"
    >
      <template #default="{ active }">
        <div
          v-if="active === 'basic'"
          class="tab-page"
        >
          <FormLayout
            v-model="formModel"
            :schema="schema"
          >
            <GridBasedForm
              v-model="formModel"
              :fields="basicFields"
            />
          </FormLayout>

          <v-textarea
            v-model="formModel.description"
            label="description"
            variant="outlined"
            rows="4"
            auto-grow
            hide-details
          />

          <v-alert
            v-if="formModel.outputMode !== 'DOWNLOAD'"
            type="info"
            variant="tonal"
            density="compact"
          >
            outputDir は物理パスではなく保存先キーのプレフィックスです。
            例：backups/customer
          </v-alert>
        </div>

        <div
          v-else-if="active === 'columns'"
          class="column-tab-page"
        >
          <GenericToolbar :items="columnToolbarItems" />

          <div class="column-pane">
            <div class="column-pane-left">
              <SimpleTable
                table-key="backup-target-column-editor"
                item-key="id"
                :items="columnRows"
                :columns="columnTableColumns"
                :filter-rules="columnFilterRules"
                enable-row-click
                @row-click="selectColumnRow"
              />
            </div>

            <div class="column-pane-right">
              <template v-if="selectedColumn">
                <div class="detail-header">
                  <div>
                    <p class="detail-eyebrow">COLUMN DETAIL</p>
                    <h5 class="detail-title">選択中Column</h5>
                  </div>

                  <div class="detail-chip">
                    {{ selectedColumn.columnName || '未設定' }}
                  </div>
                </div>

                <div class="detail-body">
                  <FormLayout
                    v-model="selectedColumn"
                    :schema="columnSchema"
                  >
                    <GridBasedForm
                      v-model="selectedColumn"
                      :fields="columnFields"
                    />
                  </FormLayout>
                </div>
              </template>

              <div
                v-else
                class="column-empty"
              >
                左の一覧から Column を選択してください。<br>
                新規追加する場合は「追加」を押してください。
              </div>
            </div>
          </div>
        </div>
      </template>
    </TabLayout>
  </DetailDialogLayout>
</template>

<style scoped>
.tab-page {
  display: grid;
  gap: 16px;
  padding-top: 16px;
}

.column-tab-page {
  display: grid;
  gap: 12px;
  padding-top: 16px;
}

.column-pane {
  display: grid;
  grid-template-columns: minmax(480px, 1.1fr) minmax(420px, 1fr);
  gap: 16px;
  min-height: 420px;
}

.column-pane-left,
.column-pane-right {
  min-width: 0;
}

.column-pane-right {
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  display: grid;
  align-content: start;
  background: #fafafa;
  overflow: hidden;
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  padding: 16px 18px;
  background: #f1f5f9;
  border-bottom: 1px solid #e5e7eb;
}

.detail-body {
  padding: 16px;
}

.detail-eyebrow {
  margin: 0 0 4px 0;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.08em;
  color: #64748b;
}

.detail-title {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
}

.detail-chip {
  padding: 6px 10px;
  border-radius: 9999px;
  background: #e2e8f0;
  font-size: 12px;
  font-weight: 600;
  color: #334155;
  max-width: 180px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.column-empty {
  color: #64748b;
  display: grid;
  place-items: center;
  min-height: 220px;
  text-align: center;
  line-height: 1.8;
  padding: 24px;
}

@media (max-width: 1100px) {
  .column-pane {
    grid-template-columns: 1fr;
  }
}
</style>