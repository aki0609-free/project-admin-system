import { del, get, post } from '@/shared/api/http'
import type { YearEndAdjustment } from '../types/yearEndAdjustmentTypes'

const basePath = '/api/operation/year-end-adjustments'

export const getYearEndAdjustments = (taxYear: number) =>
  get<YearEndAdjustment[]>(`${basePath}/${taxYear}`)

export const saveYearEndAdjustment = (request: YearEndAdjustment) =>
  post<YearEndAdjustment, YearEndAdjustment>(basePath, request)

export const deleteYearEndAdjustment = (id: number) =>
  del<undefined>(`${basePath}/${id}`)
