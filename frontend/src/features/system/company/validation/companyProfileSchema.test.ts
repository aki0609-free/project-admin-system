import { describe, expect, it } from 'vitest'

import { createEmptyCompanyProfileForm, toCapitalAmount } from '../utils/companyProfileFactory'
import { companyProfileSchema } from './companyProfileSchema'

describe('companyProfileSchema', () => {
  it('accepts a valid company profile', () => {
    const form = createEmptyCompanyProfileForm()
    form.companyName = '株式会社テスト'
    form.email = 'office@example.com'
    form.websiteUrl = 'https://example.com'
    form.qualifiedInvoiceIssuerNumber = 'T1234567890123'

    expect(companyProfileSchema.safeParse(form).success).toBe(true)
  })

  it('rejects invalid contact and invoice values', () => {
    const form = createEmptyCompanyProfileForm()
    form.companyName = '株式会社テスト'
    form.email = 'invalid'
    form.websiteUrl = 'example.com'
    form.qualifiedInvoiceIssuerNumber = '12345'

    const result = companyProfileSchema.safeParse(form)

    expect(result.success).toBe(false)
    if (result.success) return
    expect(result.error.issues.map((issue) => issue.message)).toEqual(
      expect.arrayContaining([
        'メールアドレスの形式が正しくありません',
        'Webサイトはhttp://またはhttps://から入力してください',
        '登録番号はTと13桁の数字で入力してください',
      ]),
    )
  })

  it('accepts a comma-separated capital amount and rejects invalid text', () => {
    const form = createEmptyCompanyProfileForm()
    form.companyName = '株式会社テスト'
    form.capitalAmount = '10,000,000'

    expect(companyProfileSchema.safeParse(form).success).toBe(true)
    expect(toCapitalAmount(form.capitalAmount)).toBe(10_000_000)

    form.capitalAmount = '一千万円'
    const result = companyProfileSchema.safeParse(form)

    expect(result.success).toBe(false)
    if (result.success) return
    expect(result.error.issues.map((issue) => issue.message)).toContain(
      '資本金は半角数字で入力してください（カンマ区切り可）',
    )
  })
})
