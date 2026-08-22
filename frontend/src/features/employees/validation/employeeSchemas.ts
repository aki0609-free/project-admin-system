import { z } from 'zod'
import type { EmployeeForm } from '../types/employeeFormTypes'

const optionalText = (max: number) => z.string().max(max)

export const employeeBasicSchema = z.object({
  id: z.number(),
  employeeCode: z.string().trim().min(1, '社員コードは必須です。').max(100),
  employeeName: z.string().trim().min(1, '氏名は必須です。').max(200),
  employeeNameKana: optionalText(200),
  gender: z.enum(['MALE', 'FEMALE', 'OTHER']).nullable(),
  birthDate: z.string(),
  hireDate: z.string(),
  resignDate: z.string(),
  employmentType: z.enum(['FULL_TIME', 'CONTRACT', 'PART_TIME', 'TEMPORARY', 'DAILY_WORKER']),
  employmentStatus: z.enum(['ACTIVE', 'LEAVE', 'RESIGNED']),
  phone: optionalText(50),
  email: z
    .string()
    .max(255)
    .email('メールアドレスの形式が不正です。')
    .or(z.literal('')),
  postalCode: optionalText(20),
  address: optionalText(500),
  // 旧API互換項目。画面の適用設定・検証は payrollItemSettings 側で行う。
  dormitoryFlag: z.boolean(),
  dormitoryType: z.enum(['SINGLE_ROOM', 'SHARED_ROOM']).nullable(),
  activeFlag: z.boolean(),
  payrollProfile: z.unknown(),
  contract: z.unknown(),
})

export const employeePayrollSchema = z.object({
  taxCategory: z.enum(['KOU', 'OTSU', 'HEI']),
  taxDependentCount: z.number().min(0, '扶養人数は0以上で指定してください。'),
  dependentFlag: z.boolean(),
  dependentOfOtherFlag: z.boolean(),
  paidLeaveRemainingDays: z.number().min(0, '有給残日数は0以上で指定してください。'),
  incomeTaxCalcFlag: z.boolean(),
  residentTaxCalcFlag: z.boolean(),
  residentTaxMonthly: z.number().min(0, '住民税月額は0以上で指定してください。'),
  employmentInsuranceFlag: z.boolean(),
  socialInsuranceFlag: z.boolean(),
  healthInsuranceFlag: z.boolean(),
  pensionInsuranceFlag: z.boolean(),
  careInsuranceFlag: z.boolean(),
  commuteAllowanceMonthly: z.number().min(0, '通勤手当月額は0以上で指定してください。'),
})

export const employeeContractSchema = z
  .object({
    contractStartDate: z.string(),
    contractEndDate: z.string(),
    renewalFlag: z.boolean(),
    salaryType: z.enum(['MONTHLY', 'WEEKLY', 'DAILY', 'HOURLY']),
    paymentCycle: z.enum(['DAILY', 'WEEKLY', 'MONTHLY']),
    monthlySalary: z.number().min(0, '月給は0以上で指定してください。'),
    weeklyWage: z.number().min(0, '週給は0以上で指定してください。'),
    dailyWage: z.number().min(0, '日給は0以上で指定してください。'),
    hourlyWage: z.number().min(0, '時給は0以上で指定してください。'),
    standardWorkingHours: z.number().min(0, '標準労働時間は0以上で指定してください。'),
    note: z.string().max(1000, '契約メモは1000文字以内で入力してください。'),
  })
  .superRefine((value, context) => {
    if (
      value.contractStartDate &&
      value.contractEndDate &&
      value.contractEndDate < value.contractStartDate
    ) {
      context.addIssue({
        code: 'custom',
        path: ['contractEndDate'],
        message: '契約終了日は契約開始日以降で指定してください。',
      })
    }
  })

export type EmployeeValidationTab = 'basic' | 'payrollItems' | 'payroll' | 'contract'

export type EmployeeValidationError = {
  tab: EmployeeValidationTab
  message: string
}

export const validateEmployeeForm = (form: EmployeeForm): EmployeeValidationError | null => {
  const basicResult = employeeBasicSchema.safeParse(form)
  if (!basicResult.success) {
    return {
      tab: 'basic',
      message: basicResult.error.issues[0]?.message ?? '基本情報を確認してください。',
    }
  }

  const payrollItemError = validatePayrollItemSettings(form.payrollItemSettings)
  if (payrollItemError) {
    return { tab: 'payrollItems', message: payrollItemError }
  }

  const payrollResult = employeePayrollSchema.safeParse(form.payrollProfile)
  if (!payrollResult.success) {
    return {
      tab: 'payroll',
      message: payrollResult.error.issues[0]?.message ?? '給与・税金設定を確認してください。',
    }
  }

  const contractResult = employeeContractSchema.safeParse(form.contract)
  if (!contractResult.success) {
    return {
      tab: 'contract',
      message: contractResult.error.issues[0]?.message ?? '契約情報を確認してください。',
    }
  }

  return null
}

const validatePayrollItemSettings = (
  settings: EmployeeForm['payrollItemSettings'],
): string | null => {
  for (const setting of settings) {
    if (!setting.enabled) continue

    for (const definition of setting.parameterDefinitions) {
      // 値をRule側で解決する項目は従業員画面の入力対象ではない。
      if (definition.ruleValueResolverKey) continue

      const value = setting.parameters[definition.key]?.trim() ?? ''
      if (definition.required && !value) {
        return `${setting.displayName}の「${definition.displayName}」は必須です。`
      }
      if (!value) continue

      if (definition.inputType === 'NUMBER' && !Number.isFinite(Number(value))) {
        return `${setting.displayName}の「${definition.displayName}」は数値で入力してください。`
      }
      if (definition.inputType === 'DATE' && !/^\d{4}-\d{2}-\d{2}$/.test(value)) {
        return `${setting.displayName}の「${definition.displayName}」は日付で入力してください。`
      }
      if (
        definition.inputType === 'BOOLEAN' &&
        value.toLowerCase() !== 'true' &&
        value.toLowerCase() !== 'false'
      ) {
        return `${setting.displayName}の「${definition.displayName}」の形式が不正です。`
      }
      if (
        definition.inputType === 'SELECT' &&
        !definition.options.some((option) => option.value === value)
      ) {
        return `${setting.displayName}の「${definition.displayName}」の選択値が不正です。`
      }
    }
  }

  return null
}
