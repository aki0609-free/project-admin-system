import { del, get, post, put } from '@/shared/api/http'

import type {
  CalendarHoliday,
  CalendarEffectiveHoliday,
  CalendarHolidaySaveRequest,
  CalendarPolicy,
  CalendarPolicySaveRequest,
  CalendarType,
} from '../types/calendarTypes'

const basePath = '/api/system/calendars'

export const getCalendarHolidays = (
  calendarType: CalendarType,
  year: number,
) => get<CalendarHoliday[]>(
  `${basePath}/holidays?calendarType=${calendarType}&year=${year}`,
)

export const getCalendarEffectiveHolidays = (
  calendarType: CalendarType,
  year: number,
) => get<CalendarEffectiveHoliday[]>(
  `${basePath}/effective-holidays?calendarType=${calendarType}&year=${year}`,
)

export const saveCalendarHoliday = (
  request: CalendarHolidaySaveRequest,
) => post<CalendarHoliday, CalendarHolidaySaveRequest>(
  `${basePath}/holidays`,
  request,
)

export const deleteCalendarHoliday = (id: number) =>
  del<undefined>(`${basePath}/holidays/${id}`)

export const getCalendarPolicy = (calendarType: CalendarType) =>
  get<CalendarPolicy>(`${basePath}/policy?calendarType=${calendarType}`)

export const saveCalendarPolicy = (request: CalendarPolicySaveRequest) =>
  put<CalendarPolicy, CalendarPolicySaveRequest>(
    `${basePath}/policy`,
    request,
  )
