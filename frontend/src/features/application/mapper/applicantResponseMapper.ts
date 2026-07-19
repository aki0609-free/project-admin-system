import type { ApplicantListItemResponse } from '@/features/application/types/applicantApiTypes'
import type { ApplicantRow } from '@/features/application/types/applicantTypes'
import {
  toApplicantContractType,
  toApplicantGender,
  toApplicantRecruitmentStatus,
  toApplicantRetirementStatus,
  toApplicantRoomType,
  toApplicantYesNo,
} from '@/features/application/mapper/applicantEnumMapper'

export function toApplicantRow(
  item: ApplicantListItemResponse,
): ApplicantRow {
  return {
    id: item.id,
    no: Number(item.applicationNo) || 0,

    name: item.name ?? '',
    furiganaName: item.furiganaName ?? '',
    contractType: toApplicantContractType(item.contractType),
    retirementStatus: toApplicantRetirementStatus(item.resignationStatus),
    birthDate: item.birthDate ?? null,
    gender: toApplicantGender(item.gender),

    joinDateWithoutInsurance: item.employmentDate ?? null,
    leaveDateWithoutInsurance: item.terminationDate ?? null,
    joinDateWithInsurance: item.employmentDateWithInsurance ?? null,
    leaveDateWithInsurance: item.terminationDateWithInsurance ?? null,

    contactDate: item.contactDate ?? null,
    recruitmentStatus: toApplicantRecruitmentStatus(item.recruitmentStatus),
    sourceWhere: item.searchKeywordInfo?.searchPlace ?? '',
    searchSite: item.searchKeywordInfo?.searchWebsite ?? '',
    keyword: item.searchKeywordInfo?.searchKeyword ?? '',
    repostedTo: item.searchKeywordInfo?.desiredNextJob ?? '',
    applicationReason1: item.applicationReason1 ?? '',
    applicationReason2: item.applicationReason2 ?? '',

    recruitmentCompany: item.recruitmentCompany ?? '',
    mediaType: item.mediaType ?? '',
    mediaName: item.applicationMedia?.mediaName ?? '',
    mediaArea: item.applicationMedia?.mediaArea ?? '',
    mediaSlots: item.applicationMedia?.mediaSlots ?? null,
    recruitmentUnitPrice: item.applicationMedia?.unitPrice ?? 0,

    dailyWageAtJoin:
      item.dailyWage == null || item.dailyWage === ''
        ? null
        : Number(item.dailyWage),

    needsDormitory: toApplicantYesNo(item.dormitoryInfo?.needsDormitory),
    roomType: toApplicantRoomType(item.dormitoryInfo?.roomType),
    dormitoryFee:
      item.dormitoryInfo?.rent == null || item.dormitoryInfo.rent === ''
        ? null
        : Number(item.dormitoryInfo.rent),

    previousJob: item.previousJobInfo?.previousJob ?? '',
    previousJobPeriod: item.previousJobInfo?.previousJobPeriod ?? '',
    insuredBefore: toApplicantYesNo(item.previousJobInfo?.insuredBefore),
    dormitoryExperience: toApplicantYesNo(item.previousJobInfo?.dormitoryExperience),
    previousDormitoryFee:
      item.previousJobInfo?.previousRent == null || item.previousJobInfo.previousRent === ''
        ? null
        : Number(item.previousJobInfo.previousRent),

    clientBeforeLeave: item.previousClient ?? '',
    industryBeforeLeave: item.jobCategory ?? '',
    estimatedRetirementReason: item.resignationReason ?? '',

    ageAtJoin: undefined,
    ageAtLeave: undefined,
    currentTenure: undefined,
    tenureWithoutInsurance: undefined,
    tenureWithInsurance: undefined,
  }
}