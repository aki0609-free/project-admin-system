import { z } from 'zod'

export const mailTestSendSchema = z.object({
  to: z.string().min(1, '必須です'),
  toName: z.string(),
  cc: z.string(),
  bcc: z.string(),
  subject: z.string().min(1, '必須です'),
  body: z.string().min(1, '必須です'),
  attachmentFilePath: z.string(),
  attachmentFileName: z.string(),
})