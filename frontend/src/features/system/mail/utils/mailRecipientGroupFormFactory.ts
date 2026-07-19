import type { MailRecipientGroupResponse } from '@/features/system/mail/types/mailApiTypes'
import type {
  MailRecipientForm,
  MailRecipientGroupForm,
} from '@/features/system/mail/types/mailFormTypes'
import type { MailTestSendForm } from '../types/mailFormTypes'

let tempRecipientId = -1

export const createEmptyMailRecipient = (): MailRecipientForm => ({
  id: tempRecipientId--,
  recipientKey: '',
  recipientName: '',
  email: '',
  recipientType: 'TO',
  activeFlag: true,
})

export const createEmptyMailRecipientGroupForm = (): MailRecipientGroupForm => ({
  id: 0,
  groupKey: '',
  groupName: '',
  activeFlag: true,
  recipients: [],
})

export const toMailRecipientGroupForm = (
  group: MailRecipientGroupResponse,
): MailRecipientGroupForm => ({
  id: group.id,
  groupKey: group.groupKey,
  groupName: group.groupName,
  activeFlag: group.activeFlag,
  recipients: (group.recipients ?? []).map(item => ({
    id: item.id,
    recipientKey: item.recipientKey ?? '',
    recipientName: item.recipientName ?? '',
    email: item.email,
    recipientType: item.recipientType ?? 'TO',
    activeFlag: item.activeFlag,
  })),
})

export const createMailTestSendForm = (): MailTestSendForm => ({
  to: '',
  toName: '',
  cc: '',
  bcc: '',
  subject: 'テストメール',
  body: 'これはProjectAdminからのテストメールです。',
  attachmentFilePath: '',
  attachmentFileName: '',
})