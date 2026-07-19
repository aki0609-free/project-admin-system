import { z } from 'zod'

export const mailRecipientGroupSchema = z.object({
  id: z.number(),
  groupKey: z.string().min(1, '必須です'),
  groupName: z.string().min(1, '必須です'),
  activeFlag: z.boolean(),
  recipients: z.array(z.any()),
})

export const mailRecipientSchema = z.object({
  id: z.number(),
  recipientKey: z.string(),
  recipientName: z.string(),
  email: z.string().email('メール形式が不正です'),
  recipientType: z.enum(['TO', 'CC', 'BCC']),
  activeFlag: z.boolean(),
})