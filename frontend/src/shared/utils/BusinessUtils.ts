export const formatZipCode = (zip: string) => {
  if (!zip) return ''

  const str = String(zip).replace(/\D/g, '')

  // 7桁に満たない場合のバリデーション（必要に応じて）
  if (str.length !== 7) {
    return str // またはエラーを返すなど
  }

  return `${str.slice(0, 3)}-${str.slice(3)}`
}

export const calculateAgeAt = (
  birthDate: string | null | undefined,
  targetDate: string | null | undefined,
) => {
  if (!birthDate || !targetDate) return ''

  const birth = parseDateParts(birthDate)
  const target = parseDateParts(targetDate)
  if (!birth || !target) {
    return ''
  }

  let age = target.year - birth.year

  const beforeBirthday =
    target.month < birth.month || (target.month === birth.month && target.day < birth.day)

  if (beforeBirthday) {
    age--
  }

  return age >= 0 ? String(age) : ''
}

export const calculateCurrentAge = (
  birthDate: string | null | undefined,
  currentDate = new Date(),
) => {
  const targetDate = [
    currentDate.getFullYear(),
    String(currentDate.getMonth() + 1).padStart(2, '0'),
    String(currentDate.getDate()).padStart(2, '0'),
  ].join('-')

  return calculateAgeAt(birthDate, targetDate)
}

const parseDateParts = (value: string) => {
  const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(value)
  if (!match) return null

  const [, yearText, monthText, dayText] = match
  const year = Number(yearText)
  const month = Number(monthText)
  const day = Number(dayText)
  const date = new Date(year, month - 1, day)

  if (date.getFullYear() !== year || date.getMonth() + 1 !== month || date.getDate() !== day) {
    return null
  }

  return { year, month, day }
}

export const formatAge = (value: unknown) => {
  if (value == null || value === '') {
    return ''
  }

  return `${value}歳`
}

export function downloadBlob(blob: Blob, fileName: string) {
  const url = window.URL.createObjectURL(blob)
  const link = document.createElement('a')

  link.href = url
  link.download = fileName
  link.style.display = 'none'
  document.body.appendChild(link)
  link.click()
  link.remove()

  // Safari 等では click 直後に URL を破棄するとダウンロードが始まらない。
  window.setTimeout(() => window.URL.revokeObjectURL(url), 1_000)
}
