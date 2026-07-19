import type {
  MailTemplateResponse,
  MailTemplateSaveRequest,
} from '@/features/system/mail/types/mailApiTypes'
import type { MailTemplateForm } from '@/features/system/mail/types/mailFormTypes'

export const createEmptyMailTemplateForm = (): MailTemplateForm => ({
  id: 0,
  templateKey: '',
  templateName: '',
  fromAddress: '環境設定のSMTPアドレスを使用',
  fromName: 'ProjectAdmin',
  subjectTemplate: '',
  bodyTemplate: '',
  htmlFlag: false,
  activeFlag: true,
})

export const toMailTemplateForm = (
  template: MailTemplateResponse,
): MailTemplateForm => ({
  id: template.id,
  templateKey: template.templateKey,
  templateName: template.templateName,
  fromAddress: template.fromAddress,
  fromName: template.fromName ?? '',
  subjectTemplate: template.subjectTemplate,
  bodyTemplate: template.bodyTemplate,
  htmlFlag: template.htmlFlag,
  activeFlag: template.activeFlag,
})

export const toMailTemplateSaveRequest = (
  form: MailTemplateForm,
): MailTemplateSaveRequest => ({
  templateKey: form.templateKey.trim(),
  templateName: form.templateName.trim(),
  fromName: form.fromName.trim() || null,
  subjectTemplate: form.subjectTemplate,
  bodyTemplate: form.bodyTemplate,
  htmlFlag: form.htmlFlag,
  activeFlag: form.activeFlag,
})
