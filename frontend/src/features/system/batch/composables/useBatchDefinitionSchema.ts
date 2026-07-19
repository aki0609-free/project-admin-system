import { z } from 'zod'

export const batchDefinitionSchema = z
  .object({
    id: z.number(),
    jobCode: z.string().min(1, '必須です'),
    jobName: z.string().min(1, '必須です'),
    jobType: z.enum(['BACKUP', 'IMPORT', 'MAIL', 'REPORT', 'REPORT_MAIL']),
    targetCode: z.string().min(1, '必須です'),
    immediateExecutable: z.boolean(),
    scheduleEnabled: z.boolean(),
    scheduleType: z.enum(['NONE', 'CRON']),
    cronExpression: z.string(),
    activeFlag: z.boolean(),
    description: z.string(),
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