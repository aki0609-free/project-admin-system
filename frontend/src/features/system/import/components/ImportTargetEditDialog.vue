<script setup lang="ts">
import { computed, toRef } from 'vue'
import TabLayout from '@/shared/components/layout/tab_layout/TabLayout.vue'
import FormLayout from '@/shared/components/form/base/FormLayout.vue'
import GridBasedForm from '@/shared/components/form/grid_based_form/GridBasedForm.vue'
import SimpleTable from '@/shared/components/table/simple_table/SimpleTable.vue'
import GenericToolbar from '@/shared/components/toolbar/GenericToolbar.vue'
import type { ImportTargetResponse } from '@/features/system/import/types/importApiTypes'
import type { ImportTargetDialogForm } from '@/features/system/import/types/importFormTypes'
import { useImportTargetEditDialog } from '@/features/system/import/composables/useImportTargetEditDialog'
import { useImportColumnEditor } from '@/features/system/import/composables/useImportColumnEditor'

const props = defineProps<{
  modelValue: boolean
  target: ImportTargetResponse | null
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'save', value: ImportTargetDialogForm): void
  (e: 'delete', value: ImportTargetDialogForm): void
}>()

const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value),
})

const {
  activeTab,
  formModel,
  isEdit,
  tabs,
  basicFields,
  schema,
  footerItems,
} = useImportTargetEditDialog(
  visible,
  toRef(props, 'target'),
  (form) => emit('save', form),
  (form) => emit('delete', form),
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
} = useImportColumnEditor(formModel)
</script>

<template>
  <v-dialog
    v-model="visible"
    max-width="1280"
    scrollable
    persistent
  >
    <v-card class="dialog-card">
      <v-card-title class="dialog-title">
        {{ isEdit ? 'インポート定義編集' : 'インポート定義新規作成' }}
      </v-card-title>

      <v-divider />

      <v-card-text class="dialog-body">
        <TabLayout v-model="activeTab" :tabs="tabs">
          <template #default="{ active }">
            <div v-if="active === 'basic'" class="tab-page">
              <FormLayout v-model="formModel" :schema="schema">
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
            </div>

            <div v-else-if="active === 'columns'" class="column-tab-page">
              <GenericToolbar :items="columnToolbarItems" />

              <div class="column-pane">
                <div class="column-pane-left">
                  <SimpleTable
                    tableKey="import-target-column-editor"
                    itemKey="id"
                    :items="columnRows"
                    :columns="columnTableColumns"
                    :filterRules="columnFilterRules"
                    enableRowClick
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

                  <div v-else class="column-empty">
                    左の一覧から Column を選択してください。<br>
                    新規追加する場合は「追加」を押してください。
                  </div>
                </div>
              </div>
            </div>
          </template>
        </TabLayout>
      </v-card-text>

      <v-divider />

      <div class="dialog-footer">
        <GenericToolbar :items="footerItems" />
      </div>
    </v-card>
  </v-dialog>
</template>

<style scoped>
.dialog-card {
  max-height: calc(100vh - 48px);
}

.dialog-title {
  padding: 16px 20px;
  font-weight: 700;
}

.dialog-body {
  padding: 16px 20px;
}

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
  grid-template-columns: minmax(520px, 1.15fr) minmax(420px, 1fr);
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

.dialog-footer {
  border-top: 1px solid #e5e7eb;
}

@media (max-width: 1100px) {
  .column-pane {
    grid-template-columns: 1fr;
  }
}
</style>