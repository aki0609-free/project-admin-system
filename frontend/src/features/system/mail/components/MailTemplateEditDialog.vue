<script setup lang="ts">
import { computed, toRef } from 'vue'
import DetailDialogLayout from '@/toolbox/dialog/DetailDialogLayout.vue'
import NoticeRichEditor from '@/shared/components/notice/NoticeRichEditor.vue'
import type { MailTemplateResponse } from '@/features/system/mail/types/mailApiTypes'
import type { MailTemplateForm } from '@/features/system/mail/types/mailFormTypes'
import { useMailTemplateEditDialog } from '@/features/system/mail/composables/useMailTemplateEditDialog'

const props = defineProps<{
  modelValue: boolean
  template: MailTemplateResponse | null
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'save' | 'delete', value: MailTemplateForm): void
}>()

const visible = computed({
  get: () => props.modelValue,
  set: value => emit('update:modelValue', value),
})

const templateDialog = useMailTemplateEditDialog(
  visible,
  toRef(props, 'template'),
  form => emit('save', form),
  form => emit('delete', form),
)

const placeholders = [
  '{{recipientName}}',
  '{{recipientEmail}}',
  '{{fileName}}',
  '{{reportCode}}',
  '{{businessKey}}',
  '{{executionId}}',
  '{{mailType}}',
]
</script>

<template>
  <DetailDialogLayout
    v-model="visible"
    :title="templateDialog.isEdit ? 'メッセージテンプレート編集' : 'メッセージテンプレート新規作成'"
    max-width="1280"
    :footer-items="templateDialog.footerItems"
  >
    <div class="template-form">
      <div class="basic-grid">
        <v-text-field
          v-model="templateDialog.formModel.templateKey"
          label="テンプレートキー"
          :readonly="templateDialog.isEdit"
          hint="半角英大文字・数字・アンダースコア。作成後は変更できません。"
          persistent-hint
        />

        <v-text-field
          v-model="templateDialog.formModel.templateName"
          label="テンプレート名"
        />

        <v-text-field
          v-model="templateDialog.formModel.fromAddress"
          label="差出人アドレス"
          readonly
          hint="環境設定されたSMTPアドレスを使用します。"
          persistent-hint
        />

        <v-text-field
          v-model="templateDialog.formModel.fromName"
          label="差出人名"
        />
      </div>

      <div class="switches">
        <v-switch
          v-model="templateDialog.formModel.htmlFlag"
          label="HTML形式"
          color="primary"
          hide-details
        />
        <v-switch
          v-model="templateDialog.formModel.activeFlag"
          label="有効"
          color="primary"
          hide-details
        />
      </div>

      <v-text-field
        v-model="templateDialog.formModel.subjectTemplate"
        label="件名テンプレート"
      />

      <div class="placeholder-panel">
        <span class="placeholder-label">使用可能な変数</span>
        <v-chip
          v-for="placeholder in placeholders"
          :key="placeholder"
          size="small"
          variant="outlined"
        >
          {{ placeholder }}
        </v-chip>
      </div>

      <div class="body-editor">
        <label class="body-label">本文テンプレート</label>
        <NoticeRichEditor
          v-if="templateDialog.formModel.htmlFlag"
          :key="`mail-template-editor-${templateDialog.editorRevision}`"
          v-model="templateDialog.formModel.bodyTemplate"
        />
        <v-textarea
          v-else
          v-model="templateDialog.formModel.bodyTemplate"
          rows="12"
          auto-grow
          variant="outlined"
        />
      </div>

      <v-card v-if="templateDialog.previewResult" variant="outlined" class="preview-card">
        <v-card-title>プレビュー</v-card-title>
        <v-card-subtitle>{{ templateDialog.previewResult.subject }}</v-card-subtitle>
        <v-card-text>
          <iframe
            v-if="templateDialog.previewResult.htmlFlag"
            class="preview-frame"
            sandbox=""
            :srcdoc="templateDialog.previewResult.body"
            title="メール本文プレビュー"
          />
          <pre v-else class="preview-text">{{ templateDialog.previewResult.body }}</pre>
        </v-card-text>
      </v-card>
    </div>
  </DetailDialogLayout>
</template>

<style scoped>
.template-form {
  display: grid;
  gap: 18px;
}

.basic-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.switches,
.placeholder-panel {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.placeholder-label,
.body-label {
  color: #475569;
  font-size: 0.875rem;
  font-weight: 600;
}

.body-editor {
  display: grid;
  gap: 8px;
}

.preview-card {
  min-height: 220px;
}

.preview-frame {
  width: 100%;
  min-height: 280px;
  border: 0;
  background: white;
}

.preview-text {
  margin: 0;
  white-space: pre-wrap;
  font-family: inherit;
}

@media (max-width: 900px) {
  .basic-grid {
    grid-template-columns: 1fr;
  }
}
</style>
