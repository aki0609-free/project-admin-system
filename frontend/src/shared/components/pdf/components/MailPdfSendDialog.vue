<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import type {
  MailPdfSendRequest,
  MailRecipientGroupResponse,
  MailSendResult,
  MailTemplateResponse,
} from '@/features/system/mail/types/mailApiTypes'
import { useSendPdfMailMutation } from '../api/useSendPdfMailMutation'
import { useMailRecipientGroupsQuery } from '@/features/system/mail/api/queries/useMailRecipientGroupsQuery'
import { useMailTemplatesQuery } from '@/features/system/mail/api/queries/useMailTemplatesQuery'
import { usePreviewMailTemplateMutation } from '@/features/system/mail/api/mutations/usePreviewMailTemplateMutation'

const props = defineProps<{
  modelValue: boolean
  title?: string
  pdfFileKey: string | null
  pdfFileName: string | null
  storageType: 'LOCAL' | 'S3'
  mailType?: string | null
  businessKey?: string | null
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'sent', result: MailSendResult): void
}>()

const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value),
})

const mutation = useSendPdfMailMutation()
const recipientGroupsQuery = useMailRecipientGroupsQuery()
const mailTemplatesQuery = useMailTemplatesQuery()
const templatePreviewMutation = usePreviewMailTemplateMutation()
const sendErrorMessage = ref('')
const templateErrorMessage = ref('')

const form = reactive({
  recipientGroupKey: null as string | null,
  templateKey: null as string | null,

  to: '',
  cc: '',
  bcc: '',

  subject: '',
  body: '',
  htmlFlag: false,
})

const groupItems = computed(() =>
  recipientGroupsQuery.groups.value.map((group) => ({
    title: `${group.groupName}（${group.groupKey}）`,
    value: group.groupKey,
  })),
)

const selectedGroup = computed<MailRecipientGroupResponse | null>(() => {
  if (!form.recipientGroupKey) return null

  return (
    recipientGroupsQuery.groups.value.find((group) => group.groupKey === form.recipientGroupKey) ??
    null
  )
})

const templateItems = computed(() =>
  mailTemplatesQuery.templates.value
    .filter((template) => template.activeFlag)
    .map((template) => ({
      title: `${template.templateName}（${template.templateKey}）`,
      value: template.templateKey,
    })),
)

const selectedTemplate = computed<MailTemplateResponse | null>(() => {
  if (!form.templateKey) return null

  return (
    mailTemplatesQuery.templates.value.find(
      (template) => template.templateKey === form.templateKey,
    ) ?? null
  )
})

const toAddressList = (value: string) =>
  value
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean)

const joinRecipients = (group: MailRecipientGroupResponse | null, type: 'TO' | 'CC' | 'BCC') => {
  if (!group) return ''

  return group.recipients
    .filter((item) => item.activeFlag)
    .filter((item) => item.recipientType === type)
    .map((item) => item.email)
    .filter(Boolean)
    .join(', ')
}

watch(
  () => props.modelValue,
  (value) => {
    if (!value) return

    sendErrorMessage.value = ''
    templateErrorMessage.value = ''

    form.recipientGroupKey = null
    form.templateKey = null
    form.to = ''
    form.cc = ''
    form.bcc = ''

    form.subject = props.title ? `${props.title}の送付` : 'PDF送付のご案内'
    form.body = [
      'いつもお世話になっております。',
      '',
      '添付ファイルをご確認ください。',
      '',
      'よろしくお願いいたします。',
    ].join('\n')
    form.htmlFlag = false
  },
)

watch(
  () => selectedGroup.value,
  (group) => {
    if (!group) {
      form.to = ''
      form.cc = ''
      form.bcc = ''
      return
    }

    form.to = joinRecipients(group, 'TO')
    form.cc = joinRecipients(group, 'CC')
    form.bcc = joinRecipients(group, 'BCC')
  },
)

watch(
  () => selectedTemplate.value,
  async (template) => {
    if (!template) return

    templateErrorMessage.value = ''
    try {
      const preview = await templatePreviewMutation.mutateAsync({
        subjectTemplate: template.subjectTemplate,
        bodyTemplate: template.bodyTemplate,
        htmlFlag: template.htmlFlag,
        variables: {
          fileName: props.pdfFileName ?? '',
          documentName: props.title ?? props.pdfFileName ?? '',
          reportName: props.title ?? '',
          mailType: props.mailType ?? 'PDF_MAIL',
          businessKey: props.businessKey ?? '',
          recipientName: selectedGroup.value?.groupName ?? '',
          recipientEmail: form.to,
        },
      })

      form.subject = preview.subject
      form.body = preview.body
      form.htmlFlag = preview.htmlFlag
    } catch {
      templateErrorMessage.value = 'メッセージテンプレートの適用に失敗しました。'
    }
  },
)

const canSend = computed(
  () =>
    !!props.pdfFileKey &&
    !!props.pdfFileName &&
    !!form.to.trim() &&
    !!form.subject.trim() &&
    !!form.body.trim() &&
    !mutation.isPending.value &&
    !templatePreviewMutation.isPending.value,
)

const close = () => {
  visible.value = false
}

