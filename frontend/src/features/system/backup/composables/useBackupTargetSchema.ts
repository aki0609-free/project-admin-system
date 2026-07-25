import { z } from 'zod'

export const backupTargetSchema = z
  .object({
    id: z.number(),
    targetCode: z
      .string()
      .regex(
        /^[A-Z][A-Z0-9_]{1,99}$/,
        '英大文字で始まる英大文字・数字・アンダースコアで入力してください。',
      ),
    targetName: z.string().min(1, '必須です').max(200, '200文字以内です'),
    tableName: z
      .string()
      .regex(
        /^[A-Za-z][A-Za-z0-9_]{0,199}$/,
        'テーブル名の形式が不正です。',
      ),
    description: z.string().max(500, '500文字以内です'),
    outputMode: z.enum(['DOWNLOAD', 'SERVER_FILE', 'BOTH']),
    outputDir: z
      .string()
      .max(500, '500文字以内です')
      .refine(
        value => !value || (!value.startsWith('/') && !value.split(/[\\/]/).some(part => !part || part === '.' || part === '..')),
        '相対フォルダを指定してください。',
      ),
    fileNamePattern: z
      .string()
      .max(200, '200文字以内です')
      .refine(
        value => !value || (!/[\\/]/.test(value) && value.toLowerCase().endsWith('.csv')),
        'パスを含めず.csvで終了してください。',
      )
      .refine(
        value => !value || value.includes('{timestamp}'),
        '上書き防止のため{timestamp}を含めてください。',
      ),
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
