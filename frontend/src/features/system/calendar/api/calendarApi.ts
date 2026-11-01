import { del, get, post } from '@/shared/api/http'

import type {
  CalendarHoliday,
  CalendarHolidaySaveRequest,
  CalendarType,
} from '../types/calendarTypes'

const basePath = '/api/system/calendars'

export const getCalendarHolidays = (
  calendarType: CalendarType,
  year: number,
) => get<CalendarHoliday[]>(
  `${basePath}/holidays?calendarType=${calendarType}&year=${year}`,
)

export const saveCalendarHoliday = (
  request: CalendarHolidaySaveRequest,
) => post<CalendarHoliday, CalendarHolidaySaveRequest>(
  `${basePath}/holidays`,
  request,
)

export const deleteCalendarHoliday = (id: number) =>
  del<undefined>(`${basePath}/holidays/${id}`)
