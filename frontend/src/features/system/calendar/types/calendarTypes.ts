export type CalendarType =
  | 'NATIONAL_HOLIDAY'
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
  dayType: CalendarDayType
}

export type CalendarHolidaySaveRequest = Omit<CalendarHoliday, 'editable'>

export type CalendarDayType = 'CLOSED' | 'OPEN'

export type CalendarWeekday =
  | 'MONDAY'
  | 'TUESDAY'
  | 'WEDNESDAY'
  | 'THURSDAY'
  | 'FRIDAY'
  | 'SATURDAY'
  | 'SUNDAY'

export type CalendarPolicy = {
  calendarType: CalendarType
  closedWeekdays: CalendarWeekday[]
  includedCalendarTypes: CalendarType[]
  fixedPeriods: CalendarFixedPeriod[]
  editable: boolean
}

export type CalendarPolicySaveRequest = Omit<
  CalendarPolicy,
  'editable' | 'fixedPeriods'
>

export type CalendarFixedPeriod = {
  name: string
  startMonthDay: string
  endMonthDay: string
}

export type CalendarHolidaySource =
  | 'WEEKDAY_RULE'
  | 'NATIONAL_HOLIDAY'
  | 'YEAR_END_NEW_YEAR'
  | 'INDIVIDUAL'

export type CalendarEffectiveHoliday = {
  holidayDate: string
  holidayName: string
  sources: CalendarHolidaySource[]
}
