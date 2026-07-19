<script setup lang="ts">
import { computed } from 'vue'
import SimpleTable from '@/shared/components/table/simple_table/SimpleTable.vue'
import type {
  SimpleTableColumnDef,
  SimpleTableEditableRow,
} from '@/shared/components/table/simple_table/types/item/types'
import { createSimpleTableFilterRules } from '@/shared/components/table/simple_table/utils/createSimpleTableFilterRules'
import type { MailRecipientForm } from '@/features/system/mail/types/mailFormTypes'

type RecipientRow = SimpleTableEditableRow & {
  id: number
  recipientType: string
  recipientKey: string
  recipientName: string
  email: string
  activeText: string
  raw: MailRecipientForm
}

const props = defineProps<{
  items: MailRecipientForm[]
}>()

const emit = defineEmits<{
  (e: 'row-click', row: MailRecipientForm): void
}>()

const rows = computed<RecipientRow[]>(() =>
  props.items.map(item => ({
    id: item.id,
    recipientType: item.recipientType,
    recipientKey: item.recipientKey,
    recipientName: item.recipientName,
    email: item.email,
    activeText: item.activeFlag ? '有効' : '無効',
    raw: item,
  })),
)

const columns = computed(() => {
  const defs: SimpleTableColumnDef<RecipientRow>[] = [
    { title: '種別', key: 'recipientType', width: '90px', filter: { type: 'text' } },
    { title: 'recipientKey', key: 'recipientKey', width: '180px', filter: { type: 'text' } },
    { title: '名前', key: 'recipientName', width: '180px', filter: { type: 'text' } },
    { title: 'email', key: 'email', width: '260px', filter: { type: 'text' } },
    { title: '状態', key: 'activeText', width: '100px', filter: { type: 'text' } },
  ]

  return defs
})

const filterRules = computed(() =>
  createSimpleTableFilterRules<RecipientRow>(columns.value),
)

const onRowClick = (row: RecipientRow) => {
  emit('row-click', row.raw)
}
</script>

<template>
  <SimpleTable
    table-key="mail-recipient-editor"
    item-key="id"
    :items="rows"
    :columns="columns"
    :filter-rules="filterRules"
    enable-row-click
    @row-click="onRowClick"
  />
</template>