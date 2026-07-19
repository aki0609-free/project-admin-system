import type {
  ApplicantCreateRequest,
  ApplicantUpdateRequest,
} from '@/features/application/types/applicantApiTypes'

import type { ApplicantPersistedRow } from '@/features/application/types/applicantTypes'
import { toYearMonth } from '@/shared/utils/DateUtils'

function toNullableString(value: string | null | undefined): string | null {
  if (value == null || value === '') return null
  return value
}

export function toApplicantCreateRequest(
  row: ApplicantPersistedRow,
): ApplicantCreateRequest {
  return {
    applicationNo: String(row.no),
    contractType: toNullableString(row.contractType),
    name: row.name,
    furiganaName: toNullableString(row.furiganaName),
    birthDate: row.birthDate,
    gender: toNullableString(row.gender),
    contactDate: row.contactDate,
    recruitmentStatus: toNullableString(row.recruitmentStatus),
    dailyWage:
      row.dailyWageAtJoin == null ? null : String(row.dailyWageAtJoin),

    employmentDate: row.joinDateWithoutInsurance,
    employmentDateWithInsurance: row.joinDateWithInsurance,
    terminationDate: row.leaveDateWithoutInsurance,
    terminationDateWithInsurance: row.leaveDateWithInsurance,

    jobCategory: toNullableString(row.industryBeforeLeave),
    previousClient: toNullableString(row.clientBeforeLeave),
    resignationStatus: toNullableString(row.retirementStatus),
    resignationReason: toNullableString(row.estimatedRetirementReason),
    applicationReason1: toNullableString(row.applicationReason1),
    applicationReason2: toNullableString(row.applicationReason2),
    recruitmentCompany: toNullableString(row.recruitmentCompany),
    mediaType: toNullableString(row.mediaType),

    dormitoryInfo: {
      needsDormitory: toNullableString(row.needsDormitory),
      roomType: toNullableString(row.roomType),
      rent: row.dormitoryFee == null ? null : String(row.dormitoryFee),
    },

    searchKeywordInfo: {
      searchPlace: toNullableString(row.sourceWhere),
      searchWebsite: toNullableString(row.searchSite),
      searchKeyword: toNullableString(row.keyword),
      desiredNextJob: toNullableString(row.repostedTo),
    },

    previousJobInfo: {
      previousJob: toNullableString(row.previousJob),
      previousJobPeriod: toNullableString(row.previousJobPeriod),
      insuredBefore: toNullableString(row.insuredBefore),
      dormitoryExperience: toNullableString(row.dormitoryExperience),
      previousRent:
        row.previousDormitoryFee == null
          ? null
          : String(row.previousDormitoryFee),
    },

    applicationMedia: {
      id: null,
      mediaName: toNullableString(row.mediaName),
      mediaArea: toNullableString(row.mediaArea),
      mediaSlots: row.mediaSlots,
      mediaYearMonth: toYearMonth(row.contactDate)
    },
  }
}

export function toApplicantUpdateRequest(
  row: ApplicantPersistedRow,
): ApplicantUpdateRequest {
  return {
    id: row.id,
    ...toApplicantCreateRequest(row),
  }
}