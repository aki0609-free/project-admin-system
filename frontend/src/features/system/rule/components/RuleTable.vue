<script setup lang="ts">
import { computed } from 'vue'

import SimpleTable from '@/shared/components/table/simple_table/SimpleTable.vue'
import { createSimpleTableFilterRules } from '@/shared/components/table/simple_table/utils/createSimpleTableFilterRules'
import type {
  SimpleTableColumnDef,
  SimpleTableEditableRow,
} from '@/shared/components/table/simple_table/types/item/types'
import type { RuleMasterResponse } from '@/features/system/rule/types/ruleApiTypes'

type RuleTableRow = SimpleTableEditableRow & {
  id: number
  ruleName: string
  ruleDisplayName: string
  ruleType: string
  dslType: string
  resultFactKey: string
  priority: number
  activeText: string
  raw: RuleMasterResponse
}

const props = defineProps<{
  items: RuleMasterResponse[]
}>()

const emit = defineEmits<{
  'row-click': [rule: RuleMasterResponse]
}>()

const rows = computed<RuleTableRow[]>(() =>
  props.items.map(item => ({
    id: item.id,
    ruleName: item.ruleName,
    ruleDisplayName: item.ruleDisplayName,
    ruleType: item.ruleType,
    dslType: item.dslType,
    resultFactKey: item.resultFactKey,
    priority: item.priority,
    activeText: item.activeFlag ? '有効' : '無効',
    raw: item,
  })),
)

const columns = computed<SimpleTableColumnDef<RuleTableRow>[]>(() => [
  {
    title: 'ruleName',
    key: 'ruleName',
    type: 'text',
    width: '240px',
    filter: { type: 'text' },
  },
  {
    title: '表示名',
    key: 'ruleDisplayName',
    type: 'text',
    width: '220px',
    filter: { type: 'text' },
  },
  {
    title: 'RuleType',
    key: 'ruleType',
    type: 'text',
    width: '200px',
    filter: { type: 'text' },
  },
  {
    title: 'DSL',
    key: 'dslType',
    type: 'text',
    width: '200px',
    filter: { type: 'text' },
  },
  {
    title: 'Result',
    key: 'resultFactKey',
    type: 'text',
    width: '200px',
    filter: { type: 'text' },
  },
  {
    title: 'Priority',
    key: 'priority',
    type: 'number',
    width: '200px',
    filter: { type: 'text' },
  },
  {
    title: '状態',
    key: 'activeText',
    type: 'text',
    width: '200px',
    filter: { type: 'text' },
  },
])

const filterRules = computed(() =>
  createSimpleTableFilterRules<RuleTableRow>(columns.value),
)

const handleRowClick = (row: RuleTableRow) => {
  emit('row-click', row.raw)
}
</script>

<template>
  <SimpleTable
    table-key="rule-master-table"
    item-key="id"
    :items="rows"
    :columns="columns"
    :filter-rules="filterRules"
    enable-row-click
    @row-click="handleRowClick"
  />
</template>