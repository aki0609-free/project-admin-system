import { describe, expect, it } from 'vitest'

import { toApplicantCreateRequest } from './applicantRequestMapper'
import { createEmptyApplicant } from '../utils/createEmptyApplicantForm'

describe('applicantRequestMapper', () => {
  it('keeps the existing application media id when an applicant is edited', () => {
    const row = createEmptyApplicant(101)
    row.name = '応募者テスト'
    row.contactDate = '2026-09-13'
    row.applicationMediaId = 42
    row.mediaName = '求人媒体A'

    const request = toApplicantCreateRequest(row)

    expect(request.applicationMedia).toEqual({
      id: 42,
      mediaName: '求人媒体A',
      mediaArea: null,
      mediaSlots: null,
      mediaYearMonth: '2026-09',
    })
  })
})
