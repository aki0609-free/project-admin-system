import { z } from 'zod'

export const backupTargetSchema = z
  .object({
    id: z.number(),
    targetCode: z.string().min(1, '必須です'),
    targetName: z.string().min(1, '必須です'),
    tableName: z.string().min(1, '必須です'),
    description: z.string(),
    outputMode: z.enum(['DOWNLOAD', 'SERVER_FILE', 'BOTH']),
    outputDir: z.string(),
    fileNamePattern: z.string(),
    zipRequired: z.boolean(),
    includeHeader: z.boolean(),
    backupEnabled: z.boolean(),
    activeFlag: z.boolean(),
    columns: z.array(z.any()),
  })
  .superRefine((value, ctx) => {
    if (
      value.outputMode !== 'DOWNLOAD' &&
      !value.outputDir.trim()
    ) {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        path: ['outputDir'],
        message: 'SERVER_FILE または BOTH の場合、outputDir は必須です。',
      })
    }
  })