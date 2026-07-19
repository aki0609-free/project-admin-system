<script setup lang="ts">
import { computed, toRef } from 'vue'

import { useNoticeEditDialog } from '@/features/dashboard/composables/useNoticeEditDialog'
import type {
  NoticeCreateRequest,
  NoticeResponse,
} from '@/features/dashboard/types/dashboardTypes'
import NoticeContentPreviwe from '@/shared/components/notice/NoticeContentPreviwe.vue';
import NoticeRichEditor from '@/shared/components/notice/NoticeRichEditor.vue'

const props = defineProps<{
  modelValue: boolean
  notice?: NoticeResponse | null
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  submit: [value: NoticeCreateRequest]
}>()

const dialog = computed({
  get: () => props.modelValue,
  set: value => emit('update:modelValue', value),
})

const {
  form,
  activeTab,
  isEdit,
  typeItems,
  colorItems,
  contentFormatItems,
  submit,
} = useNoticeEditDialog(
  dialog,
  toRef(props, 'notice'),
  value => emit('submit', value),
)
</script>

<template>
  <v-dialog v-model="dialog" width="820">
    <v-card rounded="xl">
      <v-card-title class="font-weight-bold">
        {{ isEdit ? 'Notice編集' : 'Notice作成' }}
      </v-card-title>

      <v-divider />

      <v-card-text>
        <v-row>
          <v-col cols="12">
            <v-text-field
              v-model="form.title"
              label="タイトル"
              variant="outlined"
              density="comfortable"
            />
          </v-col>

          <v-col cols="12" md="6">
            <v-text-field
              v-model="form.start"
              label="開始日"
              type="date"
              variant="outlined"
              density="comfortable"
            />
          </v-col>

          <v-col cols="12" md="6">
            <v-text-field
              v-model="form.end"
              label="終了日"
              type="date"
              variant="outlined"
              density="comfortable"
            />
          </v-col>

          <v-col cols="12" md="4">
            <v-select
              v-model="form.type"
              label="種別"
              :items="typeItems"
              variant="outlined"
              density="comfortable"
            />
          </v-col>

          <v-col cols="12" md="4">
            <v-select
              v-model="form.color"
              label="色"
              :items="colorItems"
              variant="outlined"
              density="comfortable"
            />
          </v-col>

          <v-col cols="12" md="4">
            <v-select
              v-model="form.contentFormat"
              label="本文形式"
              :items="contentFormatItems"
              variant="outlined"
              density="comfortable"
            />
          </v-col>

          <v-col cols="12" md="6">
            <v-checkbox
              v-model="form.pinnedFlag"
              label="固定表示する"
              density="compact"
              hide-details
            />
          </v-col>

          <v-col cols="12" md="6">
            <v-checkbox
              v-model="form.activeFlag"
              label="有効"
              density="compact"
              hide-details
            />
          </v-col>
        </v-row>

        <v-tabs v-model="activeTab" density="compact">
          <v-tab value="edit">
            編集
          </v-tab>

          <v-tab value="preview">
            プレビュー
          </v-tab>
        </v-tabs>

        <v-window v-model="activeTab">
          <v-window-item value="edit">
            <div class="mt-4">
              <NoticeRichEditor
                v-if="form.contentFormat === 'HTML'"
                v-model="form.content"
              />

              <v-textarea
                v-else
                v-model="form.content"
                label="内容"
                variant="outlined"
                rows="8"
                auto-grow
              />
            </div>
          </v-window-item>

          <v-window-item value="preview">
            <NoticeContentPreviwe
              class="mt-4"
              :content="form.content"
              :content-format="form.contentFormat"
            />
          </v-window-item>
        </v-window>
      </v-card-text>

      <v-card-actions class="justify-end">
        <v-btn variant="text" @click="dialog = false">
          キャンセル
        </v-btn>

        <v-btn color="primary" variant="flat" @click="submit">
          {{ isEdit ? '更新' : '作成' }}
        </v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>