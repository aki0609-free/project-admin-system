<script setup lang="ts">
import { toRef } from 'vue'
import SimpleTable from '@/shared/components/table/simple_table/SimpleTable.vue'
import type { NoticeRuleResponse } from '@/features/system/notice/types/noticeRuleApiTypes'
import {
  type NoticeRuleTableRow,
  useNoticeRuleTableConfig,
} from '@/features/system/notice/composables/useNoticeRuleTableConfig'

const props = defineProps<{
  items: NoticeRuleResponse[]
}>()

const emit = defineEmits<{
  (e: 'row-click', row: NoticeRuleTableRow): void
}>()

const {
  rows,
  columns,
  filterRules,
} = useNoticeRuleTableConfig(toRef(props, 'items'))
</script>

<template>
  <SimpleTable
    table-key="notice-rule-list"
    item-key="id"
    :items="rows"
    :columns="columns"
    :filter-rules="filterRules"
    enable-row-click
    @row-click="emit('row-click', $event)"
  />
</template>
