<script setup lang="ts">
import { computed } from 'vue'
import SimpleTable from '@/shared/components/table/simple_table/SimpleTable.vue'
import FormLayout from '@/shared/components/form/base/FormLayout.vue'
import SectionedForm from '@/shared/components/form/sectioned_form/SectionedForm.vue'
import type { ReportMasterForm } from '@/features/system/report/types/reportFormTypes'
import { useReportMasterParamsTab } from '@/features/system/report/composables/useReportMasterParamsTab'
import DetailPanelLayout from '@/toolbox/panel/DetailPanelLayout.vue'

const props = defineProps<{
  modelValue: ReportMasterForm
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: ReportMasterForm): void
}>()

const model = computed<ReportMasterForm>({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value),
})

const {
  rows,
  columns,
  filterRules,
  selectedParamModel,
  schema,
  sections,
  hasSelection,
  selectRow,
  add,
  remove,
  moveUp,
  moveDown,
} = useReportMasterParamsTab(model)
</script>

<template>
  <section class="param-section">
    <div class="section-header">
      <h4>パラメータ定義</h4>

      <div class="param-actions">
        <v-btn
          variant="outlined"
          color="primary"
          size="small"
          @click="add"
        >
          追加
        </v-btn>

        <v-btn
          variant="outlined"
          color="secondary"
          size="small"
          :disabled="!hasSelection"
          @click="moveUp"
        >
          ↑
        </v-btn>

        <v-btn
          variant="outlined"
          color="secondary"
          size="small"
          :disabled="!hasSelection"
          @click="moveDown"
        >
          ↓
        </v-btn>

        <v-btn
          variant="outlined"
          color="error"
          size="small"
          :disabled="!hasSelection"
          @click="remove"
        >
          削除
        </v-btn>
      </div>
    </div>

    <div class="param-pane">
      <div class="param-pane-left">
        <SimpleTable
          table-key="report-master-param-editor"
          item-key="id"
          :items="rows"
          :columns="columns"
          :filter-rules="filterRules"
          enable-row-click
          @row-click="selectRow"
        />
      </div>

      <DetailPanelLayout
        :has-data="!!selectedParamModel"
        title="選択中パラメータ"
        subtitle="PARAM DETAIL"
        :chip="selectedParamModel?.paramName || '未設定'"
        empty-message="左の一覧からパラメータを選択してください。新規追加する場合は「追加」を押してください。"
      >
        <FormLayout
          v-if="selectedParamModel"
          v-model="selectedParamModel"
          :schema="schema"
        >
          <SectionedForm :sections="sections" />
        </FormLayout>
      </DetailPanelLayout>
    </div>
  </section>
</template>

<style scoped>
.param-section {
  display: grid;
  gap: 12px;
  padding-top: 16px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.param-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.param-pane {
  display: grid;
  grid-template-columns: minmax(420px, 1.1fr) minmax(380px, 1fr);
  gap: 16px;
  min-height: 420px;
}

.param-pane-left {
  min-width: 0;
}

@media (max-width: 1100px) {
  .param-pane {
    grid-template-columns: 1fr;
  }
}
</style>