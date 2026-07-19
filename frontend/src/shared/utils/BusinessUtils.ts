export const formatZipCode = (zip: string) => {
  if (!zip) return '';
  
  const str = String(zip).replace(/\D/g, '');
  
  // 7桁に満たない場合のバリデーション（必要に応じて）
  if (str.length !== 7) {
    return str; // またはエラーを返すなど
  }
  
  return `${str.slice(0, 3)}-${str.slice(3)}`;
};

export const calculateAgeAt = (
  birthDate: string | null | undefined,
  targetDate: string | null | undefined,
) => {
  if (!birthDate || !targetDate) return ''

  const birth = new Date(birthDate)
  const target = new Date(targetDate)

  if (Number.isNaN(birth.getTime()) || Number.isNaN(target.getTime())) {
    return ''
  }

  let age = target.getFullYear() - birth.getFullYear()

  const beforeBirthday =
    target.getMonth() < birth.getMonth() ||
    (target.getMonth() === birth.getMonth() && target.getDate() < birth.getDate())

  if (beforeBirthday) {
    age--
  }

  return age >= 0 ? String(age) : ''
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
  link.click()

  window.URL.revokeObjectURL(url)
}