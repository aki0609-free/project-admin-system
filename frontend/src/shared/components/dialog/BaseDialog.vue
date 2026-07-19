<script setup lang="ts">
import { ref } from 'vue'
import { VDialog, VCard, VCardTitle, VCardText, VCardActions, VBtn } from 'vuetify/components'

const props = defineProps<{
  modelValue: boolean
  title?: string
  width?: string
  persistent?: boolean
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
}>()

const close = () => emit('update:modelValue', false)
</script>

<template>
  <v-dialog
    v-model="props.modelValue"
    :persistent="props.persistent ?? false"
    :width="props.width ?? '600px'"
    scrollable
  >
    <v-card class="d-flex flex-column">
      <!-- タイトル -->
      <v-card-title>{{ title }}</v-card-title>

      <v-divider />

      <!-- 本体 -->
      <v-card-text class="dialog-body">
        <slot />
      </v-card-text>

      <v-divider />
      
      <!-- アクションボタン -->
      <v-card-actions>
        <v-spacer />
        <v-btn text color="primary" @click="close">閉じる</v-btn>
        <slot name="actions" />
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<style scoped>
.dialog-body {
  max-height: 80vh; /* 画面に収まる最大高さ */
  overflow-y: auto;  /* 高さを超えたらスクロール */
}
</style>