import type { MailRecipientGroupSaveRequest } from '@/features/system/mail/types/mailApiTypes'
import type { MailRecipientGroupForm } from '@/features/system/mail/types/mailFormTypes'

const blankToNull = (value: string): string | null =>
  value.trim() ? value : null

export const toMailRecipientGroupSaveRequest = (
  form: MailRecipientGroupForm,
): MailRecipientGroupSaveRequest => ({
  groupKey: form.groupKey,
  groupName: form.groupName,
  activeFlag: form.activeFlag,
  recipients: form.recipients.map(item => ({
    id: item.id > 0 ? item.id : null,
    recipientKey: blankToNull(item.recipientKey),
    recipientName: blankToNull(item.recipientName),
    email: item.email,
    recipientType: item.recipientType,
    activeFlag: item.activeFlag,
  })),
})