const parseLocalDate = (value: string) => {
  const [year, month, day] = value.split('-').map(Number)

  if (!year || !month || !day) {
    throw new Error(`Invalid local date: ${value}`)
  }

  return { year, month, day }
}

export const formatLocalDate = (date: Date) => {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

export const getCalendarMonthRange = (value: string) => {
  const { year, month } = parseLocalDate(value)

  return {
    from: formatLocalDate(new Date(year, month - 1, 1)),
    to: formatLocalDate(new Date(year, month, 0)),
  }
}

export const moveCalendarMonth = (value: string, amount: number) => {
  const { year, month } = parseLocalDate(value)
  return formatLocalDate(new Date(year, month - 1 + amount, 1))
}
