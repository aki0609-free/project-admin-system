export function formatCurrency(value: number | null | undefined) {
  if (value == null) return ''

  const num = Number(value)
  if (Number.isNaN(num)) return ''

  return `${num.toLocaleString()}円`
}