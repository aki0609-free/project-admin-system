<script setup lang="ts">
import { computed, ref } from 'vue'
import type { MailSendResult } from '@/features/system/mail/types/mailApiTypes'
import MailPdfSendDialog from './components/MailPdfSendDialog.vue'

const props = defineProps<{
  modelValue: boolean
  title?: string
  pdfUrl: string | null
  pdfFileKey?: string | null
  pdfFileName?: string | null
  storageType?: 'LOCAL' | 'S3'
  mailType?: string | null
  businessKey?: string | null
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'mail-sent', result: MailSendResult): void
}>()

const visible = computed({
  get: () => props.modelValue,
  set: value => emit('update:modelValue', value),
})

const mailDialog = ref(false)
const successSnackbar = ref(false)
const successMessage = ref('')

const canSendMail = computed(() => !!props.pdfFileKey)

const close = () => {
  visible.value = false
}

const openMailDialog = () => {
  mailDialog.value = true
}

const handleMailSent = (result: MailSendResult) => {
  successMessage.value = result.message || 'メールの送信が完了しました。'
  successSnackbar.value = true

  emit('mail-sent', result)
}
</script>

<template>
  <v-dialog
    v-model="visible"
    fullscreen
    persistent
    scrim="rgba(0, 0, 0, 0.45)"
  >
    <v-card class="pdf-dialog-card">
      <v-toolbar
        density="comfortable"
        color="white"
        class="pdf-toolbar"
      >
        <v-toolbar-title class="font-weight-bold">
          {{ title ?? 'PDFプレビュー' }}
        </v-toolbar-title>

        <v-spacer />

        <v-btn
          color="primary"
          variant="flat"
          size="small"
          prepend-icon="mdi-email-outline"
          :disabled="!canSendMail"
          @click="openMailDialog"
        >
          メール送信
        </v-btn>

        <v-btn
          class="ml-2"
          color="grey-darken-1"
          variant="outlined"
          size="small"
          prepend-icon="mdi-close"
          @click="close"
        >
          閉じる
        </v-btn>
      </v-toolbar>

      <div class="pdf-body">
        <iframe
          v-if="pdfUrl"
          :src="pdfUrl"
          class="pdf-frame"
        />

        <div
          v-else
          class="empty"
        >
          <v-icon
            size="40"
            color="grey"
          >
            mdi-file-pdf-box
          </v-icon>

          <div class="mt-2">
            PDFがありません。
          </div>
        </div>
      </div>
    </v-card>
  </v-dialog>

  <MailPdfSendDialog
    v-model="mailDialog"
    :title="props.title"
    :pdf-file-key="props.pdfFileKey ?? ''"
    :pdf-file-name="props.pdfFileName ?? props.title ?? 'document.pdf'"
    :storage-type="props.storageType ?? 'LOCAL'"
    :mail-type="props.mailType"
    :business-key="props.businessKey"
    @sent="handleMailSent"
  />

  <v-snackbar
    v-model="successSnackbar"
    color="success"
    timeout="2200"
    location="top"
    rounded="pill"
    elevation="8"
  >
    <v-icon start>
      mdi-check-circle
    </v-icon>

    {{ successMessage }}
  </v-snackbar>
</template>

<style scoped>
.pdf-dialog-card {
  height: 100vh;
  display: flex;
  flex-direction: column;
  border-radius: 0;
}

.pdf-toolbar {
  border-bottom: 1px solid #e5e7eb;
  flex: 0 0 auto;
}

.pdf-body {
  flex: 1;
  min-height: 0;
  background: #f8fafc;
}

.pdf-frame {
  width: 100%;
  height: 100%;
  border: none;
  background: white;
}

.empty {
  height: 100%;
  display: grid;
  place-items: center;
  align-content: center;
  color: #64748b;
}
</style>