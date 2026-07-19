<script setup lang="ts">
import { computed } from 'vue'
import SimpleTable from '@/shared/components/table/simple_table/SimpleTable.vue'
import type {
  SimpleTableColumnDef,
  SimpleTableEditableRow,
} from '@/shared/components/table/simple_table/types/item/types'
import { createSimpleTableFilterRules } from '@/shared/components/table/simple_table/utils/createSimpleTableFilterRules'
import type { MailRecipientGroupResponse } from '@/features/system/mail/types/mailApiTypes'

type Row = SimpleTableEditableRow & {
  id: number
  groupKey: string
  groupName: string
  recipientCount: number
  activeText: string
  raw: MailRecipientGroupResponse
}

const props = defineProps<{
  items: MailRecipientGroupResponse[]
}>()

const emit = defineEmits<{
  (e: 'row-click', row: MailRecipientGroupResponse): void
}>()

const rows = computed<Row[]>(() =>
  props.items.map((item) => ({
    id: item.id,
    groupKey: item.groupKey,
    groupName: item.groupName,
    recipientCount: item.recipients?.length ?? 0,
    activeText: item.activeFlag ? '有効' : '無効',
    raw: item,
  })),
)

const columns = computed(() => {
  const defs: SimpleTableColumnDef<Row>[] = [
    { title: 'ID', key: 'id', width: '180px', filter: { type: 'text' } },
    { title: 'groupKey', key: 'groupKey', width: '180px', filter: { type: 'text' } },
    { title: 'groupName', key: 'groupName', width: '180px', filter: { type: 'text' } },
    { title: '宛先数', key: 'recipientCount', width: '180px', filter: { type: 'text' } },
    { title: '状態', key: 'activeText', width: '180px', filter: { type: 'text' } },
  ]

  return defs
})

const filterRules = computed(() =>
  createSimpleTableFilterRules<Row>(columns.value),
)

const onRowClick = (row: Row) => {
  emit('row-click', row.raw)
}
</script>

<template>
  <SimpleTable
    tableKey="mail-recipient-group-list"
    itemKey="id"
    :items="rows"
    :columns="columns"
    :filterRules="filterRules"
    enableRowClick
    @row-click="onRowClick"
  />
</template>