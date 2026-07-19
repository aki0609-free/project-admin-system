<script setup lang="ts">
import { computed } from 'vue'
import SimpleTable from '@/shared/components/table/simple_table/SimpleTable.vue'
import type {
  SimpleTableColumnDef,
  SimpleTableEditableRow,
} from '@/shared/components/table/simple_table/types/item/types'
import { createSimpleTableFilterRules } from '@/shared/components/table/simple_table/utils/createSimpleTableFilterRules'
import type { MailTemplateResponse } from '@/features/system/mail/types/mailApiTypes'

type Row = SimpleTableEditableRow & {
  id: number
  templateKey: string
  templateName: string
  format: string
  activeText: string
  raw: MailTemplateResponse
}

const props = defineProps<{ items: MailTemplateResponse[] }>()
const emit = defineEmits<{
  (e: 'row-click', row: MailTemplateResponse): void
}>()

const rows = computed<Row[]>(() =>
  props.items.map(item => ({
    id: item.id,
    templateKey: item.templateKey,
    templateName: item.templateName,
    format: item.htmlFlag ? 'HTML' : 'プレーンテキスト',
    activeText: item.activeFlag ? '有効' : '無効',
    raw: item,
  })),
)

const columns = computed<SimpleTableColumnDef<Row>[]>(() => [
  { title: 'ID', key: 'id', width: '100px', filter: { type: 'text' } },
  { title: 'テンプレートキー', key: 'templateKey', width: '260px', filter: { type: 'text' } },
  { title: 'テンプレート名', key: 'templateName', width: '280px', filter: { type: 'text' } },
  { title: '本文形式', key: 'format', width: '180px', filter: { type: 'text' } },
  { title: '状態', key: 'activeText', width: '120px', filter: { type: 'text' } },
])

const filterRules = computed(() =>
  createSimpleTableFilterRules<Row>(columns.value),
)
</script>

<template>
  <SimpleTable
    table-key="mail-template-list"
    item-key="id"
    :items="rows"
    :columns="columns"
    :filter-rules="filterRules"
    enable-row-click
    @row-click="row => emit('row-click', row.raw)"
  />
</template>
