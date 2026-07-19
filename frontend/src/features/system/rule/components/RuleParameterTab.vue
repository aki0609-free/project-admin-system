<!-- eslint-disable vue/no-mutating-props -->
<script setup lang="ts">
import { computed, ref } from 'vue'

import SimpleTable from '@/shared/components/table/simple_table/SimpleTable.vue'
import GenericToolbar from '@/shared/components/toolbar/GenericToolbar.vue'
import type { ToolbarItem } from '@/shared/components/toolbar/types/types'
import type {
  SimpleTableColumnDef,
  SimpleTableEditableRow,
} from '@/shared/components/table/simple_table/types/item/types'
import { createSimpleTableFilterRules } from '@/shared/components/table/simple_table/utils/createSimpleTableFilterRules'
import type { RuleMasterForm, RuleParameterForm } from '@/features/system/rule/types/ruleFormTypes'
import { createEmptyRuleParameter } from '../mapper/ruleFormMapper'

const props = defineProps<{
  form: RuleMasterForm
}>()

type RuleParameterRow = SimpleTableEditableRow & RuleParameterForm

const selectedParameter = ref<RuleParameterForm | null>(null)

const rows = computed<RuleParameterRow[]>(() =>
  props.form.parameters.map(item => ({
    ...item,
  })),
)

const columns = computed<SimpleTableColumnDef<RuleParameterRow>[]>(() => [
  { title: 'paramName', key: 'paramName', type: 'text', width: '200px', filter: { type: 'text' } },
  { title: 'dataType', key: 'dataType', type: 'text', width: '140px', filter: { type: 'text' } },
  { title: 'required', key: 'requiredFlag', type: 'checkbox', width: '120px', editable: true, filter: { type: 'checkbox' } },
  { title: 'default', key: 'defaultValue', type: 'text', width: '180px', filter: { type: 'text' } },
  { title: 'order', key: 'orderNo', type: 'number', width: '100px', filter: { type: 'text' } },
  { title: '説明', key: 'description', type: 'text', width: '260px', filter: { type: 'text' } },
])

const filterRules = computed(() =>
  createSimpleTableFilterRules<RuleParameterRow>(columns.value),
)

const dataTypeItems = [
  'STRING',
  'INTEGER',
  'LONG',
  'DECIMAL',
  'BOOLEAN',
  'DATE',
  'DATETIME',
]

const addParameter = () => {
  const nextOrderNo = props.form.parameters.length + 1
  const parameter = createEmptyRuleParameter(nextOrderNo)

  props.form.parameters.push(parameter)
  selectedParameter.value = parameter
}

const removeParameter = () => {
  if (!selectedParameter.value) return

  props.form.parameters = props.form.parameters.filter(
    item => item.id !== selectedParameter.value?.id,
  )

  selectedParameter.value = null
}

const selectRow = (row: RuleParameterRow) => {
  selectedParameter.value =
    props.form.parameters.find(item => item.id === row.id) ?? null
}

const toolbarItems = computed<ToolbarItem[]>(() => [
  {
    type: 'button',
    label: '追加',
    color: 'primary',
    icon: 'mdi-plus',
    onClick: addParameter,
  },
  {
    type: 'button',
    label: '削除',
    color: 'error',
    variant: 'text',
    disabled: !selectedParameter.value,
    icon: 'mdi-delete-outline',
    onClick: removeParameter,
  },
])
</script>

<template>
  <div class="parameter-tab">
    <GenericToolbar :items="toolbarItems" />

    <div class="parameter-layout">
      <SimpleTable
        table-key="rule-parameter-table"
        item-key="id"
        :items="rows"
        :columns="columns"
        :filter-rules="filterRules"
        enable-row-click
        @row-click="selectRow"
      />

      <div class="detail-panel">
        <template v-if="selectedParameter">
          <v-text-field
            v-model="selectedParameter.paramName"
            label="paramName"
            variant="outlined"
            density="comfortable"
          />

          <v-select
            v-model="selectedParameter.dataType"
            label="dataType"
            :items="dataTypeItems"
            variant="outlined"
            density="comfortable"
          />

          <v-checkbox
            v-model="selectedParameter.requiredFlag"
            label="必須"
            density="compact"
            hide-details
          />

          <v-text-field
            v-model="selectedParameter.defaultValue"
            label="defaultValue"
            variant="outlined"
            density="comfortable"
          />

          <v-text-field
            v-model.number="selectedParameter.orderNo"
            label="orderNo"
            type="number"
            variant="outlined"
            density="comfortable"
          />

          <v-textarea
            v-model="selectedParameter.description"
            label="説明"
            variant="outlined"
            rows="4"
            auto-grow
          />
        </template>

        <div v-else class="empty-panel">
          左の一覧からParameterを選択してください。
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.parameter-tab {
  display: grid;
  gap: 12px;
}

.parameter-layout {
  display: grid;
  grid-template-columns: minmax(560px, 1.2fr) minmax(360px, 0.8fr);
  gap: 16px;
}

.detail-panel {
  display: grid;
  align-content: start;
  gap: 12px;
  padding: 14px;
  border: 1px solid #e5e7eb;
  border-radius: 14px;
  background: #f8fafc;
}

.empty-panel {
  min-height: 240px;
  display: grid;
  place-items: center;
  color: #64748b;
}
</style>