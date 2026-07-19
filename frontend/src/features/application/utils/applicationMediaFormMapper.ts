import type {
  ApplicationMediaCreateRequest,
  ApplicationMediaDetail,
  ApplicationMediaEditForm,
  ApplicationMediaUpdateRequest,
} from '@/features/application/types/types'

export const createEmptyApplicationMediaForm = (): ApplicationMediaEditForm => ({
  id: null,
  recruitmentCompany: '',
  mediaName: '',
  mediaType: '',
  mediaArea: '',
  mediaSlots: null,
  mediaYearMonth: '',
  cost: null,
})

export const toApplicationMediaEditForm = (
  detail: ApplicationMediaDetail,
): ApplicationMediaEditForm => ({
  id: detail.id,
  recruitmentCompany: detail.recruitmentCompany ?? '',
  mediaName: detail.mediaName ?? '',
  mediaType: detail.mediaType ?? '',
  mediaArea: detail.mediaArea ?? '',
  mediaSlots: detail.mediaSlots ?? null,
  mediaYearMonth: detail.mediaYearMonth ?? '',
  cost: detail.cost ?? null,
})

export const toApplicationMediaCreatePayload = (
  form: ApplicationMediaEditForm,
): ApplicationMediaCreateRequest => ({
  recruitmentCompany: form.recruitmentCompany ?? '',
  mediaName: form.mediaName,
  mediaType: form.mediaType ?? '',
  mediaArea: form.mediaArea ?? '',
  mediaSlots: form.mediaSlots,
  mediaYearMonth: form.mediaYearMonth,
  cost: form.cost,
})

export const toApplicationMediaUpdatePayload = (
  form: ApplicationMediaEditForm,
): ApplicationMediaUpdateRequest => ({
  id: form.id as number,
  recruitmentCompany: form.recruitmentCompany ?? '',
  mediaName: form.mediaName,
  mediaType: form.mediaType ?? '',
  mediaArea: form.mediaArea ?? '',
  mediaSlots: form.mediaSlots,
  mediaYearMonth: form.mediaYearMonth,
  cost: form.cost,
})