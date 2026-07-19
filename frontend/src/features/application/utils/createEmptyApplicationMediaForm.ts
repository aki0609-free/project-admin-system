import type { ApplicationMediaEditForm } from '@/features/application/types/types'

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