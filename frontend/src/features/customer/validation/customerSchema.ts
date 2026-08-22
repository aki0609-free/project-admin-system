import { z } from 'zod'

const optionalText = (max: number, label: string) =>
  z.string().trim().max(max, `${label}は${max}文字以内で入力してください`)

const dayRuleSchema = z
  .object({
    type: z.enum([
      'BEFORE_DAYS',
      'EXACT_DAY',
      'DAY_OF_MONTH',
      'END_OF_MONTH',
      'AFTER_DAYS',
    ]),
    value: z.number().int().nullable().optional(),
    monthOffset: z.number().int().min(0).max(12).nullable().optional(),
  })
  .nullable()
  .superRefine((rule, context) => {
    if (!rule || rule.type === 'END_OF_MONTH') return
    if (rule.value == null || rule.value < 1 || rule.value > 31) {
      context.addIssue({
        code: 'custom',
        message: '日付指定は1日から31日の範囲で入力してください',
        path: ['value'],
      })
    }
  })

export const customerSchema = z
  .object({
    name: z.string().trim().min(1, '顧客名は必須です').max(255),
    furiganaName: optionalText(255, 'ふりがな'),
    shortName: optionalText(255, '短縮社名'),
    postNo: optionalText(255, '郵便番号'),
    address: optionalText(255, '住所'),
    representativeName: optionalText(255, '代表者名'),
    phone: optionalText(255, '電話番号'),
    jobType: optionalText(255, '職種'),
    contractFlag: optionalText(255, '契約有無'),
    invoiceType: z.enum(['PATTERN_1', 'PATTERN_2', 'PATTERN_3']),
    closingDayRule: dayRuleSchema,
    paymentDayRule: dayRuleSchema,
  })
  .passthrough()
