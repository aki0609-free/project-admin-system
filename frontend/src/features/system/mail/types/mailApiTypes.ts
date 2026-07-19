export type MailSendStatus =
  | 'DRAFT'
  | 'WAITING'
  | 'SENDING'
  | 'SENT'
  | 'FAILED'

export type MailRecipientType = 'TO' | 'CC' | 'BCC'

export type MailStorageType = 'LOCAL' | 'S3'

export type MailAttachmentRequest = {
  storageType: MailStorageType
  fileKey: string
  fileName: string
  fileSize?: number | null
  mimeType?: string | null
}

export type MailAttachmentResponse = {
  id: number
  storageType: MailStorageType
  fileKey: string
  fileName: string
  fileSize: number | null
  mimeType: string | null
}

export type MailRecipientResponse = {
  id: number
  recipientKey: string | null
  recipientName: string | null
  email: string
  recipientType: MailRecipientType
  activeFlag: boolean
}

export type MailRecipientGroupResponse = {
  id: number
  groupKey: string
  groupName: string
  activeFlag: boolean
  recipients: MailRecipientResponse[]
}

export type MailRecipientSaveRequest = {
  id: number | null
  recipientKey: string | null
  recipientName: string | null
  email: string
  recipientType: MailRecipientType
  activeFlag: boolean
}

export type MailRecipientGroupSaveRequest = {
  groupKey: string
  groupName: string
  activeFlag: boolean
  recipients: MailRecipientSaveRequest[]
}

export type MailSendQueueResponse = {
  id: number
  mailType: string | null
  businessKey: string | null
  fromAddress: string
  fromName: string | null
  toAddresses: string[]
  toName: string | null
  ccAddresses: string[]
  bccAddresses: string[]
  subject: string
  body: string
  htmlFlag: boolean
  attachments: MailAttachmentResponse[]
  status: MailSendStatus
  retryCount: number
  maxRetryCount: number
  sentAt: string | null
  lastErrorMessage: string | null
}

export type MailActivateDraftRequest = {
  mailType?: string | null
  businessKey?: string | null
}

export type MailSendResult = {
  targetCount: number
  sentCount: number
  failedCount: number
  message: string
}

export type MailTestSendRequest = {
  toAddresses: string[]
  toName: string | null
  ccAddresses: string[]
  bccAddresses: string[]
  subject: string
  body: string
  htmlFlag: boolean
  attachments: MailAttachmentRequest[]
}

export type MailPdfSendRequest = {
  mailType: string | null
  businessKey: string | null
  recipientGroupKey: string | null
  recipientKey: string | null
  toAddresses: string[]
  toName: string | null
  ccAddresses: string[]
  bccAddresses: string[]
  subject: string
  body: string
  htmlFlag: boolean
  storageType: MailStorageType
  pdfFileKey: string
  pdfFileName: string
}

export type MailTemplateResponse = {
  id: number
  templateKey: string
  templateName: string
  fromAddress: string
  fromName: string | null
  subjectTemplate: string
  bodyTemplate: string
  htmlFlag: boolean
  activeFlag: boolean
}

export type MailTemplateSaveRequest = {
  templateKey: string
  templateName: string
  fromName: string | null
  subjectTemplate: string
  bodyTemplate: string
  htmlFlag: boolean
  activeFlag: boolean
}

export type MailTemplatePreviewRequest = {
  subjectTemplate: string
  bodyTemplate: string
  htmlFlag: boolean
  variables: Record<string, unknown>
}

export type MailTemplatePreviewResponse = {
  subject: string
  body: string
  htmlFlag: boolean
}
