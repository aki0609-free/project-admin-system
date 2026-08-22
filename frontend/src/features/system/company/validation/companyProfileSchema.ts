import { z } from 'zod'

const optionalEmail = z
  .string()
  .trim()
  .max(255, 'メールアドレスは255文字以内です')
  .refine(
    (value) => !value || /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value),
    'メールアドレスの形式が正しくありません',
  )

const optionalWebsite = z
  .string()
  .trim()
  .max(500, 'Webサイトは500文字以内です')
  .refine(
    (value) => !value || /^https?:\/\/[^\s]+$/i.test(value),
    'Webサイトはhttp://またはhttps://から入力してください',
  )

const optionalInvoiceIssuerNumber = z
  .string()
  .trim()
  .refine(
    (value) => !value || /^T\d{13}$/i.test(value),
    '登録番号はTと13桁の数字で入力してください',
  )

const optionalCapitalAmount = z
  .string()
  .trim()
  .refine(
    (value) => !value || /^(?:\d{1,3}(?:,\d{3})*|\d+)(?:\.\d{1,2})?$/.test(value),
    '資本金は半角数字で入力してください（カンマ区切り可）',
  )
  .refine((value) => {
    if (!value) return true
    const normalized = value.replaceAll(',', '')
    return Number.isFinite(Number(normalized)) && Number(normalized) >= 0
  }, '資本金は0以上で入力してください')

export const companyProfileSchema = z
  .object({
    companyCode: z.string().trim().min(1, '会社コードは必須です').max(100),
    companyName: z.string().trim().min(1, '会社名は必須です').max(255),
    companyNameKana: z.string().max(255),
    shortName: z.string().max(100),
    representativeTitle: z.string().max(100),
    representativeName: z.string().max(255),
    postalCode: z.string().max(20),
    prefecture: z.string().max(100),
    city: z.string().max(100),
    addressLine1: z.string().max(255),
    addressLine2: z.string().max(255),
    phone: z.string().max(50),
    fax: z.string().max(50),
    email: optionalEmail,
    websiteUrl: optionalWebsite,
    capitalAmount: optionalCapitalAmount,
    permitNumber: z.string().max(255),
    qualifiedInvoiceIssuerNumber: optionalInvoiceIssuerNumber,
    serviceArea: z.string().max(500),
    invoiceBankName: z.string().max(255),
    invoiceBankBranchName: z.string().max(255),
    invoiceBankAccountType: z.string().max(50),
    invoiceBankAccountNumber: z.string().max(100),
    invoiceBankAccountHolder: z.string().max(255),
    activeFlag: z.boolean(),
  })
  .passthrough()
