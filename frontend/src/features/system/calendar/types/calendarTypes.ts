export type CalendarType =
  | 'GOVERNMENT'
  | 'FINANCIAL_INSTITUTION'
  | 'COMPANY'

export type CalendarHoliday = {
  id: number | null
  calendarType: CalendarType
  holidayDate: string
  holidayName: string
  note: string | null
  activeFlag: boolean
  editable: boolean
}

export type CalendarHolidaySaveRequest = Omit<CalendarHoliday, 'editable'>
