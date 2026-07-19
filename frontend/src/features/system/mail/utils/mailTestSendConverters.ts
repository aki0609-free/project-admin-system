import type {
  MailAttachmentRequest,
  MailTestSendRequest,
} from '@/features/system/mail/types/mailApiTypes'
import type { MailTestSendForm } from '@/features/system/mail/types/mailFormTypes'

const blankToNull = (value: string): string | null =>
  value.trim() ? value.trim() : null

const toAddressList = (value: string): string[] =>
  value
    .split(',')
    .map(item => item.trim())
    .filter(Boolean)

export const toMailTestSendRequest = (
  form: MailTestSendForm,
): MailTestSendRequest => {
  const attachments: MailAttachmentRequest[] = []

  if (form.attachmentFilePath.trim()) {
    attachments.push({
      storageType: 'LOCAL',
      fileKey: form.attachmentFilePath.trim(),
      fileName: form.attachmentFileName.trim() || 'attachment.pdf',
      fileSize: null,
      mimeType: null,
    })
  }

  return {
    toAddresses: toAddressList(form.to),
    toName: blankToNull(form.toName),
    ccAddresses: toAddressList(form.cc),
    bccAddresses: toAddressList(form.bcc),
    subject: form.subject,
    body: form.body,
    htmlFlag: false,
    attachments,
  }
}