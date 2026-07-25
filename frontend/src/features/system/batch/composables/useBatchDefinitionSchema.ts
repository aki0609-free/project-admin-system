import { z } from 'zod'

export const batchDefinitionSchema = z
  .object({
    id: z.number(),
    jobCode: z
      .string()
      .regex(
        /^[A-Z][A-Z0-9_]{1,99}$/,
        '英大文字で始まる英大文字・数字・アンダースコアで入力してください。',
      ),
    jobName: z.string().min(1, '必須です').max(200, '200文字以内です'),
    jobType: z.enum(['BACKUP', 'IMPORT', 'MAIL', 'REPORT', 'REPORT_MAIL']),
    targetCode: z.string().min(1, '必須です').max(100, '100文字以内です'),
    immediateExecutable: z.boolean(),
    scheduleEnabled: z.boolean(),
    scheduleType: z.enum(['NONE', 'CRON']),
    cronExpression: z.string().max(100, '100文字以内です'),
    activeFlag: z.boolean(),
    description: z.string().max(500, '500文字以内です'),
  })
  .superRefine((value, ctx) => {
    if (!value.scheduleEnabled) return

    if (value.scheduleType === 'NONE') {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        path: ['scheduleType'],
        message: 'Schedule有効時はscheduleTypeを選択してください。',
      })
    }

    if (value.scheduleType === 'CRON' && !value.cronExpression.trim()) {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        path: ['cronExpression'],
        message: 'CRON実行時はcronExpressionが必須です。',
      })
    }
  })
