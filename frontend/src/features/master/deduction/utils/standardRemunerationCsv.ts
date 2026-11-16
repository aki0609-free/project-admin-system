import type {
  StandardRemunerationEditorResponse,
  StandardRemunerationEditorRow,
} from '@/features/master/deduction/types/standardRemunerationEditorTypes'

const HEADERS = [
  'employeeCode',
  'healthStandardRemuneration',
  'pensionStandardRemuneration',
  'effectiveFrom',
  'effectiveTo',
  'sourceType',
  'note',
]

function escapeCsv(value: unknown): string {
  const text = String(value ?? '')
  return /[",\r\n]/.test(text) ? `"${text.replaceAll('"', '""')}"` : text
}

export function createStandardRemunerationCsvTemplate(
  editor: StandardRemunerationEditorResponse,
): string {
  const existingEmployeeIds = new Set(editor.rows.map(row => row.employeeId))
  const unconfiguredEmployees = editor.employees
    .filter(employee => !existingEmployeeIds.has(employee.employeeId))
    .map(employee => ({
        employeeCode: employee.employeeCode,
        healthStandardRemuneration: '',
        pensionStandardRemuneration: '',
        effectiveFrom: '',
        effectiveTo: '',
        sourceType: 'MANUAL',
        note: '',
      }))
  const source = [...editor.rows, ...unconfiguredEmployees]
  const lines = source.map(row => [
    row.employeeCode,
    row.healthStandardRemuneration,
    row.pensionStandardRemuneration,
    row.effectiveFrom,
    row.effectiveTo,
    row.sourceType,
    row.note,
  ].map(escapeCsv).join(','))
  return `\uFEFF${HEADERS.join(',')}\r\n${lines.join('\r\n')}\r\n`
}

function parseLine(line: string): string[] {
  const result: string[] = []
  let value = ''
  let quoted = false
  for (let i = 0; i < line.length; i += 1) {
    const char = line[i]
    if (char === '"' && quoted && line[i + 1] === '"') {
      value += '"'
      i += 1
    } else if (char === '"') {
      quoted = !quoted
    } else if (char === ',' && !quoted) {
      result.push(value)
      value = ''
    } else {
      value += char
    }
  }
  result.push(value)
  return result
}

export function parseStandardRemunerationCsv(
  csv: string,
  editor: StandardRemunerationEditorResponse,
): StandardRemunerationEditorRow[] {
  const lines = csv.replace(/^\uFEFF/, '').split(/\r?\n/).filter(line => line.trim())
  if (lines.length < 2) throw new Error('CSVにデータ行がありません。')
  const headers = parseLine(lines[0] ?? '')
  if (HEADERS.some((header, index) => headers[index] !== header)) {
    throw new Error('標準報酬月額CSVのヘッダー形式が正しくありません。')
  }
  const employeeByCode = new Map(editor.employees.map(employee => [employee.employeeCode, employee]))
  const currentByKey = new Map(editor.rows.map(row => [`${row.employeeCode}:${row.effectiveFrom}`, row]))
  return lines.slice(1).map((line, index) => {
    const values = parseLine(line)
    const employeeCode = values[0] ?? ''
    const effectiveFrom = values[3] ?? ''
    const employee = employeeByCode.get(employeeCode)
    if (!employee) throw new Error(`${index + 2}行目の社員コードが存在しません。`)
    const health = Number((values[1] ?? '').replaceAll(',', ''))
    const pension = Number((values[2] ?? '').replaceAll(',', ''))
    if (!effectiveFrom || !Number.isInteger(health) || !Number.isInteger(pension)) {
      throw new Error(`${index + 2}行目の適用開始日または金額が不正です。`)
    }
    const current = currentByKey.get(`${employeeCode}:${effectiveFrom}`)
    return {
      currentRecordId: current?.currentRecordId ?? null,
      employeeId: employee.employeeId,
      employeeCode: employee.employeeCode,
      employeeName: employee.employeeName,
      effectiveFrom,
      effectiveTo: values[4] || null,
      healthStandardRemuneration: health,
      pensionStandardRemuneration: pension,
      sourceType: values[5] || 'MANUAL',
      note: values[6] || null,
      changed: true,
      closed: false,
    }
  })
}
