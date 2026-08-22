<script setup lang="ts">
import { computed } from 'vue'
import type { NoticeResponse } from '@/features/dashboard/types/dashboardTypes'
import NoticeDetailDialog from '@/features/dashboard/components/NoticeDetailDialog.vue'
import AppDialog from '@/shared/ui/dialog/AppDialog.vue'
import type { ToolbarItem } from '@/shared/ui/toolbar/types'

const props = defineProps<{
  modelValue: boolean
  deleteConfirm: boolean
  notice: NoticeResponse | null
  deleting?: boolean
  canEdit: boolean
  canDelete: boolean
  getColor: (notice: NoticeResponse) => string
  getLabel: (notice: NoticeResponse) => string
  formatPeriod: (notice: NoticeResponse) => string
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  'update:deleteConfirm': [value: boolean]
  edit: []
  delete: []
}>()

const deleteConfirmVisible = computed({
  get: () => props.deleteConfirm,
  set: value => emit('update:deleteConfirm', value),
})

const deleteConfirmItems = computed<ToolbarItem[]>(() => [
  {
    type: 'button',
    label: 'キャンセル',
    intent: 'secondary',
    disabled: props.deleting,
    onClick: () => emit('update:deleteConfirm', false),
  },
  {
    type: 'button',
    label: '削除',
    intent: 'danger',
    loading: props.deleting,
    disabled: props.deleting,
    onClick: () => emit('delete'),
  },
])
</script>

<template>
  <NoticeDetailDialog
    :model-value="modelValue"
    :notice="notice"
    :can-edit="canEdit"
    :can-delete="canDelete"
    :get-color="getColor"
    :get-label="getLabel"
    :format-period="formatPeriod"
    :show-actions="canEdit || canDelete"
    @update:model-value="emit('update:modelValue', $event)"
    @edit="emit('edit')"
    @delete="emit('update:deleteConfirm', true)"
  />

  <AppDialog
    v-model="deleteConfirmVisible"
    title="お知らせ削除"
    size="sm"
    :right-footer-items="deleteConfirmItems"
  >
    「{{ notice?.title }}」を削除しますか？
  </AppDialog>
</template>
