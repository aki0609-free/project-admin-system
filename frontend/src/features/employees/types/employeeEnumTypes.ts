export const employmentTypeOptions = [
  { title: '正社員', value: 'FULL_TIME' },
  { title: '契約社員', value: 'CONTRACT' },
  { title: 'パート', value: 'PART_TIME' },
  { title: '派遣', value: 'TEMPORARY' },
  { title: '日雇い', value: 'DAILY_WORKER' },
] as const

export const employmentStatusOptions = [
  { title: '在籍', value: 'ACTIVE' },
  { title: '休職', value: 'LEAVE' },
  { title: '退職', value: 'RESIGNED' },
] as const

export const toOptionTitle = (
  options: readonly { title: string; value: string }[],
  value: string | null | undefined,
) => {
  if (!value) return ''
  return options.find((option) => option.value === value)?.title ?? value
}