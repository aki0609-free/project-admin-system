import type { MailRecipientType } from '@/features/system/mail/types/mailApiTypes'

export type MailRecipientForm = {
  id: number
  recipientKey: string
  recipientName: string
  email: string
  recipientType: MailRecipientType
  activeFlag: boolean
}

export type MailRecipientGroupForm = {
  id: number
  groupKey: string
  groupName: string
  activeFlag: boolean
  recipients: MailRecipientForm[]
}

export type MailTestSendForm = {
  to: string
  toName: string
  cc: string
  bcc: string
  subject: string
  body: string
  attachmentFilePath: string
  attachmentFileName: string
}

export type MailTemplateForm = {
  id: number
  templateKey: string
  templateName: string
  fromAddress: string
  fromName: string
  subjectTemplate: string
  bodyTemplate: string
  htmlFlag: boolean
  activeFlag: boolean
}
