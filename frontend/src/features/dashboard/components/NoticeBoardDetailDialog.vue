<script setup lang="ts">
import type { NoticeResponse } from '@/features/dashboard/types/dashboardTypes'
import NoticeDetailDialog from '@/features/dashboard/components/NoticeDetailDialog.vue'

defineProps<{
  modelValue: boolean
  deleteConfirm: boolean
  notice: NoticeResponse | null
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
</script>

<template>
  <NoticeDetailDialog
    :model-value="modelValue"
    :notice="notice"
    :get-color="getColor"
    :get-label="getLabel"
    :format-period="formatPeriod"
    show-actions
    @update:model-value="emit('update:modelValue', $event)"
    @edit="emit('edit')"
    @delete="emit('update:deleteConfirm', true)"
  />

  <v-dialog
    :model-value="deleteConfirm"
    width="420"
    @update:model-value="emit('update:deleteConfirm', $event)"
  >
    <v-card rounded="xl">
      <v-card-title class="font-weight-bold">
        Notice削除
      </v-card-title>

      <v-card-text>
        「{{ notice?.title }}」を削除しますか？
      </v-card-text>

      <v-card-actions class="justify-end">
        <v-btn
          variant="text"
          @click="emit('update:deleteConfirm', false)"
        >
          キャンセル
        </v-btn>

        <v-btn
          color="error"
          variant="flat"
          @click="emit('delete')"
        >
          削除
        </v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>