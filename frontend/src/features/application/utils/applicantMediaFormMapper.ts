import type {
  ApplicantPersistedRow,
  ApplicantRow,
} from '@/features/application/types/applicantTypes'

export function toApplicantPersistedRow(row: ApplicantRow): ApplicantPersistedRow {
  return {
    id: row.id,
    no: row.no,
    name: row.name,
    furiganaName: row.furiganaName,
    contractType: row.contractType,
    retirementStatus: row.retirementStatus,
    birthDate: row.birthDate,
    gender: row.gender,

    joinDateWithoutInsurance: row.joinDateWithoutInsurance,
    leaveDateWithoutInsurance: row.leaveDateWithoutInsurance,
    joinDateWithInsurance: row.joinDateWithInsurance,
    leaveDateWithInsurance: row.leaveDateWithInsurance,

    contactDate: row.contactDate,
    recruitmentStatus: row.recruitmentStatus,
    sourceWhere: row.sourceWhere,
    searchSite: row.searchSite,
    keyword: row.keyword,
    repostedTo: row.repostedTo,
    applicationReason1: row.applicationReason1,
    applicationReason2: row.applicationReason2,

    recruitmentCompany: row.recruitmentCompany,
    mediaType: row.mediaType,
    mediaName: row.mediaName,
    mediaArea: row.mediaArea,
    mediaSlots: row.mediaSlots,
    recruitmentUnitPrice: row.recruitmentUnitPrice,

    dailyWageAtJoin: row.dailyWageAtJoin,
    needsDormitory: row.needsDormitory,
    roomType: row.roomType,
    dormitoryFee: row.dormitoryFee,
    previousJob: row.previousJob,
    previousJobPeriod: row.previousJobPeriod,
    insuredBefore: row.insuredBefore,
    dormitoryExperience: row.dormitoryExperience,
    previousDormitoryFee: row.previousDormitoryFee,

    clientBeforeLeave: row.clientBeforeLeave,
    industryBeforeLeave: row.industryBeforeLeave,
    estimatedRetirementReason: row.estimatedRetirementReason,
  }
}