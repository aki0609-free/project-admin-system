import type { ResidentTaxEditorResponse } from '@/features/master/deduction/types/residentTaxEditorTypes'

const FISCAL_MONTHS = [6, 7, 8, 9, 10, 11, 12, 1, 2, 3, 4, 5] as const

export function resolveResidentTaxFiscalYear(targetDate: string): number {
  const [yearText, monthText] = targetDate.split('-')
  const year = Number(yearText)
  const month = Number(monthText)

  if (!Number.isInteger(year) || !Number.isInteger(month) || month < 1 || month > 12) {
    throw new Error('参照基準日が正しくありません。')
  }

  return month < 6 ? year - 1 : year
}

export function createResidentTaxCsvTemplate(
  response: ResidentTaxEditorResponse,
  fiscalYear: number,
): string {
  const header = [
    '社員ID',
    '社員コード',
    '氏名',
    '年度',
    ...FISCAL_MONTHS.map((month) => `${month}月`),
  ]

  const rows = response.employees.map((employee) => [
    employee.employeeId,
    employee.employeeCode,
    employee.employeeName,
    fiscalYear,
    ...FISCAL_MONTHS.map(() => ''),
  ])

  return `\uFEFF${[header, ...rows]
    .map((row) => row.map(escapeCsvValue).join(','))
    .join('\r\n')}\r\n`
}

function escapeCsvValue(value: string | number): string {
  const text = String(value)
  if (!/[",\r\n]/.test(text)) return text

  return `"${text.replaceAll('"', '""')}"`
}
