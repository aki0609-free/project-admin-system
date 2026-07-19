<script setup lang="ts">
import { computed, toRef } from 'vue'

import DetailDialogLayout from '@/toolbox/dialog/DetailDialogLayout.vue'
import FormLayout from '@/shared/components/form/base/FormLayout.vue'
import GridBasedForm from '@/shared/components/form/grid_based_form/GridBasedForm.vue'

import NoticeContentPreview from '@/shared/components/notice/NoticeContentPreviwe.vue'
import NoticeRichEditor from '@/shared/components/notice/NoticeRichEditor.vue'

import type { NoticeRuleResponse } from '@/features/system/notice/types/noticeRuleApiTypes'
import type { NoticeRuleForm } from '@/features/system/notice/types/noticeRuleFormTypes'
import { useNoticeRuleEditDialog } from '@/features/system/notice/composables/useNoticeRuleEditDialog'

const props = defineProps<{
  modelValue: boolean
  rule: NoticeRuleResponse | null
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'save' | 'delete', value: NoticeRuleForm): void
}>()

const visible = computed({
  get: () => props.modelValue,
  set: value => emit('update:modelValue', value),
})

const {
  formModel,
  isEdit,
  fields,
  schema,
  footerItems,
} = useNoticeRuleEditDialog(
  visible,
  toRef(props, 'rule'),
  form => emit('save', form),
  form => emit('delete', form),
)

const contentFormatItems = [
  { title: '通常テキスト', value: 'PLAIN_TEXT' },
  { title: 'リッチテキスト', value: 'HTML' },
  { title: 'Markdown', value: 'MARKDOWN' },
]

const previewContent = computed(() =>
  formModel.noticeBodyTemplate
    .replaceAll('{label}', '株式会社A')
    .replaceAll('{date}', '2026-05-31')
    .replaceAll('{key}', '1'),
)
</script>

<template>
  <DetailDialogLayout
    v-model="visible"
    :title="isEdit ? 'NoticeRule編集' : 'NoticeRule新規作成'"
    max-width="1180"
    :footer-items="footerItems"
  >
    <FormLayout
      v-model="formModel"
      :schema="schema"
    >
      <GridBasedForm
        v-model="formModel"
        :fields="fields"
      />
    </FormLayout>

    <div class="template-block">
      <v-select
        v-model="formModel.noticeContentFormat"
        label="本文形式"
        :items="contentFormatItems"
        variant="outlined"
        density="comfortable"
        hide-details
      />

      <NoticeRichEditor
        v-if="formModel.noticeContentFormat === 'HTML'"
        v-model="formModel.noticeBodyTemplate"
      />

      <v-textarea
        v-else
        v-model="formModel.noticeBodyTemplate"
        label="本文テンプレート"
        variant="outlined"
        rows="8"
        auto-grow
        hide-details
      />

      <v-alert
        type="info"
        variant="tonal"
        density="compact"
      >
        使用可能な変数：{label} / {date} / {key}
      </v-alert>

      <NoticeContentPreview
        :content="previewContent"
        :content-format="formModel.noticeContentFormat"
      />
    </div>
  </DetailDialogLayout>
</template>

<style scoped>
.template-block {
  display: grid;
  gap: 12px;
  margin-top: 20px;
}
</style>