const send = async () => {
  if (!canSend.value || !props.pdfFileKey || !props.pdfFileName) return

  sendErrorMessage.value = ''

  const request: MailPdfSendRequest = {
    mailType: props.mailType ?? 'PDF_MAIL',
    businessKey: props.businessKey ?? null,

    recipientGroupKey: form.recipientGroupKey,
    recipientKey: null,

    toAddresses: toAddressList(form.to),
    toName: null,
    ccAddresses: toAddressList(form.cc),
    bccAddresses: toAddressList(form.bcc),

    subject: form.subject,
    body: form.body,
    htmlFlag: form.htmlFlag,

    storageType: props.storageType,
    pdfFileKey: props.pdfFileKey,
    pdfFileName: props.pdfFileName,
  }

  const result = (await mutation.mutateAsync(request)) as MailSendResult

  if (result.failedCount > 0 || result.sentCount === 0) {
    sendErrorMessage.value = result.message || 'メール送信に失敗しました。'
    return
  }

  emit('sent', result)
  visible.value = false
}
</script>

<template>
  <v-dialog v-model="visible" width="1180" max-width="calc(100vw - 48px)" scrollable>
    <v-card rounded="xl" class="mail-card">
      <div class="mail-header">
        <div class="header-icon">
          <v-icon size="24"> mdi-email-send-outline </v-icon>
        </div>

        <div class="header-text">
          <div class="text-h6 font-weight-bold">PDFメール送信</div>
          <div class="text-caption text-grey">表示中のPDFを添付してメール送信します</div>
        </div>

        <v-spacer />

        <v-btn icon="mdi-close" variant="text" size="small" @click="close" />
      </div>

      <v-divider />

      <v-card-text class="mail-body">
        <v-alert v-if="!pdfFileKey" type="warning" variant="tonal" density="compact">
          添付対象のPDFキーがありません。
        </v-alert>

        <div class="attachment-box">
          <v-icon color="red" size="28"> mdi-file-pdf-box </v-icon>

          <div class="attachment-text">
            <div class="attachment-title">
              {{ pdfFileName ?? 'document.pdf' }}
            </div>
          </div>
        </div>

        <div class="field-grid">
          <v-select
            v-model="form.recipientGroupKey"
            label="宛先グループ"
            placeholder="指定なしの場合は手入力"
            :items="groupItems"
            clearable
            prepend-inner-icon="mdi-account-group-outline"
            variant="outlined"
            density="comfortable"
            hide-details
          />

          <v-text-field
            v-model="form.to"
            label="TO"
            placeholder="example@example.com"
            prepend-inner-icon="mdi-account-outline"
            variant="outlined"
            density="comfortable"
            hide-details
          />

          <v-text-field
            v-model="form.cc"
            label="CC"
            placeholder="複数の場合はカンマ区切り"
            prepend-inner-icon="mdi-account-multiple-outline"
            variant="outlined"
            density="comfortable"
            hide-details
          />

          <v-text-field
            v-model="form.bcc"
            label="BCC"
            placeholder="複数の場合はカンマ区切り"
            prepend-inner-icon="mdi-eye-off-outline"
            variant="outlined"
            density="comfortable"
            hide-details
          />

          <v-select
            v-model="form.templateKey"
            label="メッセージテンプレート"
            placeholder="選択しない場合は自由入力"
            :items="templateItems"
            :loading="mailTemplatesQuery.isLoading.value || templatePreviewMutation.isPending.value"
            clearable
            prepend-inner-icon="mdi-text-box-check-outline"
            variant="outlined"
            density="comfortable"
            hint="テンプレート適用後も件名と本文を自由に編集できます"
            persistent-hint
          />

          <v-text-field
            v-model="form.subject"
            label="件名"
            prepend-inner-icon="mdi-format-title"
            variant="outlined"
            density="comfortable"
            hide-details
          />

          <v-textarea
            v-model="form.body"
            :label="form.htmlFlag ? '本文（HTML）' : '本文'"
            prepend-inner-icon="mdi-text-box-outline"
            variant="outlined"
            rows="8"
            auto-grow
            hide-details
            class="body-field"
          />
        </div>

        <v-alert
          v-if="templateErrorMessage"
          type="error"
          variant="tonal"
          density="compact"
        >
          {{ templateErrorMessage }}
        </v-alert>

        <v-alert
          v-if="mutation.isError.value || sendErrorMessage"
          type="error"
          variant="tonal"
          density="compact"
        >
          {{ sendErrorMessage || 'メール送信に失敗しました。' }}
        </v-alert>
      </v-card-text>

      <v-divider />

      <v-card-actions class="mail-actions">
        <v-btn variant="text" @click="close"> キャンセル </v-btn>

        <v-btn
          color="primary"
          variant="flat"
          prepend-icon="mdi-send"
          :loading="mutation.isPending.value"
          :disabled="!canSend"
          @click="send"
        >
          送信する
        </v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<style scoped>
.mail-card {
  max-height: calc(100vh - 48px);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  width: 100%;
}

.mail-header {
  flex: 0 0 auto;
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 18px 22px;
  background: linear-gradient(135deg, #f8fafc, #eef6ff);
}

.mail-body {
  flex: 1 1 auto;
  min-height: 0;
  overflow-y: auto;

  display: grid;
  gap: 16px;
  padding: 20px 22px;
  background: #ffffff;
}

.attachment-box {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px;
  border: 1px solid #e2e8f0;
  border-radius: 14px;
  background: #f8fafc;
}

.attachment-text {
  min-width: 0;
}

.attachment-title {
  font-weight: 700;
  color: #0f172a;
}

.field-grid {
  display: grid;
  gap: 14px;
}

.body-field {
  min-height: 180px;
}

.mail-actions {
  flex: 0 0 auto;
  padding: 14px 22px;
  justify-content: flex-end;
  background: #f8fafc;
}
</style>
